# 消息删除逻辑修复方案

## 问题描述
当前删除消息使用单一的 `is_delete` 字段，导致：
- B 向 A 发送消息
- A 删除消息后，B 也看不到这条消息了

## 解决方案
将 `is_delete` 改为两个字段，实现"按用户删除"：
- `is_delete_by_sender`：发送者是否删除
- `is_delete_by_receiver`：接收者是否删除

只有双方都删除，消息才真正不可见。

---

## 1. 数据库修改

### SQL 语句
```sql
-- 添加新字段
ALTER TABLE tb_messages 
ADD COLUMN `is_delete_by_sender` tinyint(1) NOT NULL DEFAULT 0 COMMENT '发送者删除标记：0-未删除、1-已删除' AFTER `is_delete`,
ADD COLUMN `is_delete_by_receiver` tinyint(1) NOT NULL DEFAULT 0 COMMENT '接收者删除标记：0-未删除、1-已删除' AFTER `is_delete_by_sender`;

-- 迁移数据：将原有 is_delete=1 的记录根据发送者/接收者关系设置对应字段
UPDATE tb_messages 
SET is_delete_by_sender = CASE WHEN from_user_id = `to` THEN 1 ELSE 0 END,
    is_delete_by_receiver = 1
WHERE is_delete = 1;

-- 删除旧字段（可选，建议保留一段时间观察）
-- ALTER TABLE tb_messages DROP COLUMN is_delete;
```

### 添加索引
```sql
CREATE INDEX idx_msg_sender_delete ON tb_messages (from_user_id, is_delete_by_sender);
CREATE INDEX idx_msg_receiver_delete ON tb_messages (`to`, is_delete_by_receiver);
```

---

## 2. 实体类修改

### Message.java
```java
/**
 * 发送者删除标记：0-未删除、1-已删除
 */
@Schema(description = "发送者删除标记：0-未删除、1-已删除", example = "false")
private Boolean is_delete_by_sender;

/**
 * 接收者删除标记：0-未删除、1-已删除
 */
@Schema(description = "接收者删除标记：0-未删除、1-已删除", example = "false")
private Boolean is_delete_by_receiver;
```

### 删除旧字段
```java
// 删除以下字段
// private Boolean is_delete;
```

---

## 3. Mapper XML 修改

### 会话列表查询
```xml
<select id="selectConversationList" resultType="top.playereg.pix_vision.pojo.VO.ConversationVO">
    SELECT 
        t.other_user_id,
        (SELECT m2.message FROM tb_messages m2 
         WHERE m2.message_type = 'private'
           AND ((m2.from_user_id = #{userId} AND m2.`to` = t.other_user_id AND m2.is_delete_by_sender = 0)
                OR (m2.from_user_id = t.other_user_id AND m2.`to` = #{userId} AND m2.is_delete_by_receiver = 0))
         ORDER BY m2.create_time DESC LIMIT 1) AS last_message,
        MAX(t.create_time) AS last_message_time,
        SUM(CASE WHEN t.is_received = 1 AND t.is_read = 0 THEN 1 ELSE 0 END) AS unread_count
    FROM (
        SELECT 
            m.message_id,
            CASE WHEN m.from_user_id = #{userId} THEN m.`to` ELSE m.from_user_id END AS other_user_id,
            m.create_time,
            m.is_read,
            CASE WHEN m.`to` = #{userId} THEN 1 ELSE 0 END AS is_received
        FROM tb_messages m
        WHERE m.message_type = 'private'
          AND (m.from_user_id = #{userId} OR m.`to` = #{userId})
          AND (
            (m.from_user_id = #{userId} AND m.is_delete_by_sender = 0)
            OR 
            (m.`to` = #{userId} AND m.is_delete_by_receiver = 0)
          )
    ) t
    GROUP BY t.other_user_id
    ORDER BY MAX(t.create_time) DESC
</select>
```

### 聊天记录查询
```xml
<select id="selectChatHistory" resultType="top.playereg.pix_vision.pojo.VO.MessageVO">
    SELECT m.message_id,
           m.message,
           m.project,
           m.from_user_id,
           m.message_type,
           m.ref_id,
           m.`to`,
           m.is_read,
           m.create_time,
           CASE WHEN m.from_user_id = 0 THEN 'system' ELSE u.username END AS from_username,
           CASE WHEN m.from_user_id = 0 THEN '系统通知' ELSE u.nickname END AS from_nickname,
           CASE WHEN m.from_user_id = 0 THEN NULL ELSE u.avatar_url END AS from_avatar_url
    FROM tb_messages m
    LEFT JOIN tb_user u ON m.from_user_id = u.user_id AND m.from_user_id > 0
    WHERE m.message_type = 'private'
      AND ((m.from_user_id = #{userId} AND m.`to` = #{otherUserId})
           OR (m.from_user_id = #{otherUserId} AND m.`to` = #{userId}))
      AND (
        (m.from_user_id = #{userId} AND m.is_delete_by_sender = 0)
        OR 
        (m.`to` = #{userId} AND m.is_delete_by_receiver = 0)
      )
    ORDER BY m.create_time DESC
</select>
```

