# 用户数据变更审核系统设计方案（含用户端去重）

## 1. 概述

本方案覆盖两部分：

1. **用户端**：提交数据变更时自动去重，同一用户同一类型最多保留一条待审核记录
2. **管理员端**：纯审核队列，只展示待审核条目，支持批量通过/拒绝

权限要求 role=55（审核员）或 role=77（系统管理员）。参考现有 `AdminWorksController` 和 `AdminUserController` 的模式设计。

---

## 2. 核心设计原则：纯审核表

`tb_user_data_change_lock` 定位为**纯审核队列**，而非变更历史日志：

- 管理员查询接口**仅返回 status=20（待审核）的条目**
- 同一 `userId + type` 下**最多存在一条 status=20 的记录**
- 审核通过或拒绝后，该记录状态变更，从管理员视角消失
- 审核通过时的连带操作（更新用户数据、重命名头像文件）在 Service 层完成

---

## 3. 当前进度

### 3.1 已完成（用户端）

| 功能 | type | 实现位置 | 审核方式 |
|------|:---:|------|------|
| 昵称修改 | 100 | `UserServiceImpl.updateNicknameWithAudit()` | AI 预审核，状态可能为 10/20/30 |
| 权限升级申请 | 200 | `UserServiceImpl.applyRoleUpgrade()` | 人工审核，状态固定 20 |
| 头像上传 | 300 | `UserServiceImpl.updateAvatarWithLock()` | 人工审核，状态固定 20 |

### 3.2 已完成（基础设施）

- `UserDataChangeLock` 实体：已包含 `lockId`（AUTO_INCREMENT PK）和 `createTime` 字段
- `UserDataChangeLockMapper`：已实现 `insertLock()` 方法
- `UserDataChangeLockMapper.xml`：已有 INSERT SQL

### 3.3 待实现

| 模块 | 内容                                         |
|------|--------------------------------------------|
| 实体 | `UserDataChangeLock` 新增 `isDelete` 字段      |
| 用户端 | 提交前去重（软删除旧 pending + 旧头像 .pend→.del）       |
| Mapper | 新增 `updatePendingToDeleted`、分页查询、批量查询、批量更新 |
| Service | 新增 `batchReviewUserDataChange` 批量审核、分页查询方法 |
| VO | 新建 `UserDataChangeLockVO` 展示对象             |
| Controller | 新建 `AdminUserDataChangeController`         |

---

## 4. 实体与表结构

### 4.1 实体（需新增 isDelete 字段）

```java
@Data
@TableName("tb_user_data_change_lock")
public class UserDataChangeLock {
    @TableId(type = IdType.AUTO)
    private Integer lockId;         // 主键
    private Integer userId;         // 待审核用户 ID
    private Integer type;           // 100-昵称 / 200-权限 / 300-头像
    private String nickname;        // 待审核昵称（type=100）
    private Integer userRole;       // 修改的用户角色（type=200）
    private String avatarUrl;       // 更改的头像路径（type=300）
    private String oldData;         // 旧数据（用于回滚）
    private Integer approvalStatus; // 10-通过 / 20-待审核 / 30-未过审
    private Boolean isDelete;       // 软删除标记（false-正常 / true-已删除）
    private Timestamp createTime;   // 创建时间
}
```

### 4.2 数据库变更

新增 `is_delete` 字段 + 建议索引：

```sql
-- 新增软删除字段
ALTER TABLE `tb_user_data_change_lock`
    ADD COLUMN `is_delete` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '软删除标记：0-正常, 1-已删除';

-- 建议索引
ALTER TABLE `tb_user_data_change_lock`
    ADD INDEX `idx_approval_status` (`approval_status`),
    ADD INDEX `idx_type` (`type`),
    ADD INDEX `idx_user_type_status` (`user_id`, `type`, `approval_status`);
```

> `idx_user_type_status` 用于用户端去重查询（`WHERE user_id=? AND type=? AND approval_status=20 AND is_delete=0`）。

---

## 5. 用户端去重机制

### 5.1 设计原则

