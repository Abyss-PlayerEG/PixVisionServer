# 管理员端用户数据变更人工审核接口设计方案

## 1. 概述

为 `tb_user_data_change_lock` 表提供管理员端人工审核功能，包含**批量审核操作**和**分页查询**两个接口。权限要求 role=55（审核员）或 role=77（系统管理员）。参考现有 `AdminWorksController` 和 `AdminUserController` 的模式设计。

---

## 2. 数据库变更

当前 `tb_user_data_change_lock` 表缺少主键和时间字段，需要执行以下 DDL：

```sql
ALTER TABLE `tb_user_data_change_lock`
    ADD COLUMN `lock_id` int NOT NULL AUTO_INCREMENT PRIMARY KEY FIRST,
    ADD COLUMN `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '数据条目创建时间戳';

-- 为分页查询优化
ALTER TABLE `tb_user_data_change_lock`
    ADD INDEX `idx_approval_status` (`approval_status`),
    ADD INDEX `idx_type` (`type`);
```

### 完整表结构（变更后）

| 字段 | 类型 | 说明 |
|------|------|------|
| lock_id | int AUTO_INCREMENT PK | 主键 |
| user_id | int NOT NULL | 待审核用户 ID |
| type | int NOT NULL | 100-昵称 / 200-权限 / 300-头像 |
| nickname | varchar(48) | 待审核昵称 |
| user_role | int | 修改的用户角色 |
| avatar_url | varchar(96) | 更改的头像路径 |
| old_data | varchar(100) NOT NULL | 旧数据（用于回滚） |
| approval_status | int NOT NULL DEFAULT 20 | 10-通过 / 20-待审核 / 30-未过审 |
| create_time | timestamp NOT NULL | 创建时间 |

---

## 3. 状态转换规则

```
         ┌──── 待审核 (20) ────┐
         │   (用户提交变更)      │
         ▼                      ▼
   审核通过 (10)           未过审 (30)
   (更新用户数据)          (不可再修改)
         │
         ├── 可退回 ──→ 待审核 (20)
         │
         └── 可标记 ──→ 未过审 (30)
```

| 当前状态 | 允许转换到 | 说明 |
|---------|:---:|------|
| 10（已通过） | 20, 30 | 可退回到待审核，或标记为未过审 |
| 20（待审核） | 10 | 只能审核通过 |
| 30（未过审） | 无 | 不可再修改 |

**通过（→10）时的连带操作**（根据 type 更新 tb_user 对应字段）：

| type | 说明 | 更新操作 |
|:---:|------|------|
| 100 | 昵称 | `userMapper.updateUserNickname(userId, nickname, adminId)` |
| 200 | 权限 | `userMapper.updateUserRole(userId, userRole, adminId)` + 清除角色缓存 |
| 300 | 头像 | `userMapper.updateUserAvatar(userId, avatarUrl, adminId)` |

**退回或标记未过审（10→20 / 10→30）时**：仅更新 lock 表 `approval_status`，不修改 tb_user 数据。

---

## 4. 接口 1：批量审核操作

### 4.1 基本信息

| 项目 | 内容 |
|------|------|
| URL | `POST /api/admin/user-data-change/review` |
| 权限 | role=55（审核员）或 role=77（系统管理员） |
| 描述 | 批量审核用户数据变更锁记录 |

### 4.2 请求参数

| 参数 | 类型 | 必填 | 说明 |
|------|------|:---:|------|
| lockIds | List\<Integer\> | 是 | lock_id 列表 |
| targetStatus | Integer | 是 | 目标审核状态：10-通过 / 20-退回待审核 / 30-标记未过审 |

### 4.3 返回说明

返回 `AdminBatchOperateWorkResult`（复用现有类）：

| 字段 | 类型 | 说明 |
|------|------|------|
| totalCount | int | 总数 |
| successCount | int | 成功数 |
| failedWorkIds | List\<Integer\> | 失败的 lock_id 列表 |

### 4.4 业务逻辑

```
1. 校验 lockIds 非空、targetStatus 合法（10/20/30）
2. 从 Token 获取当前管理员 ID
3. 遍历 lockIds 列表：
   a. 查询 lock 记录是否存在
   b. 验证状态转换合法性：
      - 当前10 → 目标 20/30：只更新 lock 状态
      - 当前20 → 目标 10：更新 lock 状态 + 根据 type 更新 tb_user 对应字段
      - 当前30：跳过（不可修改）
      - 同状态转换：跳过
   c. 记录成功/失败
