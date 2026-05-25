# 管理员端用户数据变更人工审核接口设计方案

## 1. 概述

为 `tb_user_data_change_lock` 表提供管理员端人工审核功能，包含**批量审核操作**和**分页查询**两个接口。权限要求 role=55（审核员）或 role=77（系统管理员）。

参考现有 `AdminWorksController` 和 `AdminUserController` 的模式设计，使用 `@RequireRole` 类级别注解 + `request.getAttribute("userId")` 获取管理员 ID。

---

## 2. 当前进度

### 2.1 已完成（用户端）

| 功能 | type | 实现位置 | 审核方式 |
|------|:---:|------|------|
| 昵称修改 | 100 | `UserServiceImpl.updateNicknameWithAudit()` | AI 预审核 → 状态可能为 10/20/30 |
| 权限升级申请 | 200 | `UserServiceImpl.applyRoleUpgrade()` | 人工审核 → 状态固定 20 |
| 头像上传 | 300 | `UserServiceImpl.updateAvatarWithLock()` | 人工审核 → 状态固定 20 |

用户端三种数据变更均已正确写入 `tb_user_data_change_lock` 表（通过 `UserDataChangeLockMapper.insertLock()`），等待管理员审核。

### 2.2 已完成（基础设施）

- `UserDataChangeLock` 实体：已包含 `lockId`（AUTO_INCREMENT PK）和 `createTime` 字段
- `UserDataChangeLockMapper`：已实现 `insertLock()` 方法
- `UserDataChangeLockMapper.xml`：已有 INSERT SQL

### 2.3 待实现（管理员端）

- Mapper：新增分页查询、批量查询、批量更新方法 + XML SQL
- Service：新增 `batchReviewUserDataChange()` 批量审核方法
- VO：新建 `UserDataChangeLockVO` 展示对象
- Controller：新建 `AdminUserDataChangeController`

---

## 3. 实体与表结构

### 3.1 当前实体（已完成）

```java
@Data
@TableName("tb_user_data_change_lock")
public class UserDataChangeLock {
    @TableId(type = IdType.AUTO)
    private Integer lockId;       // 主键
    private Integer userId;       // 待审核用户 ID
    private Integer type;         // 100-昵称 / 200-权限 / 300-头像
    private String nickname;      // 待审核昵称
    private Integer userRole;     // 修改的用户角色
    private String avatarUrl;     // 更改的头像路径
    private String oldData;       // 旧数据（用于回滚）
    private Integer approvalStatus; // 10-通过 / 20-待审核 / 30-未过审
    private Timestamp createTime; // 创建时间
}
```

### 3.2 建议索引

```sql
ALTER TABLE `tb_user_data_change_lock`
    ADD INDEX `idx_approval_status` (`approval_status`),
    ADD INDEX `idx_type` (`type`);
```

> 注意：`lock_id` 主键和 `create_time` 字段已存在，无需重复添加。

---

## 4. 状态转换规则

```
         ┌──── 待审核 (20) ────┐
         │   (用户提交变更)      │
         ▼                      ▼
   审核通过 (10)           未过审 (30)
   (更新用户数据)          (标记违规)
         │
         ├── 可退回 ──→ 待审核 (20)
         │
         └── 可标记 ──→ 未过审 (30)
```

| 当前状态 | 允许转换到 | 说明 |
|---------|:---:|------|
| 10（已通过） | 20, 30 | 可退回到待审核，或标记为未过审 |
| 20（待审核） | 10 | 只能审核通过 |
| 30（未过审） | 无 | 终态，不可再修改 |

### 4.1 审核通过（→10）时的连带操作

| type | 说明 | 数据库更新 | 文件操作 |
|:---:|------|------|------|
| 100 | 昵称 | `userMapper.updateUserNickname(userId, nickname, adminId)` | 无 |
| 200 | 权限 | `userMapper.updateUserRole(userId, userRole, adminId)` + `clearUserRoleCache(userId)` | 无 |
| 300 | 头像 | `userMapper.updateUserAvatar(userId, avatarUrl, adminId)` | 头像文件重命名：`xxx.png.pend` → `xxx.png` |

### 4.2 标记未过审（→30）时的连带操作

| type | 说明 | 文件操作 |
|:---:|------|------|
| 100 | 昵称 | 无 |
| 200 | 权限 | 无 |
| 300 | 头像 | 头像文件重命名：`xxx.png.pend` → `xxx.png.fail` |

### 4.3 退回到待审核（10→20）

仅更新 lock 表 `approval_status`，不修改 `tb_user` 数据，不操作文件。

