# 昵称修改 AI 审核功能实现方案

## 1. 概述

为 `/api/user/profile/nickname/change` 接口引入 AI 内容审核能力，审核用户提交的新昵称是否包含违规内容。参考现有 `/api/comment/add` 的 AI 审核流程，复用已有的 `ContentAuditService` 组件。审核结果存入 `tb_user_data_change_lock` 表，同时根据审核状态决定是否立即更新昵称。

---

## 2. 现有组件复用分析

### 2.1 可直接复用的组件

| 组件 | 说明 |
|------|------|
| `ContentAuditService` / `ContentAuditServiceImpl` | AI 文案审核服务，调用 Python `/content/audit` 接口 |
| `ContentAuditResult` | 审核结果 POJO（status, reason, insult_words） |
| `PythonServerConfig` | Python 服务 URL 配置 |
| `PythonApiResponse<T>` | 通用 Python API 响应包装类 |
| `RestTemplate` (HttpClientConfig) | HTTP 客户端（已配置超时） |

### 2.2 参考的评论审核流程

```
Controller → CommentService.addComment() → ContentAuditService.auditContent(text)
    → Python API POST /content/audit
    → 返回 ContentAuditResult { status, reason, insult_words }
    → 映射: normal→10, neutral→20, violation→30
    → 插入评论到 tb_comments
    → 返回 CommentAddResult { success, approvalStatus, auditReason }
Controller 根据 approvalStatus 返回差异化响应
```

---

## 3. 数据库表分析

### 3.1 现有表结构 `tb_user_data_change_lock`

```sql
CREATE TABLE `tb_user_data_change_lock` (
    `user_id` int NOT NULL COMMENT '待审核用户id',
    `type` int NOT NULL COMMENT '类型：100-昵称、200-权限、300-头像',
    `nickname` varchar(48) DEFAULT NULL COMMENT '待审核昵称',
    `user_role` int DEFAULT NULL COMMENT '修改的用户角色',
    `avatar_url` varchar(96) DEFAULT NULL COMMENT '更改的用户头像路径',
    `old_data` varchar(100) NOT NULL COMMENT '旧数据，用于回滚',
    `approval_status` int NOT NULL DEFAULT '20' COMMENT '审核状态，10-通过、20-待审核、30-未过审',
    KEY `tb_user_data_change_lock_tb_user_FK` (`user_id`),
    CONSTRAINT `tb_user_data_change_lock_tb_user_FK` FOREIGN KEY (`user_id`) REFERENCES `tb_user` (`user_id`)
) COMMENT='用户信息锁表，用于存放未过审的用户信息';
```

### 3.2 改进建议

**建议添加主键** `lock_id`，用于 MyBatis-Plus 实体映射和后续的人工审核操作：

```sql
ALTER TABLE `tb_user_data_change_lock`
    ADD COLUMN `lock_id` int NOT NULL AUTO_INCREMENT PRIMARY KEY FIRST;
```

---

## 4. 核心业务流程

```
用户提交昵称修改
    ↓
Controller 提取 Token → 验证 Token → 解析 userId
    ↓
校验参数（非空、长度 1-20）
    ↓
调用 UserService.updateNicknameWithAudit(userId, newNickname, adminId)
    ↓
查询用户当前昵称（作为 old_data）
    ↓
调用 ContentAuditService.auditContent(newNickname)
    ↓
┌─ normal（通过）──────────────────────────────────────────┐
│  1. 更新 tb_user.nickname                                │
│  2. 插入 tb_user_data_change_lock（approval_status=10）  │
│  3. 返回 NicknameChangeResult(success=true, status=10)   │
└──────────────────────────────────────────────────────────┘
┌─ neutral / AI 不可用（待审核）────────────────────────────┐
│  1. 不更新 tb_user.nickname                              │
│  2. 插入 tb_user_data_change_lock（approval_status=20）  │
│  3. 返回 NicknameChangeResult(success=true, status=20)   │
└──────────────────────────────────────────────────────────┘
┌─ violation（违规）───────────────────────────────────────┐
│  1. 不更新 tb_user.nickname                              │
│  2. 插入 tb_user_data_change_lock（approval_status=30）  │
│  3. 返回 NicknameChangeResult(success=true, status=30)   │
└──────────────────────────────────────────────────────────┘
    ↓
Controller 根据 approvalStatus 返回差异化响应
```