4. 返回统计结果
```

### 4.5 Controller 伪代码

```java
@PostMapping("/review")
public ResponsePojo<AdminBatchOperateWorkResult> batchReview(
    HttpServletRequest request,
    @RequestParam List<Integer> lockIds,
    @Schema(allowableValues = {"10", "20", "30"}) @RequestParam Integer targetStatus
) {
    // 参数校验
    // 从 request.getAttribute("userId") 获取 adminId
    // 调用 userService.batchReviewUserDataChange(lockIds, targetStatus, adminId)
    // 返回统计结果
}
```

---

## 5. 接口 2：分页查询审批记录

### 5.1 基本信息

| 项目 | 内容 |
|------|------|
| URL | `GET /api/admin/user-data-change/page/{current}/{size}` |
| 权限 | role=55（审核员）或 role=77（系统管理员） |
| 描述 | 分页查询用户数据变更锁记录，支持按状态、类型筛选和时间排序 |

### 5.2 请求参数

| 参数 | 类型 | 必填 | 说明 |
|------|------|:---:|------|
| current | Long | 是 | 当前页码（从 1 开始），路径参数 |
| size | Long | 是 | 每页大小（1-500），路径参数 |
| approvalStatus | Integer | 否 | 审核状态筛选：10-通过 / 20-待审核 / 30-未过审 |
| type | Integer | 否 | 类型筛选：100-昵称 / 200-权限 / 300-头像 |
| orderBy | String | 否 | 排序方式（默认 `newest_pending`） |

### 5.3 排序规则

| orderBy 值 | 排序规则 |
|------|------|
| `newest_pending`（默认） | 待审核条目优先（`approval_status ASC`），然后按 `create_time DESC` |
| `oldest` | 按 `create_time ASC` |
| `newest` | 按 `create_time DESC` |

### 5.4 返回说明

返回 `IPage<UserDataChangeLockVO>`，包含：

| 字段 | 类型 | 说明 |
|------|------|------|
| lockId | Integer | 主键 |
| userId | Integer | 用户 ID |
| type | Integer | 100-昵称 / 200-权限 / 300-头像 |
| typeName | String | 类型名称（如"昵称"） |
| nickname | String | 待审核昵称 |
| userRole | Integer | 修改的角色 |
| avatarUrl | String | 修改的头像 |
| oldData | String | 旧数据 |
| approvalStatus | Integer | 10-通过 / 20-待审核 / 30-未过审 |
| statusName | String | 状态名称（如"待审核"） |
| createTime | Timestamp | 创建时间 |

### 5.5 SQL 查询逻辑

```sql
SELECT l.*, u.username, u.nickname as current_nickname
FROM tb_user_data_change_lock l
LEFT JOIN tb_user u ON l.user_id = u.user_id
WHERE 1=1
  AND (:approvalStatus IS NULL OR l.approval_status = :approvalStatus)
  AND (:type IS NULL OR l.type = :type)
ORDER BY
  CASE WHEN :orderBy = 'newest_pending'
    THEN l.approval_status END ASC,  -- 20(待审核)排最前
  l.create_time DESC