> 对于头像类型退回到待审核：文件已在审批通过时重命名为正常后缀，退回到待审核后不恢复 `.pend` 后缀（文件保持当前状态即可）。

---

## 5. 接口 1：批量审核操作

### 5.1 基本信息

| 项目 | 内容 |
|------|------|
| URL | `POST /api/admin/user-data-change/review` |
| 权限 | role=55（审核员）或 role=77（系统管理员） |
| 描述 | 批量审核用户数据变更锁记录 |

### 5.2 请求参数

| 参数 | 类型 | 必填 | 说明 |
|------|------|:---:|------|
| lockIds | List\<Integer\> | 是 | lock_id 列表 |
| targetStatus | Integer | 是 | 目标审核状态：10-通过 / 20-退回待审核 / 30-标记未过审 |

### 5.3 返回说明

复用现有 `AdminBatchOperateWorkResult`：

| 字段 | 类型 | 说明 |
|------|------|------|
| totalCount | int | 总数 |
| successCount | int | 成功数 |
| failedWorkIds | List\<Integer\> | 失败的 lock_id 列表 |

### 5.4 业务逻辑

```
1. 校验 lockIds 非空、targetStatus 合法（10/20/30）
2. 从 request.getAttribute("userId") 获取当前管理员 ID
3. 遍历 lockIds 列表：
   a. 查询 lock 记录是否存在 → 不存在则记录失败
   b. 验证状态转换合法性：
      - 当前30（终态）：跳过，不可修改
      - 当前20 → 目标10：通过。更新 lock 状态 + 连带更新用户数据
      - 当前20 → 目标20/30：非法，待审核只能通过
      - 当前10 → 目标20：退回。只更新 lock 状态
      - 当前10 → 目标30：标记未过审。更新 lock 状态 + 头像文件重命名（仅type=300）
      - 同状态转换：跳过
   c. 根据 type 和目标状态执行连带操作（见 4.1、4.2）
   d. 记录成功/失败
4. 返回统计结果
```

---

## 6. 接口 2：分页查询审批记录

### 6.1 基本信息

| 项目 | 内容 |
|------|------|
| URL | `GET /api/admin/user-data-change/page/{current}/{size}` |
| 权限 | role=55（审核员）或 role=77（系统管理员） |
| 描述 | 分页查询用户数据变更锁记录，支持按状态、类型筛选和时间排序 |

### 6.2 请求参数

| 参数 | 类型 | 必填 | 说明 |
|------|------|:---:|------|
| current | Long | 是 | 当前页码（从 1 开始），路径参数 |
| size | Long | 是 | 每页大小（1-500），路径参数 |
| approvalStatus | Integer | 否 | 审核状态筛选：10-通过 / 20-待审核 / 30-未过审 |
| type | Integer | 否 | 类型筛选：100-昵称 / 200-权限 / 300-头像 |
| orderBy | String | 否 | 排序方式（默认 `newest_pending`） |

### 6.3 排序规则

| orderBy 值 | 排序规则 |
|------|------|
| `newest_pending`（默认） | 待审核条目优先（`approval_status ASC`），然后按 `create_time DESC` |
| `oldest` | 按 `create_time ASC` |
| `newest` | 按 `create_time DESC` |

### 6.4 返回说明

返回 `IPage<UserDataChangeLockVO>`，包含：

| 字段 | 类型 | 说明 |
|------|------|------|
| lockId | Integer | 主键 |
| userId | Integer | 用户 ID |
| username | String | 用户名（关联查询 tb_user） |
| type | Integer | 100-昵称 / 200-权限 / 300-头像 |
| typeName | String | 类型名称（"昵称"/"权限"/"头像"） |
| nickname | String | 待审核昵称（仅 type=100） |
| userRole | Integer | 修改的角色（仅 type=200） |
| avatarUrl | String | 修改的头像路径（仅 type=300） |
| oldData | String | 旧数据 |
| approvalStatus | Integer | 10-通过 / 20-待审核 / 30-未过审 |
| statusName | String | 状态名称（"已通过"/"待审核"/"未过审"） |
| createTime | Timestamp | 创建时间 |

### 6.5 SQL 查询逻辑

```sql
SELECT l.*, u.username, u.nickname as current_nickname
FROM tb_user_data_change_lock l
LEFT JOIN tb_user u ON l.user_id = u.user_id
WHERE 1=1
  AND (:approvalStatus IS NULL OR l.approval_status = :approvalStatus)
  AND (:type IS NULL OR l.type = :type)
ORDER BY
  CASE WHEN :orderBy = 'newest_pending'
    THEN l.approval_status END ASC,
  l.create_time DESC
```

---

## 7. 文件变更清单

### 7.1 新建文件

