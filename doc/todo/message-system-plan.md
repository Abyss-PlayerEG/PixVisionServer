# 站内消息系统实现计划书

> 目标：基于现有 `tb_messages` 表实现统一的消息系统，支持私信、系统通知、互动通知等场景
> 核心设计：系统发送统一作为用户处理（`from_user_id=0` 表示系统）
> 技术栈：Spring Boot 3.3 + WebSocket + MySQL + Redis
> 总计：10 新建 + 5 修改 = **15 个文件**
> 审查状态：**已通过审查** - 2026-06-11

---

## 执行步骤总览

| Step | 阶段 | 文件数 | 内容 | 依赖 |
|:---:|------|:---:|------|:---:|
| 1 | 数据库 | 1 修改 | V4.0 SQL 确认（已完成） | 无 |
| 2 | 枚举常量 | 2 新建 | MessageType + MessageProject 枚举 | 无 |
| 3 | 实体层 | 1 新建 | Message 实体类 | Step 2 |
| 4 | VO 层 | 2 新建 | MessageVO + ConversationVO | Step 3 |
| 5 | Mapper 层 | 1 新建 | MessageMapper 接口 + XML | Step 3 |
| 6 | WebSocket 核心 | 3 新建 | Config + Handler + SessionManager | 无 |
| 7 | Service 层 | 1 新建 | MessageService 接口 + 实现 | Step 3,5,6 |
| 8 | Controller 层 | 1 新建 | MessageController | Step 7 |
| 9 | 业务集成 | 5 修改 | 各业务模块发送通知 | Step 7 |
| 10 | 验证 | - | 全量编译 + 功能测试 | Step 1~9 |

---

## 一、数据库设计（已完成）

### 1.1 当前 tb_messages 表结构（V4.0）

```sql
CREATE TABLE `tb_messages` (
  `message_id` int NOT NULL AUTO_INCREMENT COMMENT 'id唯一值',
  `message` varchar(512) COMMENT '消息内容',
  `project` varchar(64) COMMENT '消息主题（MessageType.code）',
  `from_user_id` int DEFAULT NULL COMMENT '发送者用户ID（系统消息为0）',
  `message_type` varchar(32) NOT NULL DEFAULT 'system' COMMENT '消息类型：system-系统通知、private-私信',
  `to` int DEFAULT NULL COMMENT '接收者用户ID',
  `is_read` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否已读，0 - 未读、1 - 已读',
  `is_delete` tinyint(1) NOT NULL DEFAULT '0' COMMENT '软删除，0 - 未删除、1 - 已删除',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`message_id`),
  KEY `idx_from_user_id` (`from_user_id`),
  KEY `idx_message_type` (`message_type`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_conversation` (`from_user_id`,`to`,`message_type`)
);
```

### 1.2 设计说明

| 字段 | 用途 | 说明 |
|------|------|------|
| `from_user_id` | 发送者 | **系统消息统一使用 `0`**，表示系统用户 |
| `to` | 接收者 | 普通用户 user_id |
| `message_type` | 消息类型 | `system` - 系统通知，`private` - 私信 |
| `project` | 消息主题 | 细分消息类型（审核、点赞、评论等） |

### 1.3 验证
- [x] V4.0 SQL 已包含升级后的表结构
- [x] 索引已创建

---

## 二、枚举常量定义（2 个文件）

### 2.1 MessageType 消息类型枚举
- **路径**: `enums/MessageType.java`

```java
public enum MessageType {
    SYSTEM("system", "系统通知"),    // 系统公告、审核结果等
    PRIVATE("private", "私信");      // 用户私信
    
    private final String code;
    private final String desc;
}
```

### 2.2 MessageProject 消息主题枚举
- **路径**: `enums/MessageProject.java`

```java
public enum MessageProject {
    // 系统通知类（from_user_id = 0）
    WORK_AUDIT("work_audit", "作品审核"),
    SERIES_AUDIT("series_audit", "合集审核"),
    COMMENT_AUDIT("comment_audit", "评论审核"),
    SYSTEM_NOTICE("system_notice", "系统公告"),
    ACCOUNT_NOTICE("account_notice", "账号通知"),
    