同一用户提交同类型数据变更时，如果已有 status=20 的待审核记录，则**先软删除旧记录、重命名关联文件，再插入新记录**。确保管理员审核队列中不会出现同一用户同一类型的重复条目。

### 5.2 触发条件

仅当新记录的 `approvalStatus == 20` 时触发去重。如果 AI 审核直接通过（status=10）或直接拒绝（status=30），不触发去重（因为不会出现在管理员审核队列中）。

### 5.3 实现逻辑

```
supersedePendingLocks(userId, type):
    1. 查询该 userId + type 下所有 approval_status=20 AND is_delete=0 的记录
    2. 遍历旧记录：
       a. 如果 type=300（头像）：将 .pend 文件重命名为 .del
          - 文件路径：FilePathConfig.AvatarPath + avatarUrl
          - 文件命名规则：UUID.png.pend → UUID.png.del
       b. UPDATE 该记录 SET is_delete = 1
    3. 正常插入新记录
```

### 5.4 调用位置

在三个用户端方法的 `insertLock` 之前调用：

| 方法 | type | 调用条件 |
|------|:---:|------|
| `updateNicknameWithAudit()` | 100 | `approvalStatus == 20` 时 |
| `updateAvatarWithLock()` | 300 | 始终调用（固定20） |
| `applyRoleUpgrade()` | 200 | 始终调用（固定20） |

### 5.5 Mapper 新增方法

```java
/**
 * 查询指定用户指定类型的有效待审核记录（用于去重判断）
 *
 * @param userId 用户 ID
 * @param type   变更类型（100/200/300）
 * @return 待审核记录列表
 */
List<UserDataChangeLock> selectPendingByUserAndType(
    @Param("userId") Integer userId,
    @Param("type") Integer type
);

/**
 * 软删除指定用户指定类型的待审核记录
 * <p>将 approval_status=20 AND is_delete=0 的记录标记为 is_delete=1。</p>
 *
 * @param userId 用户 ID
 * @param type   变更类型（100/200/300）
 * @return 更新的记录数
 */
int updatePendingToDeleted(
    @Param("userId") Integer userId,
    @Param("type") Integer type
);
```

```sql
<!-- 查询有效待审核记录 -->
<select id="selectPendingByUserAndType" resultType="UserDataChangeLock">
    SELECT * FROM tb_user_data_change_lock
    WHERE user_id = #{userId}
      AND type = #{type}
      AND approval_status = 20
      AND is_delete = 0
</select>

<!-- 软删除待审核记录 -->
<update id="updatePendingToDeleted">
    UPDATE tb_user_data_change_lock
    SET is_delete = 1
    WHERE user_id = #{userId}
      AND type = #{type}
      AND approval_status = 20
      AND is_delete = 0
</update>
```

### 5.6 Service 层实现要点

```java
/**
 * 清除同一用户同类型的旧待审核记录
 * <p>仅在新记录的 approvalStatus 为 20 时调用。软删除旧记录，头像文件重命名为 .fail。</p>
 *
 * @param userId 用户 ID
 * @param type   变更类型
 */
private void supersedePendingLocks(Integer userId, Integer type) {
    List<UserDataChangeLock> oldLocks = userDataChangeLockMapper
        .selectPendingByUserAndType(userId, type);

    if (oldLocks.isEmpty()) {
        return;
    }

    for (UserDataChangeLock oldLock : oldLocks) {
        // 头像类型：重命名旧的 .pend 文件为 .del
        if (type == 300 && oldLock.getAvatarUrl() != null) {
            String oldPath = FilePathConfig.AvatarPath + "/" + oldLock.getAvatarUrl();
            String newPath = oldPath.replace(".pend", ".del");
            File file = new File(oldPath);
            if (file.exists()) {
                file.renameTo(new File(newPath));
                log.info("已将旧待审核头像重命名为 .del - {}", newPath);
            }
        }
    }

    // 批量软删除
    int count = userDataChangeLockMapper.updatePendingToDeleted(userId, type);
    log.info("已软删除旧待审核记录 - userId: {}, type: {}, 条数: {}", userId, type, count);
}
```