| 文件 | 说明 |
|------|------|
| `controller/admin/AdminUserDataChangeController.java` | 管理员审批 Controller（分页查询 + 批量审核） |
| `pojo/UserDataChangeLockVO.java` | 分页查询 VO（含 username、typeName、statusName 等展示字段） |

### 7.2 修改文件

| 文件 | 变更内容 |
|------|------|
| `mapper/UserDataChangeLockMapper.java` | 新增 `selectLockPage`、`selectLockByIds`、`batchUpdateApprovalStatus` 方法 |
| `resources/mapper/UserDataChangeLockMapper.xml` | 新增分页查询、批量查询、批量更新 SQL |
| `service/UserService.java` | 新增 `batchReviewUserDataChange` 方法签名 |
| `service/Impl/UserServiceImpl.java` | 实现批量审核逻辑（状态转换 + 连带用户数据更新 + 头像文件处理） |

### 7.3 无需变更（已完成）

| 文件 | 说明 |
|------|------|
| `pojo/UserDataChangeLock.java` | lockId 和 createTime 已存在，无需修改 |
| `sql/db_pix_vision-V3.2.sql` | 主键和时间字段已存在，建议仅添加索引 |

---

## 8. Controller 类结构

```java
@RestController
@RequestMapping("/api/admin/user-data-change")
@RequiredArgsConstructor
@Tag(name = "系统管理员接口 - 用户数据变更审核")
@RequireRole(value = {55, 77})
public class AdminUserDataChangeController {

    private final UserService userService;

    // POST /review → batchReview()
    // GET  /page/{current}/{size} → getLockPage()
}
```

### 8.1 Controller 伪代码

```java
@PostMapping("/review")
public ResponsePojo<AdminBatchOperateWorkResult> batchReview(
    HttpServletRequest request,
    @RequestParam List<Integer> lockIds,
    @Schema(allowableValues = {"10", "20", "30"}) @RequestParam Integer targetStatus
) {
    // 参数校验：lockIds 非空、targetStatus 合法
    // 从 request.getAttribute("userId") 获取 adminId
    // 调用 userService.batchReviewUserDataChange(lockIds, targetStatus, adminId)
    // 返回统计结果
}

@GetMapping("/page/{current}/{size}")
public ResponsePojo<IPage<UserDataChangeLockVO>> getLockPage(
    @PathVariable Long current,
    @PathVariable Long size,
    @RequestParam(required = false) Integer approvalStatus,
    @RequestParam(required = false) Integer type,
    @RequestParam(required = false, defaultValue = "newest_pending") String orderBy
) {
    // 分页参数校验（使用 PageUtils.validatePageParams）
    // 调用 userService.getUserDataChangeLockPage(...)
    // 返回分页结果
}
```

---

## 9. Mapper 新增方法

```java
// 分页查询（含用户名关联）
IPage<UserDataChangeLockVO> selectLockPage(
    Page<UserDataChangeLockVO> page,
    @Param("approvalStatus") Integer approvalStatus,
    @Param("type") Integer type,
    @Param("orderBy") String orderBy
);

// 根据 ID 批量查询（用于审核时验证）
List<UserDataChangeLock> selectLockByIds(@Param("lockIds") List<Integer> lockIds);

// 批量更新审核状态
int batchUpdateApprovalStatus(
    @Param("lockIds") List<Integer> lockIds,
    @Param("approvalStatus") Integer approvalStatus
);
```

---

## 10. Service 新增方法

```java
/**
 * 批量审核用户数据变更
 * <p>
 * 遍历 lockIds 列表，根据状态转换规则验证合法性，审核通过时连带更新用户数据。
 * 头像类型审核时同步处理文件重命名。
 * </p>
 *
 * @param lockIds      lock_id 列表
 * @param targetStatus 目标审核状态（10-通过 / 20-退回待审核 / 30-标记未过审）
 * @param adminId      执行操作的管理员 ID
 * @return 批量操作结果（总数/成功数/失败ID列表）
 */
@Transactional
AdminBatchOperateWorkResult batchReviewUserDataChange(
    List<Integer> lockIds, Integer targetStatus, Integer adminId
);
```

---

## 11. 核心审核逻辑（Service 层实现要点）