    // 互动通知类（from_user_id = 实际用户）
    LIKE("like", "点赞"),
    STAR("star", "收藏"),
    COMMENT("comment", "评论"),
    
    // 私信类
    PRIVATE_MESSAGE("private_message", "私信");
    
    private final String code;
    private final String desc;
}
```

### 2.3 验证
- [ ] 编译通过

---

## 三、实体层（1 个文件）

### 3.1 Message 实体类
- **路径**: `pojo/entity/Message.java`
- **注解**: `@Data`, `@TableName("tb_messages")`, `@TableId(type = IdType.AUTO)`
- **说明**: 使用 `@TableLogic` 注解自动处理软删除，无需在 SQL 中手动过滤

```java
@Data
@TableName("tb_messages")
@Schema(description = "消息实体")
public class Message {
    
    @TableId(type = IdType.AUTO)
    @Schema(description = "消息ID")
    private Integer message_id;
    
    @Schema(description = "消息内容")
    private String message;
    
    @Schema(description = "消息主题（MessageProject.code）")
    private String project;
    
    @Schema(description = "发送者用户ID（系统消息为0）")
    private Integer from_user_id;
    
    @Schema(description = "消息类型（MessageType.code）")
    private String message_type;
    
    @Schema(description = "接收者用户ID")
    private Integer to;
    
    @Schema(description = "是否已读：0-未读、1-已读")
    private Boolean is_read;
    
    @TableLogic
    @Schema(description = "软删除：0-未删除、1-已删除")
    private Boolean is_delete;
    
    @Schema(description = "创建时间")
    private LocalDateTime create_time;
}
```

**重要说明**：
- `@TableLogic` 注解会让 MyBatis-Plus 自动在查询时添加 `is_delete = 0` 条件
- 使用 `BaseMapper` 的查询方法会自动过滤已删除数据
- 自定义 XML 中的 SQL 需要手动添加 `is_delete = 0` 条件

### 3.2 验证
- [ ] 编译通过
- [ ] 字段与数据库列名对应正确（snake_case）

---

## 四、VO 层（2 个文件）

### 4.1 MessageVO 消息详情 VO
- **路径**: `pojo/VO/MessageVO.java`
- **说明**: 继承 Message，额外携带发送者信息

```java
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "消息详情VO")
public class MessageVO extends Message {
    
    @Schema(description = "发送者用户名")
    private String from_username;
    
    @Schema(description = "发送者昵称")
    private String from_nickname;
    
    @Schema(description = "发送者头像")
    private String from_avatar_url;
}
```

### 4.2 ConversationVO 会话列表 VO
- **路径**: `pojo/VO/ConversationVO.java`
- **说明**: 会话列表展示用

```java
@Data
@Schema(description = "会话列表VO")
public class ConversationVO {
    
    @Schema(description = "对方用户ID")
    private Integer other_user_id;
    
    @Schema(description = "对方用户名")
    private String other_username;
    
    @Schema(description = "对方昵称")
    private String other_nickname;
    
    @Schema(description = "对方头像")
    private String other_avatar_url;
    
    @Schema(description = "最后一条消息内容")
    private String last_message;
    
    @Schema(description = "最后消息时间")
    private LocalDateTime last_message_time;
    
    @Schema(description = "未读消息数量")
    private Integer unread_count;
}
```

### 4.3 验证
- [ ] 编译通过

---

## 五、Mapper 层（1 个文件）

### 5.1 MessageMapper 接口
- **路径**: `mapper/MessageMapper.java`
- `extends BaseMapper<Message>`

```java
@Mapper
@Repository
public interface MessageMapper extends BaseMapper<Message> {
    
    /**
     * 查询用户未读消息数量
     */
    int selectUnreadCount(@Param("userId") Integer userId);
    