---

## 6. 管理员端状态转换规则

由于管理员**仅能看到 status=20 的条目**，状态转换规则大幅简化：

```
待审核 (20) ──通过──▶ 已通过 (10)   + 连带更新用户数据
待审核 (20) ──拒绝──▶ 未过审 (30)   + 头像文件 .pend→.fail
```

### 6.1 审核通过（20→10）时的连带操作

| type | 数据库更新 | 文件操作 |
|:---:|------|------|
| 100 | `userMapper.updateUserNickname(userId, nickname, adminId)` | 无 |
| 200 | `userMapper.updateUserRole(userId, userRole, adminId)` + `clearUserRoleCache(userId)` | 无 |
| 300 | `userMapper.updateUserAvatar(userId, avatarUrl, adminId)` | 头像重命名：`xxx.png.pend` → `xxx.png` |

### 6.2 审核拒绝（20→30）时的连带操作

| type | 数据库更新 | 文件操作 |
|:---:|------|------|
| 100 | 无 | 无 |
| 200 | 无 | 无 |
| 300 | 无 | 头像重命名：`xxx.png.pend` → `xxx.png.fail` |

---

## 7. 接口 1：批量审核操作

### 7.1 基本信息

| 项目 | 内容 |
|------|------|
| URL | `POST /api/admin/user-data-change/review` |
| 权限 | role=55（审核员）或 role=77（系统管理员） |
| 描述 | 批量审核用户数据变更锁记录，支持通过和拒绝 |

### 7.2 请求参数

| 参数 | 类型 | 必填 | 说明 |
|------|------|:---:|------|
| lockIds | List\<Integer\> | 是 | lock_id 列表 |
| targetStatus | Integer | 是 | 目标审核状态：10-通过 / 20-拒绝（仅支持 10 和 20） |

> 说明：接口参数 `targetStatus` 接受 10（通过）和 20（拒绝），Service 层将拒绝转为 30 写入数据库。

### 7.3 返回说明

复用现有 `AdminBatchOperateWorkResult`：

| 字段 | 类型 | 说明 |
|------|------|------|
| totalCount | int | 总数 |
| successCount | int | 成功数 |
| failedWorkIds | List\<Integer\> | 失败的 lock_id 列表 |

### 7.4 业务逻辑

```
1. 校验 lockIds 非空
2. 校验 targetStatus 为 10 或 20
3. 从 request.getAttribute("userId") 获取当前管理员 ID
4. 遍历 lockIds：
   a. 查询 lock 记录 → 不存在则记录失败
   b. 验证当前状态必须是 20（待审核），否则记录失败
   c. 如果 targetStatus=10（通过）：
      - 根据 type 执行连带操作（更新用户昵称/角色/头像）
      - 头像类型：重命名 .pend → 正常后缀
   d. 如果 targetStatus=20（拒绝）：
      - 头像类型：重命名 .pend → .fail
   e. 更新 lock 的 approval_status（10 或 30）
   f. 记录成功
5. 返回统计结果
```

---

## 8. 接口 2：分页查询待审核记录

### 8.1 基本信息

| 项目 | 内容 |
|------|------|
| URL | `GET /api/admin/user-data-change/pending/{current}/{size}` |
| 权限 | role=55（审核员）或 role=77（系统管理员） |
| 描述 | 分页查询待审核的用户数据变更记录，支持按类型筛选和时间排序 |

### 8.2 请求参数

| 参数 | 类型 | 必填 | 说明 |
|------|------|:---:|------|
| current | Long | 是 | 当前页码（从 1 开始），路径参数 |
| size | Long | 是 | 每页大小（1-500），路径参数 |
| type | Integer | 否 | 类型筛选：100-昵称 / 200-权限 / 300-头像 |

### 8.3 排序规则

默认按 `create_time DESC`（最新提交在前）。

### 8.4 返回说明

返回 `IPage<UserDataChangeLockVO>`，管理员端专用展示对象：