```java
@Transactional
public AdminBatchOperateWorkResult batchReviewUserDataChange(
    List<Integer> lockIds, Integer targetStatus, Integer adminId
) {
    List<Integer> failedIds = new ArrayList<>();
    int successCount = 0;

    for (Integer lockId : lockIds) {
        UserDataChangeLock lock = userDataChangeLockMapper.selectById(lockId);
        if (lock == null) {
            failedIds.add(lockId);
            continue;
        }

        int currentStatus = lock.getApprovalStatus();

        // 终态（30）不可修改
        if (currentStatus == 30) {
            failedIds.add(lockId);
            continue;
        }
        // 同状态跳过
        if (currentStatus == targetStatus) {
            failedIds.add(lockId);
            continue;
        }
        // 待审核只能通过
        if (currentStatus == 20 && targetStatus != 10) {
            failedIds.add(lockId);
            continue;
        }
        // 已通过可以退回或标记未过审
        if (currentStatus == 10 && targetStatus == 10) {
            failedIds.add(lockId);
            continue;
        }

        Integer userId = lock.getUserId();

        // 审核通过（→10）：连带更新用户数据
        if (targetStatus == 10 && currentStatus == 20) {
            switch (lock.getType()) {
                case 100: // 昵称
                    userMapper.updateUserNickname(userId, lock.getNickname(), adminId);
                    break;
                case 200: // 权限
                    userMapper.updateUserRole(userId, lock.getUserRole(), adminId);
                    clearUserRoleCache(userId);
                    break;
                case 300: // 头像
                    userMapper.updateUserAvatar(userId, lock.getAvatarUrl(), adminId);
                    // 头像文件重命名：xxx.png.pend → xxx.png
                    renameAvatarFile(lock.getAvatarUrl(), ".pend", "");
                    break;
            }
        }

        // 标记未过审（→30）：头像需要文件处理
        if (targetStatus == 30 && lock.getType() == 300) {
            // 当前待审核(20)：xxx.png.pend → xxx.png.fail
            // 当前已通过(10)：xxx.png → xxx.png.fail
            String fromSuffix = (currentStatus == 20) ? ".pend" : "";
            renameAvatarFile(lock.getAvatarUrl(), fromSuffix, ".fail");
        }

        // 更新 lock 的审核状态
        userDataChangeLockMapper.batchUpdateApprovalStatus(List.of(lockId), targetStatus);
        successCount++;
    }

    return new AdminBatchOperateWorkResult(lockIds.size(), successCount, failedIds);
}
```

---

## 12. 头像文件重命名逻辑

头像上传时以 `.pend` 后缀保存（如 `a1b2c3.png.pend`），审核流程涉及文件重命名：

| 操作 | 转换 | 说明 |
|------|------|------|
| 审批通过（20→10） | `.pend` → （无后缀） | `a1b2c3.png.pend` → `a1b2c3.png` |
| 标记未过审（20→30） | `.pend` → `.fail` | `a1b2c3.png.pend` → `a1b2c3.png.fail` |
| 标记未过审（10→30） | （无后缀）→ `.fail` | `a1b2c3.png` → `a1b2c3.png.fail` |

实现使用 `java.io.File.renameTo()`，参考 `ImageController` 中的文件后缀解析机制。

---

## 13. 实施步骤

| 序号 | 步骤 | 涉及文件 |
|:---:|------|------|
| 1 | 建议添加数据库索引 | `sql/db_pix_vision-V3.2.sql`（仅索引，无 DDL 变更） |
| 2 | 新建 VO | `pojo/UserDataChangeLockVO.java` |
| 3 | 修改 Mapper 接口 | `mapper/UserDataChangeLockMapper.java` - 新增 3 个方法 |
| 4 | 修改 Mapper XML | `resources/mapper/UserDataChangeLockMapper.xml` - 新增对应 SQL |
| 5 | 修改 Service 接口 | `service/UserService.java` - 新增 `batchReviewUserDataChange` |
| 6 | 修改 Service 实现 | `service/Impl/UserServiceImpl.java` - 实现审核逻辑 |
| 7 | 新建 Controller | `controller/admin/AdminUserDataChangeController.java` |

---

## 14. 文件变更汇总

| 操作 | 文件路径 |
|------|------|
| **新建** | `src/main/java/top/playereg/pix_vision/controller/admin/AdminUserDataChangeController.java` |
| **新建** | `src/main/java/top/playereg/pix_vision/pojo/UserDataChangeLockVO.java` |
| **修改** | `src/main/java/top/playereg/pix_vision/mapper/UserDataChangeLockMapper.java` |
| **修改** | `src/main/resources/mapper/UserDataChangeLockMapper.xml` |
| **修改** | `src/main/java/top/playereg/pix_vision/service/UserService.java` |
| **修改** | `src/main/java/top/playereg/pix_vision/service/Impl/UserServiceImpl.java` |
| **建议** | `sql/db_pix_vision-V3.2.sql`（添加 `idx_approval_status` 和 `idx_type` 索引） |

---

**文档更新时间**: 2026-05-25  
**方案版本**: v2.0