    /**
     * 查询用户未读私信数量
     */
    int selectUnreadPrivateCount(@Param("userId") Integer userId);
    
    /**
     * 查询用户未读系统通知数量
     */
    int selectUnreadSystemCount(@Param("userId") Integer userId);
    
    /**
     * 分页查询会话列表（按最后消息时间排序）
     */
    IPage<ConversationVO> selectConversationList(
        Page<Message> page,
        @Param("userId") Integer userId
    );
    
    /**
     * 分页查询与某用户的聊天记录
     */
    IPage<MessageVO> selectChatHistory(
        Page<Message> page,
        @Param("userId") Integer userId,
        @Param("otherUserId") Integer otherUserId
    );
    
    /**
     * 分页查询系统通知列表
     */
    IPage<MessageVO> selectSystemMessages(
        Page<Message> page,
        @Param("userId") Integer userId,
        @Param("project") String project,
        @Param("isRead") Boolean isRead
    );
    
    /**
     * 标记与某用户的所有私信为已读
     */
    int markConversationAsRead(
        @Param("userId") Integer userId,
        @Param("otherUserId") Integer otherUserId
    );
    
    /**
     * 批量标记消息已读
     */
    int batchMarkAsRead(
        @Param("userId") Integer userId,
        @Param("messageIds") List<Integer> messageIds
    );
    
    /**
     * 全部标记已读
     */
    int markAllAsRead(
        @Param("userId") Integer userId,
        @Param("messageType") String messageType
    );
    