| 字段 | 类型 | 说明 |
|------|------|------|
| lockId | Integer | 主键 |
| userId | Integer | 用户 ID |
| username | String | 用户名（关联查询 tb_user） |
| userCurrentNickname | String | 用户当前昵称（关联查询 tb_user） |
| type | Integer | 变更类型 |
| typeName | String | 类型中文名（"昵称修改"/"权限申请"/"头像修改"） |
| nickname | String | 待审核昵称（仅 type=100） |
| userRole | Integer | 申请的角色代码（仅 type=200） |
| avatarUrl | String | 待审核头像路径（仅 type=300） |
| oldData | String | 旧数据 |
| createTime | Timestamp | 提交时间 |

> 注意：因为管理员只查看 status=20 的记录，VO 中不包含 `approvalStatus` 和 `statusName` 字段。

### 8.5 SQL 查询逻辑

```sql
SELECT l.lock_id, l.user_id, l.type, l.nickname, l.user_role, l.avatar_url, l.old_data, l.create_time,
       u.username, u.nickname AS user_current_nickname
FROM tb_user_data_change_lock l
LEFT JOIN tb_user u ON l.user_id = u.user_id
WHERE l.approval_status = 20
  AND l.is_delete = 0
  AND (:type IS NULL OR l.type = :type)
ORDER BY l.create_time DESC
```

---

## 9. 文件变更清单

### 9.1 新建文件

| 文件 | 说明 |
|------|------|
| `controller/admin/AdminUserDataChangeController.java` | 管理员审批 Controller |
| `pojo/UserDataChangeLockVO.java` | 分页查询 VO（含 username、typeName 展示字段） |

### 9.2 修改文件

| 文件 | 变更内容 |
|------|------|
| `pojo/UserDataChangeLock.java` | 新增 `isDelete` 字段（Boolean） |
| `mapper/UserDataChangeLockMapper.java` | 新增 `selectPendingByUserAndType`、`updatePendingToDeleted`、`selectPendingPage`、`selectLockByIds`、`batchUpdateApprovalStatus` |
| `resources/mapper/UserDataChangeLockMapper.xml` | 新增对应 SQL |
| `service/UserService.java` | 新增 `batchReviewUserDataChange` 和 `getPendingLockPage` 方法签名 |
| `service/Impl/UserServiceImpl.java` | 新增 `supersedePendingLocks` 私有方法；在三个提交方法中调用去重；实现批量审核和分页查询 |

### 9.3 建议变更（非必要）

| 文件 | 说明 |
|------|------|
| `sql/db_pix_vision-V3.2.sql` | 添加 `is_delete` 字段 + `idx_approval_status`、`idx_type`、`idx_user_type_status` 索引 |

---

## 10. Controller 类结构

```java
@RestController
@RequestMapping("/api/admin/user-data-change")
@RequiredArgsConstructor
@Tag(name = "系统管理员接口 - 用户数据变更审核")
@RequireRole(value = {55, 77})
public class AdminUserDataChangeController {

    private final UserService userService;

    @PostMapping("/review")
    public ResponsePojo<AdminBatchOperateWorkResult> batchReview(
        HttpServletRequest request,
        @RequestParam List<Integer> lockIds,
        @Schema(allowableValues = {"10", "20"}, description = "10-通过, 20-拒绝")
        @RequestParam Integer targetStatus
    ) {
        // 校验 targetStatus 为 10 或 20
        // 从 request.getAttribute("userId") 获取 adminId
        // 调用 userService.batchReviewUserDataChange(lockIds, targetStatus, adminId)
        // 返回统计结果
    }

    @GetMapping("/pending/{current}/{size}")
    public ResponsePojo<IPage<UserDataChangeLockVO>> getPendingPage(
        @PathVariable Long current,
        @PathVariable Long size,
        @RequestParam(required = false) Integer type
    ) {
        // 分页参数校验（使用 PageUtils.validatePageParams）
        // 调用 userService.getPendingLockPage(current, size, type)
        // 返回分页结果
    }
}
```

---

## 11. Mapper 新增方法汇总

