# 私信加密存储实施计划

## 一、需求概述

| 项目 | 说明 |
|------|------|
| **加密范围** | 仅私信（`message_type = 'private'`），系统通知保持明文 |
| **加密算法** | RSACipher 混合加密（小数据 RSA，大数据 AES+RSA） |
| **密钥更换** | 管理员接口，旧密钥解密 → 新密钥加密 → 批量更新 |

---

## 二、执行阶段总览

| 阶段 | 任务 | 状态 |
|------|------|------|
| 1 | 激活 RSACipher 工具类 | 待执行 |
| 2 | 消息加密存储 | 待执行 |
| 3 | 密钥更换接口 | 待执行 |

---

## 三、详细步骤

### 阶段 1：激活 RSACipher 工具类

| 步骤 | 文件 | 改动内容 |
|------|------|----------|
| 1.1 | `RSACipher.java` | 移除 `@Deprecated` 注解 |
| 1.2 | `RSACipher.java` | 验证静态方法在 Spring Bean 初始化后可用 |

### 阶段 2：消息加密存储

| 步骤 | 文件 | 改动内容 |
|------|------|----------|
| 2.1 | `MessageServiceImpl.java` | `sendMessage` 方法：私信内容加密后存储 |
| 2.2 | `MessageServiceImpl.java` | `buildMessageVO` 方法：读取时解密私信内容 |
| 2.3 | `MessageServiceImpl.java` | `getChatHistory` 返回前解密 |
| 2.4 | `MessageServiceImpl.java` | `getConversationList` 的 last_message 解密 |

### 阶段 3：密钥更换接口

| 步骤 | 文件 | 改动内容 |
|------|------|----------|
| 3.1 | `MessageMapper.java` | 新增 `selectAllPrivateMessages` 批量查询方法 |
| 3.2 | `MessageMapper.java` | 新增 `batchUpdateMessageContent` 批量更新方法 |
| 3.3 | `MessageMapper.xml` | 对应 SQL 实现 |
| 3.4 | `AdminMessageController.java` | 新建管理员消息管理控制器 |
| 3.5 | `MessageService.java` | 新增 `rotateEncryptionKeys` 接口方法 |
| 3.6 | `MessageServiceImpl.java` | 实现密钥更换逻辑（分批处理） |

---

## 四、加密策略

```
发送私信：
  原文 → RSACipher.encryptToBase64(原文) → 存入 message 字段

读取私信：
  message 字段 → RSACipher.decryptToString(密文) → 返回给前端

会话列表：
  last_message 子查询 → 应用层解密后返回
```

---

## 五、密钥更换流程

```
POST /api/admin/message/rotate-keys

1. 验证管理员权限（角色 77）
2. 调用 RSACipher.rotateKeys()：
   a. 备份旧密钥为 .bak
   b. 生成新密钥对
   c. 保存新密钥到文件
3. 分批查询所有私信（message_type = 'private'）
4. 对每批数据：
   a. 旧密钥解密
   b. 新密钥加密
   c. 批量更新数据库
5. 返回处理结果（成功数量/失败数量）
```

---

## 六、风险与注意事项

| 风险 | 应对措施 |
|------|----------|
| 密钥更换失败 | 旧密钥已备份为 .bak，可手动恢复 |
| 大量数据更新 | 分批处理（每批 1000 条），避免内存溢出 |
| 会话列表性能 | last_message 解密在应用层，不影响 SQL 性能 |
| 系统通知加密 | 只加密私信，系统通知保持明文 |

---

## 七、相关文件清单

| 文件路径 | 说明 |
|----------|------|
| `src/main/java/top/playereg/pix_vision/util/RSACipher.java` | RSA 加解密工具类 |
| `src/main/java/top/playereg/pix_vision/service/MessageService.java` | 消息服务接口 |
| `src/main/java/top/playereg/pix_vision/service/Impl/MessageServiceImpl.java` | 消息服务实现 |
| `src/main/java/top/playereg/pix_vision/mapper/MessageMapper.java` | 消息 Mapper 接口 |
| `src/main/resources/mapper/MessageMapper.xml` | 消息 Mapper XML |
| `src/main/java/top/playereg/pix_vision/controller/admin/AdminMessageController.java` | 管理员消息控制器（新建） |