    /**
     * 批量软删除消息
     */
    int batchSoftDelete(
        @Param("userId") Integer userId,
        @Param("messageIds") List<Integer> messageIds
    );
}
```

### 5.2 MessageMapper.xml
- **路径**: `resources/mapper/MessageMapper.xml`

**关键 SQL 实现**:

#### 会话列表查询
```sql
<select id="selectConversationList" resultType="ConversationVO">
    SELECT 
        CASE WHEN from_user_id = #{userId} THEN `to` ELSE from_user_id END AS other_user_id,
        (SELECT message FROM tb_messages m2 
         WHERE m2.message_type = 'private' 
           AND ((m2.from_user_id = #{userId} AND m2.`to` = CASE WHEN m.from_user_id = #{userId} THEN m.`to` ELSE m.from_user_id END)
                OR (m2.from_user_id = CASE WHEN m.from_user_id = #{userId} THEN m.`to` ELSE m.from_user_id END AND m2.`to` = #{userId}))
           AND m2.is_delete = 0
         ORDER BY m2.create_time DESC LIMIT 1) AS last_message,
        MAX(m.create_time) AS last_message_time,
        SUM(CASE WHEN m.`to` = #{userId} AND m.is_read = 0 THEN 1 ELSE 0 END) AS unread_count
    FROM tb_messages m
    WHERE m.message_type = 'private'
      AND (m.from_user_id = #{userId} OR m.`to` = #{userId})
      AND m.is_delete = 0
    GROUP BY CASE WHEN m.from_user_id = #{userId} THEN m.`to` ELSE m.from_user_id END
    ORDER BY MAX(m.create_time) DESC
</select>
```

**注意**：MySQL 8.0 严格模式下 GROUP BY 不能直接使用别名，需要使用完整表达式

#### 聊天记录查询
```sql
<select id="selectChatHistory" resultType="MessageVO">
    SELECT m.*, 
           CASE WHEN m.from_user_id = 0 THEN 'system' ELSE u.username END AS from_username,
           CASE WHEN m.from_user_id = 0 THEN '系统通知' ELSE u.nickname END AS from_nickname,
           CASE WHEN m.from_user_id = 0 THEN NULL ELSE u.avatar_url END AS from_avatar_url
    FROM tb_messages m
    LEFT JOIN tb_user u ON m.from_user_id = u.user_id AND m.from_user_id > 0
    WHERE m.message_type = 'private'
      AND ((m.from_user_id = #{userId} AND m.`to` = #{otherUserId})
           OR (m.from_user_id = #{otherUserId} AND m.`to` = #{userId}))
      AND m.is_delete = 0
    ORDER BY m.create_time DESC
</select>
```

**注意**：系统消息（from_user_id=0）在 tb_user 表中不存在，需要特殊处理

### 5.3 验证
- [ ] 编译通过
- [ ] MyBatis 绑定正确
- [ ] SQL 语句执行正确

---

## 六、WebSocket 核心层（3 个文件）

### 6.1 WebSocket 配置类
- **路径**: `config/WebSocketConfig.java`
- **注解**: `@Configuration`, `@EnableWebSocket`
- **实现**: `WebSocketConfigurer`

```java
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    
    @Autowired
    private NotificationWebSocketHandler notificationHandler;
    
    @Autowired
    private WebSocketAuthInterceptor authInterceptor;
    
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(notificationHandler, "/api/ws/notification")
                .addInterceptors(authInterceptor)
                .setAllowedOrigins("*");
    }
}
```

### 6.2 WebSocket 认证拦截器
- **路径**: `handler/WebSocketAuthInterceptor.java`
- **实现**: `HandshakeInterceptor`
- **逻辑**:
  - 从 URL 参数提取 `token`
  - 验证 JWT 有效性
  - 验证 Token 白名单
  - 将 `userId` 存入 WebSocketSession attributes

### 6.3 WebSocket 消息处理器
- **路径**: `handler/NotificationWebSocketHandler.java`
- **继承**: `TextWebSocketHandler`

```java
@Component
public class NotificationWebSocketHandler extends TextWebSocketHandler {
    
    @Autowired
    private WebSocketSessionManager sessionManager;
    
    private static final PixVisionLogger log = PixVisionLogger.create(NotificationWebSocketHandler.class);
    
    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Integer userId = (Integer) session.getAttributes().get("userId");
        sessionManager.addSession(userId, session);
        log.info("WebSocket 连接建立，用户 ID: {}", userId);
    }
    
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Integer userId = (Integer) session.getAttributes().get("userId");
        sessionManager.removeSession(userId);
        log.info("WebSocket 连接关闭，用户 ID: {}", userId);
    }
    
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        // 处理心跳 PING/PONG
        if ("PING".equals(message.getPayload())) {
            session.sendMessage(new TextMessage("PONG"));
        }
    }
    
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("WebSocket 传输错误: {}", exception.getMessage());
        sessionManager.removeSession((Integer) session.getAttributes().get("userId"));
    }
}
```

### 6.4 WebSocket 会话管理器
- **路径**: `manager/WebSocketSessionManager.java`
- **注解**: `@Component`

```java
@Component
public class WebSocketSessionManager {
    
    private final ConcurrentHashMap<Integer, WebSocketSession> sessions = new ConcurrentHashMap<>();
    
    public void addSession(Integer userId, WebSocketSession session) {
        sessions.put(userId, session);
    }
    
    public void removeSession(Integer userId) {
        sessions.remove(userId);
    }
    
    public WebSocketSession getSession(Integer userId) {
        return sessions.get(userId);
    }
    
    public boolean isOnline(Integer userId) {
        WebSocketSession session = sessions.get(userId);
        return session != null && session.isOpen();
    }
    
    public void sendMessage(Integer userId, String message) {
        WebSocketSession session = sessions.get(userId);
        if (session != null && session.isOpen()) {
            try {
                session.sendMessage(new TextMessage(message));
            } catch (IOException e) {
                // 处理发送失败
            }
        }
    }
    
    public int getOnlineCount() {
        return sessions.size();
    }
}
```

### 6.5 验证
- [ ] 编译通过
- [ ] WebSocket 连接成功
- [ ] 认证拦截器工作正常
- [ ] 心跳机制正常

---

## 七、Service 层（1 个文件）

### 7.1 MessageService 接口
- **路径**: `service/MessageService.java`

```java
public interface MessageService {
    
    /**
     * 发送消息（通用方法）
     * @param fromUserId 发送者（系统消息传 0）
     * @param toUserId 接收者
     * @param content 消息内容
     * @param type 消息类型
     * @param project 消息主题
     */
    void sendMessage(Integer fromUserId, Integer toUserId, String content, 
                     MessageType type, MessageProject project);
    
    /**
     * 发送私信
     */
    void sendPrivateMessage(Integer fromUserId, Integer toUserId, String content);
    
    /**
     * 发送系统通知（from_user_id = 0）
     */
    void sendSystemNotice(Integer toUserId, String content, MessageProject project);
    
    /**
     * 获取未读消息数量
     * <p>
     * 返回 Map 结构：
     * - total: 总未读数
     * - private: 未读私信数
     * - system: 未读系统通知数
     * </p>
     */
    Map<String, Integer> getUnreadCount(Integer userId);
    
    /**
     * 分页查询会话列表
     */
    IPage<ConversationVO> getConversationList(Page<Message> page, Integer userId);
    
    /**
     * 分页查询聊天记录
     */
    IPage<MessageVO> getChatHistory(Page<Message> page, Integer userId, Integer otherUserId);
    
    /**
     * 分页查询系统通知
     */
    IPage<MessageVO> getSystemMessages(Page<Message> page, Integer userId, 
                                       String project, Boolean isRead);
    
    /**
     * 标记会话已读
     */
    boolean markConversationAsRead(Integer userId, Integer otherUserId);
    
    /**
     * 全部标记已读
     */
    boolean markAllAsRead(Integer userId, String messageType);
    
    /**
     * 删除消息（软删除）
     */
    boolean deleteMessage(Integer userId, Integer messageId);
    
    /**
     * 批量删除消息（软删除）
     */
    boolean batchDeleteMessages(Integer userId, List<Integer> messageIds);
}
```

### 7.2 MessageServiceImpl 实现
- **路径**: `service/Impl/MessageServiceImpl.java`
- **注入**: `MessageMapper`, `WebSocketSessionManager`, `UserMapper`

**核心逻辑**:
```java
@Override
public void sendMessage(Integer fromUserId, Integer toUserId, String content, 
                        MessageType type, MessageProject project) {
    // 1. 创建消息实体
    Message message = new Message();
    message.setFrom_user_id(fromUserId);
    message.setTo(toUserId);
    message.setMessage(content);
    message.setMessage_type(type.getCode());
    message.setProject(project.getCode());
    message.setIs_read(false);
    message.setIs_delete(false);
    message.setCreate_time(LocalDateTime.now());
    
    // 2. 写入数据库
    messageMapper.insert(message);
    
    // 3. 构建推送消息
    Map<String, Object> pushData = new HashMap<>();
    pushData.put("type", "notification");
    pushData.put("data", buildMessageVO(message));
    
    // 4. WebSocket 推送
    if (sessionManager.isOnline(toUserId)) {
        sessionManager.sendMessage(toUserId, JSON.toJSONString(pushData));
    }
    // 离线用户：消息已持久化，上线后可查询
}
```

### 7.3 验证
- [ ] 编译通过
- [ ] 消息写入数据库成功
- [ ] 在线用户收到 WebSocket 推送

---

## 八、Controller 层（1 个文件）

### 8.1 MessageController
- **路径**: `controller/MessageController.java`
- **注解**: `@RestController`, `@RequestMapping("/api/message")`

| 接口 | 方法 | 说明 | 认证 |
|------|------|------|------|
| `/unread-count` | GET | 获取未读消息数量 | 需要 |
| `/conversations/{current}/{size}` | GET | 分页查询会话列表 | 需要 |
| `/chat/{otherUserId}/{current}/{size}` | GET | 分页查询聊天记录 | 需要 |
| `/system/{current}/{size}` | GET | 分页查询系统通知 | 需要 |
| `/send` | POST | 发送私信 | 需要 |
| `/read/conversation/{otherUserId}` | POST | 标记会话已读 | 需要 |
| `/read-all` | POST | 全部标记已读 | 需要 |
| `/delete/{messageId}` | POST | 删除单条消息 | 需要 |
| `/batch-delete` | POST | 批量删除消息 | 需要 |

### 8.2 Swagger 文档
- 每个接口添加 `@Operation` 注解
- 遵循项目文档规范

### 8.3 验证
- [ ] 编译通过
- [ ] Swagger 文档显示正确
- [ ] 接口功能正常

---

## 九、业务集成（5 个修改文件）

### 9.1 作品审核通知
- **文件**: `service/Impl/WorkServiceImpl.java`
- **改动点**: `uploadWork()` 方法，AI 审核完成后发送通知
- **调用**: `messageService.sendSystemNotice(userId, content, MessageProject.WORK_AUDIT)`

### 9.2 合集审核通知
- **文件**: `service/Impl/SeriesServiceImpl.java`
- **改动点**: `addSeries()` / `updateSeriesInfo()` 方法
- **调用**: `messageService.sendSystemNotice(userId, content, MessageProject.SERIES_AUDIT)`

### 9.3 评论审核通知
- **文件**: `service/Impl/CommentServiceImpl.java`
- **改动点**: `addComment()` 方法
- **调用**: `messageService.sendSystemNotice(userId, content, MessageProject.COMMENT_AUDIT)`

### 9.4 点赞通知
- **文件**: `service/Impl/LikeServiceImpl.java`
- **改动点**: `toggleLike()` 方法，点赞成功后通知作品作者
- **调用**: `messageService.sendMessage(fromUserId, workAuthorId, content, MessageType.SYSTEM, MessageProject.LIKE)`
- **自排除**: `if (!fromUserId.equals(workAuthorId))` — 不给自己发通知

### 9.5 收藏通知
- **文件**: `service/Impl/StarServiceImpl.java`
- **改动点**: `toggleStar()` 方法，收藏成功后通知作品作者
- **调用**: `messageService.sendMessage(fromUserId, workAuthorId, content, MessageType.SYSTEM, MessageProject.STAR)`
- **自排除**: `if (!fromUserId.equals(workAuthorId))` — 不给自己发通知

### 9.6 验证
- [ ] 编译通过
- [ ] 各业务场景触发通知正确

---

## 十、可选扩展（不在本次范围内）

- [ ] Redis Pub/Sub 支持多实例部署
- [ ] 消息免打扰设置
- [ ] 消息撤回功能
- [ ] 图片/文件消息支持
- [ ] 管理员群发通知接口
- [ ] WebSocket 连接数监控

---

## 改动文件总览

### 新建文件（10 个）

| 文件路径 | 说明 |
|---------|------|
| `enums/MessageType.java` | 消息类型枚举 |
| `enums/MessageProject.java` | 消息主题枚举 |
| `pojo/entity/Message.java` | 消息实体类 |
| `pojo/VO/MessageVO.java` | 消息详情 VO |
| `pojo/VO/ConversationVO.java` | 会话列表 VO |
| `mapper/MessageMapper.java` | Mapper 接口 |
| `resources/mapper/MessageMapper.xml` | Mapper XML |
| `config/WebSocketConfig.java` | WebSocket 配置 |
| `handler/WebSocketAuthInterceptor.java` | WebSocket 认证拦截器 |
| `handler/NotificationWebSocketHandler.java` | WebSocket 消息处理器 |
| `manager/WebSocketSessionManager.java` | WebSocket 会话管理器 |
| `service/MessageService.java` | 消息服务接口 |
| `service/Impl/MessageServiceImpl.java` | 消息服务实现 |
| `controller/MessageController.java` | 消息控制器 |

### 修改文件（5 个）

| 文件路径 | 改动内容 |
|---------|----------|
| `service/Impl/WorkServiceImpl.java` | 作品审核完成后发送通知 |
| `service/Impl/SeriesServiceImpl.java` | 合集审核完成后发送通知 |
| `service/Impl/CommentServiceImpl.java` | 评论审核完成后发送通知 |
| `service/Impl/LikeServiceImpl.java` | 点赞成功后发送通知 |
| `service/Impl/StarServiceImpl.java` | 收藏成功后发送通知 |

---

## 设计决策记录

1. **系统消息 from_user_id = 0** — 统一作为用户处理，简化查询逻辑
2. **复用 tb_messages 表** — V4.0 已升级表结构，支持私信和系统通知
3. **会话列表使用 GROUP BY** — 通过 `from_user_id` 和 `to` 动态计算对方用户
4. **软删除仅对自己可见** — 删除消息只影响自己的视图
5. **WebSocket 单机部署** — 内存管理连接，预留 Redis Pub/Sub 扩展点
6. **消息先入库再推送** — 保证消息不丢失，离线用户上线后可查询
7. **使用 @TableLogic 注解** — 自动处理软删除，无需在 SQL 中手动过滤 `is_delete = 0`
8. **系统用户特殊处理** — `from_user_id=0` 在 tb_user 表中不存在，LEFT JOIN 时需要特殊处理
9. **点赞/收藏自排除** — 用户操作自己的作品时不发送通知，避免干扰
10. **互动通知使用 system 类型** — LIKE/STAR/COMMENT 的 `message_type` 为 `system`，`from_user_id` 为实际用户

---

## 审查问题修复记录（2026-06-11）

### 高优先级问题（已修复）

| 问题 | 修复方案 | 影响范围 |
|------|----------|----------|
| 会话列表 SQL GROUP BY 错误 | 使用完整表达式 `GROUP BY CASE WHEN...` | MessageMapper.xml |
| 聊天记录查询系统用户 LEFT JOIN 问题 | 添加 `AND m.from_user_id > 0` 条件，使用 CASE WHEN 处理系统用户 | MessageMapper.xml |
| 点赞/收藏通知缺少自排除 | 添加 `if (!fromUserId.equals(workAuthorId))` 判断 | LikeServiceImpl, StarServiceImpl |
| 互动通知 message_type 定义不清 | 明确 LIKE/STAR/COMMENT 使用 `system` 类型，`from_user_id` 为实际用户 | 设计决策记录 |

### 中优先级问题（已修复）

| 问题 | 修复方案 | 影响范围 |
|------|----------|----------|
| 未读计数返回结构未定义 | 明确 Map 结构：`total`、`private`、`system` | MessageService 接口 |
| 软删除处理方式未明确 | 使用 `@TableLogic` 注解自动处理 | Message 实体类 |
| 缺少批量删除接口 | 添加 `batchDeleteMessages()` 和 `batchSoftDelete()` 方法 | MessageService, MessageMapper, MessageController |

### 低优先级问题（后续优化）

| 问题 | 说明 | 建议 |
|------|------|------|
| 会话列表性能 | 大数据量下 GROUP BY 性能差 | 后续可考虑 Redis 缓存或添加 conversation_id 字段 |
| 消息长度限制 | varchar(512) 约 170 个中文字符 | 如需更长内容，可扩展字段 |
| 并发未读计数 | 多用户同时操作可能不一致 | 后续可使用 Redis 原子操作 |

---

## 消息推送 JSON 格式

```json
{
  "type": "notification",
  "data": {
    "messageId": 123,
    "message": "你的作品「风景画」已通过审核",
    "project": "work_audit",
    "fromUserId": 0,
    "fromUsername": "system",
    "fromNickname": "系统",
    "fromAvatarUrl": null,
    "messageType": "system",
    "createTime": "2025-01-15T10:30:00",
    "unreadCount": 5
  }
}
```

---

**总计：10 新建 + 5 修改 = 15 个文件**