```java
// ========== 用户端去重 ==========

/**
 * 查询指定用户指定类型的有效待审核记录（is_delete=0, approval_status=20）
 */
List<UserDataChangeLock> selectPendingByUserAndType(
    @Param("userId") Integer userId,
    @Param("type") Integer type
);

/**
 * 软删除指定用户指定类型的待审核记录（SET is_delete = 1）
 */
int updatePendingToDeleted(
    @Param("userId") Integer userId,
    @Param("type") Integer type
);

// ========== 管理员端 ==========

/**
 * 分页查询待审核记录（含用户名关联）
 */
IPage<UserDataChangeLockVO> selectPendingPage(
    Page<UserDataChangeLockVO> page,
    @Param("type") Integer type
);

/**
 * 根据 ID 批量查询（用于审核时验证）
 */
List<UserDataChangeLock> selectLockByIds(@Param("lockIds") List<Integer> lockIds);

/**
 * 批量更新审核状态
 */
int batchUpdateApprovalStatus(
    @Param("lockIds") List<Integer> lockIds,
    @Param("approvalStatus") Integer approvalStatus
);
```

---

## 12. Service 新增方法

```java
// ========== 管理员端 ==========

/**
 * 批量审核用户数据变更
 * <p>
 * 遍历 lockIds，验证当前状态必须为 20（待审核），
 * 通过时连带更新用户数据，拒绝时处理头像文件。
 * </p>
 *
 * @param lockIds      lock_id 列表
 * @param targetStatus 目标状态（10-通过 / 20-拒绝）
 * @param adminId      执行操作的管理员 ID
 * @return 批量操作结果
 */
@Transactional
AdminBatchOperateWorkResult batchReviewUserDataChange(
    List<Integer> lockIds, Integer targetStatus, Integer adminId
);

/**
 * 分页查询待审核记录
 *
 * @param current 当前页码
 * @param size    每页大小
 * @param type    变更类型筛选（可选）
 * @return 分页结果
 */
IPage<UserDataChangeLockVO> getPendingLockPage(Long current, Long size, Integer type);
```

---

## 13. 核心审核逻辑（Service 层）

```java
@Transactional
public AdminBatchOperateWorkResult batchReviewUserDataChange(
    List<Integer> lockIds, Integer targetStatus, Integer adminId
) {
    List<Integer> failedIds = new ArrayList<>();
    int successCount = 0;

    // 将接口参数 10(通过)/20(拒绝) 转为数据库状态 10(通过)/30(拒绝)
    Integer dbStatus = targetStatus.equals(20) ? 30 : targetStatus;

    for (Integer lockId : lockIds) {
        UserDataChangeLock lock = userDataChangeLockMapper.selectById(lockId);
        if (lock == null) {
            failedIds.add(lockId);
            continue;
        }

        // 只能审核待审核状态的记录
        if (lock.getApprovalStatus() != 20) {
            failedIds.add(lockId);
            continue;
        }

        Integer userId = lock.getUserId();

        // 审核通过：连带更新用户数据
        if (dbStatus == 10) {
            switch (lock.getType()) {
                case 100:
                    userMapper.updateUserNickname(userId, lock.getNickname(), adminId);
                    break;
                case 200:
                    userMapper.updateUserRole(userId, lock.getUserRole(), adminId);
                    clearUserRoleCache(userId);
                    break;
                case 300:
                    userMapper.updateUserAvatar(userId, lock.getAvatarUrl(), adminId);
                    // 头像重命名：xxx.png.pend → xxx.png
                    renameAvatarFile(lock.getAvatarUrl(), ".pend", "");
                    break;
            }
        }

        // 审核拒绝：头像文件处理
        if (dbStatus == 30 && lock.getType() == 300) {
            // xxx.png.pend → xxx.png.fail
            renameAvatarFile(lock.getAvatarUrl(), ".pend", ".fail");
        }

        // 更新审核状态
        lock.setApprovalStatus(dbStatus);
        userDataChangeLockMapper.updateById(lock);
        successCount++;
    }

    return new AdminBatchOperateWorkResult(lockIds.size(), successCount, failedIds);
}
```