### 系统通知查询
```xml
<select id="selectSystemMessages" resultType="top.playereg.pix_vision.pojo.VO.MessageVO">
    SELECT m.message_id,
           m.message,
           m.project,
           m.from_user_id,
           m.message_type,
           m.ref_id,
           m.`to`,
           m.is_read,
           m.create_time,
           CASE WHEN m.from_user_id = 0 THEN 'system' ELSE u.username END AS from_username,
           CASE WHEN m.from_user_id = 0 THEN '系统通知' ELSE u.nickname END AS from_nickname,
           CASE WHEN m.from_user_id = 0 THEN NULL ELSE u.avatar_url END AS from_avatar_url
    FROM tb_messages m
    LEFT JOIN tb_user u ON m.from_user_id = u.user_id AND m.from_user_id > 0
    WHERE m.message_type = 'system'
      AND m.`to` = #{userId}
      AND m.is_delete_by_receiver = 0
      <if test="project != null and project != ''">
          AND m.project = #{project}
      </if>
      <if test="isRead != null">
          AND m.is_read = #{isRead}
      </if>
    ORDER BY m.create_time DESC
</select>
```

### 删除消息（新）
```xml
<!-- 删除消息（按用户） -->
<update id="deleteMessageByUser">
    UPDATE tb_messages
    SET 
        <if test="isSender">
            is_delete_by_sender = 1
        </if>
        <if test="isReceiver">
            is_delete_by_receiver = 1
        </if>
    WHERE message_id = #{messageId}
      AND (
        <if test="isSender">
            (from_user_id = #{userId} AND is_delete_by_sender = 0)
        </if>
        <if test="isSender and isReceiver">
            OR
        </if>
        <if test="isReceiver">
            (`to` = #{userId} AND is_delete_by_receiver = 0)
        </if>
      )
</update>

<!-- 批量删除消息（按用户） -->
<update id="batchDeleteMessageByUser">
    UPDATE tb_messages
    SET 
        <if test="isSender">
            is_delete_by_sender = 1
        </if>
        <if test="isReceiver">
            is_delete_by_receiver = 1
        </if>
    WHERE message_id IN
    <foreach collection="messageIds" item="id" open="(" separator="," close=")">
        #{id}
    </foreach>
    AND (
        <if test="isSender">
            (from_user_id = #{userId} AND is_delete_by_sender = 0)
        </if>
        <if test="isSender and isReceiver">
            OR
        </if>
        <if test="isReceiver">
            (`to` = #{userId} AND is_delete_by_receiver = 0)
        </if>
    )
</update>
```

---

## 4. Service 层修改

### MessageServiceImpl.java
```java
/**
 * 删除消息（软删除 - 按用户）
 * 
 * @param userId    用户ID
 * @param messageId 消息ID
 * @return 是否成功
 */
@Override
public boolean deleteMessage(Integer userId, Integer messageId) {
    try {
        Message message = messageMapper.selectById(messageId);
        if (message == null) {
            log.warn("消息不存在，消息ID：{}", messageId);
            return false;
        }
        
        boolean isSender = message.getFrom_user_id().equals(userId);
        boolean isReceiver = message.getTo().equals(userId);
        
        // 只能删除自己发送或接收的消息
        if (!isSender && !isReceiver) {
            log.warn("无权删除消息，用户：{}，消息ID：{}", userId, messageId);
            return false;
        }
        
        int count = messageMapper.deleteMessageByUser(messageId, userId, isSender, isReceiver);
        log.debug("删除消息成功，用户：{}，消息ID：{}，是发送者：{}，是接收者：{}", userId, messageId, isSender, isReceiver);
        return count > 0;
    } catch (Exception e) {
        log.error("删除消息失败，用户：{}，消息ID：{}，错误：{}", userId, messageId, e.getMessage());
        return false;
    }
}

/**
 * 批量删除消息（软删除 - 按用户）
 *
 * @param userId     用户ID
 * @param messageIds 消息ID列表
 * @return 是否成功
 */
@Override
public boolean batchDeleteMessages(Integer userId, List<Integer> messageIds) {
    if (messageIds == null || messageIds.isEmpty()) {
        return true;
    }
    try {
        // 需要逐个判断是发送者还是接收者
        int successCount = 0;
        for (Integer messageId : messageIds) {
            if (deleteMessage(userId, messageId)) {
                successCount++;
            }
        }
        log.debug("批量删除消息成功，用户：{}，消息数量：{}，成功数量：{}", userId, messageIds.size(), successCount);
        return successCount > 0;
    } catch (Exception e) {
        log.error("批量删除消息失败，用户：{}，错误：{}", userId, e.getMessage());
        return false;
    }
}
```