---

## 5. 新建文件清单

### 5.1 `src/main/java/top/playereg/pix_vision/pojo/UserDataChangeLock.java`

实体类，对应 `tb_user_data_change_lock` 表。

```java
package top.playereg.pix_vision.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 用户数据变更锁定实体
 * <p>对应 tb_user_data_change_lock 表，用于存放待审核的用户信息变更记录</p>
 *
 * @author PlayerEG
 */
@Data
@TableName("tb_user_data_change_lock")
public class UserDataChangeLock {

    /** 主键 ID */
    @TableId(type = IdType.AUTO)
    private Integer lockId;

    /** 待审核用户 ID */
    private Integer userId;

    /** 修改类型：100-昵称、200-权限、300-头像 */
    private Integer type;

    /** 待审核昵称 */
    private String nickname;

    /** 修改的用户角色 */
    private Integer userRole;

    /** 更改的用户头像路径 */
    private String avatarUrl;

    /** 旧数据，用于回滚 */
    private String oldData;

    /** 审核状态：10-通过、20-待审核、30-未过审 */
    private Integer approvalStatus;
}
```

### 5.2 `src/main/java/top/playereg/pix_vision/pojo/NicknameChangeResult.java`

昵称修改结果封装类，类似 `CommentAddResult`。

```java
package top.playereg.pix_vision.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 昵称修改结果
 * <p>封装昵称修改操作的结果，包含审核状态和审核原因</p>
 *
 * @author PlayerEG
 */
@Data
@AllArgsConstructor
public class NicknameChangeResult {

    /** 是否操作成功（数据库操作成功即为 true） */
    private Boolean success;

    /**
     * 审核状态
     * <ul>
     *   <li>10 - 审核通过，昵称已更新</li>
     *   <li>20 - 待审核，昵称暂未更新</li>
     *   <li>30 - 未过审（违规），昵称未更新</li>
     * </ul>
     */
    private Integer approvalStatus;

    /** 审核原因（违规时记录 AI 审核返回的原因） */
    private String auditReason;
}
```

### 5.3 `src/main/java/top/playereg/pix_vision/mapper/UserDataChangeLockMapper.java`

```java
package top.playereg.pix_vision.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;
import top.playereg.pix_vision.pojo.UserDataChangeLock;

/**
 * 用户数据变更锁定 Mapper
 *
 * @author PlayerEG
 */
@Mapper
@Repository
public interface UserDataChangeLockMapper extends BaseMapper<UserDataChangeLock> {

    /**
     * 插入一条变更锁定记录
     *
     * @param lock 锁定记录实体
     * @return 影响行数
     */
    int insertLock(UserDataChangeLock lock);
}
```