> 批量更新可直接使用 MyBatis-Plus 的 `updateById` 逐条更新，审核操作量级不大，无需批量 SQL 优化。若需要，可保留 `batchUpdateApprovalStatus` 方法。

---

## 14. 头像文件重命名逻辑

头像上传时以 `.pend` 后缀保存（如 `a1b2c3.png.pend`），审核流程涉及文件重命名：

| 操作 | 转换 | 说明 |
|------|------|------|
| 审核通过 | `.pend` → （无后缀） | `a1b2c3.png.pend` → `a1b2c3.png` |
| 审核拒绝 | `.pend` → `.fail` | `a1b2c3.png.pend` → `a1b2c3.png.fail` |
| 用户重新提交（去重） | `.pend` → `.del` | 旧 pending 被新申请覆盖，重命名为 .del 标记废弃 |

实现使用 `java.io.File.renameTo()`，文件位于 `FilePathConfig.AvatarPath` 目录。

---

## 15. 实施步骤

| 序号 | 步骤 | 涉及文件 |
|:---:|------|------|
| 1 | 数据库：添加 `is_delete` 字段 + 索引 | `sql/db_pix_vision-V3.2.sql` |
| 2 | 实体：新增 `isDelete` 字段 | `pojo/UserDataChangeLock.java` |
| 3 | 修改 Mapper 接口 | `mapper/UserDataChangeLockMapper.java` - 新增 5 个方法 |
| 4 | 修改 Mapper XML | `resources/mapper/UserDataChangeLockMapper.xml` - 新增对应 SQL |
| 5 | 用户端去重：实现 `supersedePendingLocks` + 在三个提交方法中调用 | `service/Impl/UserServiceImpl.java` |
| 6 | 新建 VO | `pojo/UserDataChangeLockVO.java` |
| 7 | 修改 Service 接口 | `service/UserService.java` - 新增 2 个方法 |
| 8 | 修改 Service 实现：批量审核 + 分页查询 | `service/Impl/UserServiceImpl.java` |
| 9 | 新建 Controller | `controller/admin/AdminUserDataChangeController.java` |

---

## 16. 文件变更汇总

| 操作 | 文件路径 |
|------|------|
| **新建** | `src/main/java/top/playereg/pix_vision/controller/admin/AdminUserDataChangeController.java` |
| **新建** | `src/main/java/top/playereg/pix_vision/pojo/UserDataChangeLockVO.java` |
| **修改** | `src/main/java/top/playereg/pix_vision/pojo/UserDataChangeLock.java` |
| **修改** | `src/main/java/top/playereg/pix_vision/mapper/UserDataChangeLockMapper.java` |
| **修改** | `src/main/resources/mapper/UserDataChangeLockMapper.xml` |
| **修改** | `src/main/java/top/playereg/pix_vision/service/UserService.java` |
| **修改** | `src/main/java/top/playereg/pix_vision/service/Impl/UserServiceImpl.java` |
| **修改** | `sql/db_pix_vision-V3.2.sql` |

---

## 17. 与 v2.0 的主要差异

| 方面 | v2.0（旧） | v3.0（新） |
|------|------|------|
| 表定位 | 变更记录表（含历史） | 纯审核队列 |
| 管理员可见范围 | status=10/20/30 均可查询 | 仅 status=20 |
| 分页查询筛选 | approvalStatus + type | 仅 type（固定 status=20） |
| 状态转换 | 20→10, 20→30, 10→20, 10→30 | 仅 20→10 和 20→30 |
| 用户重复提交 | 无处理（会产生多条20） | 软删除旧 pending + 头像.pend→.del |
| 旧记录处理 | 标记为30（status=30堆积） | 软删除（is_delete=1，数据可追溯） |
| 接口参数 targetStatus | 10/20/30 | 10/20（拒绝在 Service 层转为30） |
| VO 字段 | 含 approvalStatus、statusName | 不含（始终待审核） |

---

**文档更新时间**: 2026-05-25  
**方案版本**: v3.0