---

## 5. Mapper 接口修改

### MessageMapper.java
```java
/**
 * 删除消息（按用户标记）
 *
 * @param messageId 消息ID
 * @param userId    用户ID
 * @param isSender  是否是发送者
 * @param isReceiver 是否是接收者
 * @return 影响行数
 */
int deleteMessageByUser(@Param("messageId") Integer messageId, 
                        @Param("userId") Integer userId,
                        @Param("isSender") boolean isSender,
                        @Param("isReceiver") boolean isReceiver);

/**
 * 批量删除消息（按用户标记）
 *
 * @param userId     用户ID
 * @param messageIds 消息ID列表
 * @param isSender   是否是发送者
 * @param isReceiver 是否是接收者
 * @return 影响行数
 */
int batchDeleteMessageByUser(@Param("userId") Integer userId,
                             @Param("messageIds") List<Integer> messageIds,
                             @Param("isSender") boolean isSender,
                             @Param("isReceiver") boolean isReceiver);
```

---

## 6. 未读消息统计修改

### MessageMapper.xml
```xml
<!-- 未读私信数量 -->
<select id="selectUnreadPrivateCount" resultType="java.lang.Integer">
    SELECT COUNT(*)
    FROM tb_messages
    WHERE `to` = #{userId}
      AND message_type = 'private'
      AND is_read = 0
      AND is_delete_by_receiver = 0
</select>

<!-- 未读系统通知数量 -->
<select id="selectUnreadSystemCount" resultType="java.lang.Integer">
    SELECT COUNT(*)
    FROM tb_messages
    WHERE `to` = #{userId}
      AND message_type = 'system'
      AND is_read = 0
      AND is_delete_by_receiver = 0
</select>

<!-- 总未读数量 -->
<select id="selectUnreadCount" resultType="java.lang.Integer">
    SELECT COUNT(*)
    FROM tb_messages
    WHERE `to` = #{userId}
      AND is_read = 0
      AND is_delete_by_receiver = 0
</select>
```

---

## 7. 标记已读逻辑修改

### markConversationAsRead
```xml
<update id="markConversationAsRead">
    UPDATE tb_messages
    SET is_read = 1
    WHERE message_type = 'private'
      AND `to` = #{userId}
      AND from_user_id = #{otherUserId}
      AND is_read = 0
      AND is_delete_by_receiver = 0
</update>
```

---

## 8. 清理逻辑（可选）

### 定时任务：清理双方都删除的消息
```java
/**
 * 清理双方都删除的消息（物理删除或归档）
 * 建议每天凌晨执行一次
 */
@Scheduled(cron = "0 0 2 * * ?")
public void cleanDeletedMessages() {
    // 删除双方都标记删除的消息（超过7天）
    messageMapper.physicalDeleteOldMessages(7);
}
```

### SQL
```xml
<!-- 物理删除双方都删除超过7天的消息 -->
<delete id="physicalDeleteOldMessages">
    DELETE FROM tb_messages
    WHERE is_delete_by_sender = 1
      AND is_delete_by_receiver = 1
      AND create_time < DATE_SUB(NOW(), INTERVAL #{days} DAY)
</delete>
```

---

## 测试场景

### 场景1：A 删除消息，B 仍可见
1. B 发送消息给 A
2. A 删除消息
3. 验证：A 看不到消息，B 仍能看到

### 场景2：双方都删除
1. B 发送消息给 A
2. A 删除消息
3. B 删除消息
4. 验证：双方都看不到消息

### 场景3：发送者删除自己发送的消息
1. A 发送消息给 B
2. A 删除消息
3. 验证：A 看不到消息，B 仍能看到

### 场景4：会话列表显示
1. A 和 B 有多条消息
2. A 删除部分消息
3. 验证：A 的会话列表只显示未删除的消息

---

## 迁移步骤

1. 执行数据库 ALTER TABLE 添加新字段
2. 迁移数据（将旧的 is_delete 映射到新字段）
3. 更新实体类 Message.java
4. 更新 MessageMapper.java 和 MessageMapper.xml
5. 更新 MessageServiceImpl.java
6. 测试所有场景
7. 确认无问题后，删除旧字段（可选）