### 5.4 `src/main/resources/mapper/UserDataChangeLockMapper.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="top.playereg.pix_vision.mapper.UserDataChangeLockMapper">

    <!-- 插入锁定记录 -->
    <insert id="insertLock" parameterType="top.playereg.pix_vision.pojo.UserDataChangeLock">
        INSERT INTO tb_user_data_change_lock
        (user_id, type, nickname, old_data, approval_status)
        VALUES
        (#{userId}, #{type}, #{nickname}, #{oldData}, #{approvalStatus})
    </insert>

</mapper>
```

---

## 6. 修改文件清单

### 6.1 `service/UserService.java` - 新增接口方法

在 `updateUserNickname` 方法声明之后，新增方法：

```java
/**
 * 更新用户昵称（带 AI 审核）
 * <p>
 * 与 {@link #updateUserNickname} 不同，此方法会先调用 AI 审核服务检查新昵称。
 * 根据审核结果决定是否立即更新昵称，并将审核记录写入 tb_user_data_change_lock 表。
 * </p>
 *
 * <h3>审核结果处理</h3>
 * <ul>
 *   <li>normal（通过）：直接更新昵称，lock 记录 approval_status=10</li>
 *   <li>neutral / AI 不可用（待审核）：不更新昵称，lock 记录 approval_status=20</li>
 *   <li>violation（违规）：不更新昵称，lock 记录 approval_status=30</li>
 * </ul>
 *
 * <h3>使用场景</h3>
 * <ol>
 *   <li>用户自行修改昵称时调用此方法（经过 AI 审核）</li>
 *   <li>管理员直接修改昵称时调用 {@link #updateUserNickname}（跳过审核）</li>
 * </ol>
 *
 * @param userId   用户 ID
 * @param nickname 新昵称
 * @param adminId  执行操作的用户 ID（用户自己更新时传自身 ID）
 * @return 昵称修改结果，包含成功状态、审核状态和审核原因
 * @author PlayerEG
 * @see #updateUserNickname(Integer, String, Integer)
 */
NicknameChangeResult updateNicknameWithAudit(Integer userId, String nickname, Integer adminId);
```

### 6.2 `service/Impl/UserServiceImpl.java` - 实现 AI 审核逻辑

需要新增的依赖注入：

```java
@Autowired
private ContentAuditService contentAuditService;

@Autowired
private UserDataChangeLockMapper userDataChangeLockMapper;
```

新增方法实现：

```java
/**
 * 更新用户昵称（带 AI 审核）
 *
 * @param userId   用户 ID
 * @param nickname 新昵称
 * @param adminId  执行操作的用户 ID
 * @return 昵称修改结果
 * @author PlayerEG
 */
@Override
@Transactional
public NicknameChangeResult updateNicknameWithAudit(Integer userId, String nickname, Integer adminId) {
    log.info("开始带AI审核的昵称修改 - 用户ID: {}, 新昵称: {}", userId, nickname);

    if (userId == null || userId <= 0) {
        log.error("用户 ID 无效: {}", userId);
        return new NicknameChangeResult(false, null, null);
    }

    if (nickname == null || nickname.isEmpty()) {
        log.error("昵称不能为空");
        return new NicknameChangeResult(false, null, null);
    }

    if (nickname.length() < 1 || nickname.length() > 20) {
        log.error("昵称长度必须在 1-20 个字符之间，当前长度: {}", nickname.length());
        return new NicknameChangeResult(false, null, null);
    }

    // 查询用户当前昵称作为 old_data
    User currentUser = userMapper.selectAllUserInfoById(userId);
    if (currentUser == null) {
        log.error("用户不存在 - 用户ID: {}", userId);
        return new NicknameChangeResult(false, null, null);
    }

    String oldNickname = currentUser.getNickname();

    // 新旧昵称相同，无需审核和更新
    if (nickname.equals(oldNickname)) {
        log.info("新旧昵称相同，跳过更新 - 用户ID: {}, 昵称: {}", userId, nickname);
        return new NicknameChangeResult(true, 10, null);
    }

    // 调用 AI 审核服务
    Integer approvalStatus = 20; // 默认待审核
    String auditReason = null;
    ContentAuditResult auditResult = contentAuditService.auditContent(nickname);

    if (auditResult != null) {
        auditReason = auditResult.getReason();
        switch (auditResult.getStatus()) {
            case "normal":
                approvalStatus = 10;
                break;
            case "neutral":
                approvalStatus = 20;
                break;
            case "violation":
                approvalStatus = 30;
                break;
            default:
                log.warn("AI 审核返回未知状态: {}, 降级为待审核", auditResult.getStatus());
                break;
        }
        log.info("AI 审核结果 - 状态: {}, 原因: {}, 命中敏感词: {}, 最终审核状态: {}",
            auditResult.getStatus(), auditResult.getReason(), auditResult.getInsult_words(), approvalStatus);
    } else {
        log.warn("AI 审核服务不可用，降级为待审核");
    }

    // 审核通过时直接更新昵称
    if (approvalStatus == 10) {
        int result = userMapper.updateUserNickname(userId, nickname, adminId);
        if (result <= 0) {
            log.error("昵称更新失败 - 用户ID: {}", userId);
            return new NicknameChangeResult(false, null, null);
        }
        log.info("昵称审核通过，已直接更新 - 用户ID: {}", userId);
    }

    // 插入锁定记录
    UserDataChangeLock lock = new UserDataChangeLock();
    lock.setUserId(userId);
    lock.setType(100); // 昵称类型
    lock.setNickname(nickname);
    lock.setOldData(oldNickname);
    lock.setApprovalStatus(approvalStatus);

    int insertResult = userDataChangeLockMapper.insertLock(lock);
    if (insertResult <= 0) {
        log.error("插入锁定记录失败 - 用户ID: {}", userId);
        return new NicknameChangeResult(false, null, null);
    }

    log.info("昵称修改锁定记录已插入 - 用户ID: {}, 审核状态: {}, lockId: {}",
        userId, approvalStatus, lock.getLockId());

    return new NicknameChangeResult(true, approvalStatus, auditReason);
}
```

**重要提醒**：需要在 `UserServiceImpl` 中注入以下两个新依赖：
- `ContentAuditService contentAuditService`（使用 `@Autowired`）
- `UserDataChangeLockMapper userDataChangeLockMapper`（使用 `@Autowired`）

### 6.3 `controller/UserProfileController.java` - 修改昵称修改接口

修改 `updateNickname` 方法的 Service 调用部分（第 543 行附近），将：

```java
// 调用服务层更新昵称
Boolean result = userService.updateUserNickname(userId, nickname, userId);

if (result) {
    log.info("用户昵称修改成功，用户 ID: {}, 用户名: {}, 新昵称: {}", userId, username, nickname);
    return ResponsePojo.success(true, "昵称修改成功");
} else {
    log.error("用户昵称修改失败，用户 ID: {}, 用户名: {}", userId, username);
    return ResponsePojo.error(false, "昵称修改失败");
}
```

替换为：

```java
// 调用带 AI 审核的昵称修改服务
NicknameChangeResult result = userService.updateNicknameWithAudit(userId, nickname, userId);

if (result.getSuccess() == null || !result.getSuccess()) {
    log.error("用户昵称修改失败，用户 ID: {}, 用户名: {}", userId, username);
    return ResponsePojo.error(false, "昵称修改失败");
}

Integer approvalStatus = result.getApprovalStatus();
String auditReason = result.getAuditReason();

// 违规内容
if (approvalStatus != null && approvalStatus == 30) {
    String reason = auditReason != null ? auditReason : "未知原因";
    log.warn("昵称审核不通过（违规），用户 ID: {}, 原因: {}", userId, reason);
    return ResponsePojo.error(false, "违规内容：" + reason);
}

// 待审核
if (approvalStatus != null && approvalStatus == 20) {
    log.info("昵称修改已提交，等待人工审核，用户 ID: {}", userId);
    return ResponsePojo.success(true, "昵称修改已提交，等待人工审核");
}

// 审核通过（10）
log.info("用户昵称修改成功，用户 ID: {}, 用户名: {}, 新昵称: {}", userId, username, nickname);
return ResponsePojo.success(true, "昵称修改成功");
```

同时需要在 Controller 文件头部新增 import：

```java
import top.playereg.pix_vision.pojo.NicknameChangeResult;
```

### 6.4 `UserProfileController` - Swagger 文档更新

更新 `@Operation` 注解的 description，在**返回说明**和**业务逻辑**部分加入 AI 审核相关内容：

**返回说明**新增：
```
- **AI 审核不通过（违规）**：返回 **{"data": false}** 和"违规内容：{原因}"提示
- **AI 审核存疑（待审核）**：返回 **{"data": true}** 和"昵称修改已提交，等待人工审核"提示
```

**业务逻辑**步骤更新为：
```
5. 调用 AI 审核服务对昵称内容进行安全审核
6. 根据审核结果：
   - 通过：直接更新昵称并记录 lock 表
   - 存疑：暂不更新，将变更记录写入 lock 表，等待人工审核
   - 违规：暂不更新，将违规记录写入 lock 表
7. 返回差异化的响应消息
```

**注意事项**新增：
```
- 昵称修改会自动调用 AI 审核服务进行内容安全审核
- AI 审核不通过（违规）时直接拦截，data 返回 false
- AI 审核存疑时标记为待审核，管理员审核通过后昵称才会更新
- AI 审核服务不可用时自动降级为待审核状态
```

---

## 7. Controller 差异化响应汇总

| 审核状态 | approvalStatus | data | message |
|---------|---------------|------|---------|
| 通过 | 10 | `true` | `"昵称修改成功"` |
| 待审核 | 20 | `true` | `"昵称修改已提交，等待人工审核"` |
| 违规 | 30 | `false` | `"违规内容：{AI审核原因}"` |
| 操作失败 | null | `false` | `"昵称修改失败"` |

---

## 8. 边缘情况与异常处理

### 8.1 新旧昵称相同

如果用户提交的新昵称与当前昵称完全一致，跳过 AI 审核和数据库操作，直接返回成功（approvalStatus=10）。

### 8.2 AI 审核服务不可用

当 `contentAuditService.auditContent()` 返回 `null`（网络异常、超时、服务宕机等），降级处理为 `approvalStatus=20`（待审核），不会阻塞用户操作。

### 8.3 事务一致性

`updateNicknameWithAudit` 方法标注 `@Transactional`，确保：
- 审核通过时，昵称更新和 lock 记录插入在同一事务中
- 任一操作失败时整体回滚

### 8.4 管理员操作不受影响

管理员批量更新用户信息（`AdminUserController`）仍调用原有的 `updateUserNickname` 方法，不经过 AI 审核，确保管理操作不受影响。

### 8.5 并发修改

短时间内连续提交同名昵称修改可能产生多条 lock 记录。后续人工审核功能开发时可通过查询最新的 `lock_id` 记录处理，当前版本仅做插入不做去重。

---

## 9. 实施步骤

| 序号 | 步骤 | 文件 | 说明 |
|------|------|------|------|
| 1 | 数据库变更 | `sql/` | 为 `tb_user_data_change_lock` 添加 `lock_id` 自增主键（建议） |
| 2 | 新建 POJO | `pojo/UserDataChangeLock.java` | 锁定表实体类 |
| 3 | 新建结果类 | `pojo/NicknameChangeResult.java` | 昵称修改结果封装 |
| 4 | 新建 Mapper | `mapper/UserDataChangeLockMapper.java` | Mapper 接口 |
| 5 | 新建 XML | `resources/mapper/UserDataChangeLockMapper.xml` | SQL 映射 |
| 6 | 修改接口 | `service/UserService.java` | 新增 `updateNicknameWithAudit` 方法签名 |
| 7 | 修改实现 | `service/Impl/UserServiceImpl.java` | 实现 AI 审核逻辑（注入新依赖） |
| 8 | 修改 Controller | `controller/UserProfileController.java` | 调用新方法 + 差异化响应 + 更新 Swagger 文档 |

---

## 10. 文件变更汇总

| 操作 | 文件路径 |
|------|---------|
| **新建** | `src/main/java/top/playereg/pix_vision/pojo/UserDataChangeLock.java` |
| **新建** | `src/main/java/top/playereg/pix_vision/pojo/NicknameChangeResult.java` |
| **新建** | `src/main/java/top/playereg/pix_vision/mapper/UserDataChangeLockMapper.java` |
| **新建** | `src/main/resources/mapper/UserDataChangeLockMapper.xml` |
| **修改** | `src/main/java/top/playereg/pix_vision/service/UserService.java` |
| **修改** | `src/main/java/top/playereg/pix_vision/service/Impl/UserServiceImpl.java` |
| **修改** | `src/main/java/top/playereg/pix_vision/controller/UserProfileController.java` |
| **建议** | `sql/db_pix_vision-V3.1.sql`（添加 lock_id 主键） |

---

**文档生成时间**: 2026-05-24  
**方案版本**: v1.0