```

---

## 6. 文件变更清单

### 6.1 新建文件

| 文件 | 说明 |
|------|------|
| `controller/admin/AdminUserDataChangeController.java` | 管理员审批 Controller |
| `pojo/adminPojo/AdminBatchReviewResult.java` | 批量审核结果类（可选，可复用 `AdminBatchOperateWorkResult`） |
| `pojo/UserDataChangeLockVO.java` | 分页查询 VO（含用户名、类型名称、状态名称等展示字段） |

### 6.2 修改文件

| 文件 | 变更内容 |
|------|------|
| `pojo/UserDataChangeLock.java` | 添加 `createTime` 字段 |
| `mapper/UserDataChangeLockMapper.java` | 新增 `selectLockPage`、`selectLockByIds`、`batchUpdateApprovalStatus` 方法 |
| `resources/mapper/UserDataChangeLockMapper.xml` | 新增分页查询、批量查询、批量更新 SQL |
| `service/UserService.java` | 新增 `batchReviewUserDataChange` 方法签名 |
| `service/Impl/UserServiceImpl.java` | 实现批量审核逻辑（状态转换 + 连带用户数据更新） |
| `sql/db_pix_vision-V3.1.sql` | 添加 lock_id 主键 + create_time 字段 + 索引 |

### 6.3 Controller 类结构

```
controller/admin/AdminUserDataChangeController.java
├── @RestController
├── @RequestMapping("/api/admin/user-data-change")
├── @RequireRole(value = {55, 77})
├── @Tag(name = "系统管理员接口 - 用户数据变更审核")
│
├── POST /review          → batchReview()
└── GET  /page/{current}/{size} → getLockPage()
```

### 6.4 Mapper 新增方法

```java
// 分页查询
IPage<UserDataChangeLock> selectLockPage(
    Page<UserDataChangeLock> page,
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

### 6.5 Service 新增方法

```java
/**
 * 批量审核用户数据变更
 *
 * @param lockIds      lock_id 列表
 * @param targetStatus 目标审核状态（10-通过 / 20-退回待审核 / 30-标记未过审）
 * @param adminId      管理员 ID
 * @return 批量操作结果
 */
AdminBatchOperateWorkResult batchReviewUserDataChange(
    List<Integer> lockIds, Integer targetStatus, Integer adminId
);
```

---

## 7. 核心审核逻辑（Service 层实现要点）

```java
for (Integer lockId : lockIds) {
    UserDataChangeLock lock = 查询 lock;
    if (lock == null) → 记录失败，继续;

    int currentStatus = lock.getApprovalStatus();
    int target = targetStatus;

    // 验证状态转换合法性
    if (currentStatus == 30) → 记录失败（未过审不可修改），继续;
    if (currentStatus == target) → 记录失败（同状态），继续;
    if (currentStatus == 10 && target == 10) → 不允许，继续;
    if (currentStatus == 20 && target != 10) → 待审核只能通过，继续;

    // 审核通过且当前是待审核时，连带更新用户数据
    if (target == 10 && currentStatus == 20) {
        Integer userId = lock.getUserId();
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
                break;
        }
    }

    // 更新 lock 的审核状态
    userDataChangeLockMapper.batchUpdateApprovalStatus(List.of(lockId), target);
    记录成功;
}
```

---

## 8. 实施步骤

| 序号 | 步骤 | 涉及文件 |
|:---:|------|------|
| 1 | 数据库变更 | `sql/db_pix_vision-V3.1.sql` |
| 2 | 修改实体类 | `pojo/UserDataChangeLock.java` - 添加 `createTime` |
| 3 | 新建 VO | `pojo/UserDataChangeLockVO.java` |
| 4 | 修改 Mapper | `mapper/UserDataChangeLockMapper.java` - 新增 3 个方法 |
| 5 | 修改 XML | `resources/mapper/UserDataChangeLockMapper.xml` - 新增 SQL |
| 6 | 修改 Service 接口 | `service/UserService.java` - 新增 `batchReviewUserDataChange` |
| 7 | 修改 Service 实现 | `service/Impl/UserServiceImpl.java` - 实现审核逻辑 |
| 8 | 新建 Controller | `controller/admin/AdminUserDataChangeController.java` |

---

## 9. 文件变更汇总

| 操作 | 文件路径 |
|------|------|
| **新建** | `src/main/java/top/playereg/pix_vision/controller/admin/AdminUserDataChangeController.java` |
| **新建** | `src/main/java/top/playereg/pix_vision/pojo/UserDataChangeLockVO.java` |
| **修改** | `src/main/java/top/playereg/pix_vision/pojo/UserDataChangeLock.java` |
| **修改** | `src/main/java/top/playereg/pix_vision/mapper/UserDataChangeLockMapper.java` |
| **修改** | `src/main/resources/mapper/UserDataChangeLockMapper.xml` |
| **修改** | `src/main/java/top/playereg/pix_vision/service/UserService.java` |
| **修改** | `src/main/java/top/playereg/pix_vision/service/Impl/UserServiceImpl.java` |
| **建议** | `sql/db_pix_vision-V3.1.sql`（添加 lock_id 主键 + create_time + 索引） |

---

**文档生成时间**: 2026-05-24  
**方案版本**: v1.0
