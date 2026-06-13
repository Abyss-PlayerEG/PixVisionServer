package top.playereg.pix_vision.service.Impl;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.playereg.pix_vision.enums.MessageProject;
import top.playereg.pix_vision.enums.MessageType;
import top.playereg.pix_vision.manager.WebSocketSessionManager;
import top.playereg.pix_vision.mapper.MessageMapper;
import top.playereg.pix_vision.mapper.UserMapper;
import top.playereg.pix_vision.pojo.VO.ConversationVO;
import top.playereg.pix_vision.pojo.VO.MessageVO;
import top.playereg.pix_vision.pojo.entity.Message;
import top.playereg.pix_vision.pojo.entity.user.User;
import top.playereg.pix_vision.service.MessageService;
import top.playereg.pix_vision.util.PixVisionLogger;
import top.playereg.pix_vision.util.RSACipher;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 消息服务实现类
 * <p>
 * 实现站内消息的核心业务功能，包括消息发送、查询、标记已读等。
 * </p>
 *
 * <h3>功能说明</h3>
 * <ul>
 *   <li>发送消息（系统通知、互动通知、私信）</li>
 *   <li>查询未读消息数量</li>
 *   <li>查询会话列表</li>
 *   <li>查询聊天记录</li>
 *   <li>标记消息已读</li>
 *   <li>删除消息（软删除）</li>
 * </ul>
 *
 * <h3>使用场景</h3>
 * <ol>
 *   <li>站内消息系统</li>
 *   <li>私信功能</li>
 *   <li>系统通知推送</li>
 * </ol>
 *
 * @author PlayerEG
 * @since V4.0
 */
@Service
public class MessageServiceImpl implements MessageService {

    private static final PixVisionLogger log = PixVisionLogger.create(MessageServiceImpl.class);

    @Autowired
    private MessageMapper messageMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private WebSocketSessionManager sessionManager;

    @Autowired
    private RSACipher rsaCipher;

    /**
     * 发送消息（通用方法）
     *
     * @param fromUserId 发送者用户ID（系统消息传 0）
     * @param toUserId   接收者用户ID
     * @param content    消息内容
     * @param type       消息类型
     * @param project    消息主题
     */
    @Override
    public void sendMessage(Integer fromUserId, Integer toUserId, String content,
                            MessageType type, MessageProject project) {
        // 参数校验
        if (toUserId == null || toUserId <= 0) {
            log.error("接收者用户ID无效：{}", toUserId);
            return;
        }
        if (content == null || content.isEmpty()) {
            log.error("消息内容为空");
            return;
        }
        if (type == null || project == null) {
            log.error("消息类型或主题为空");
            return;
        }

        // 创建消息实体
        Message message = new Message();
        message.setFrom_user_id(fromUserId != null ? fromUserId : 0);
        message.setTo(toUserId);
        
        // 私信内容加密存储
        String storedContent = content;
        if (type == MessageType.PRIVATE) {
            try {
                storedContent = RSACipher.encryptToBase64(content);
                log.debug("私信内容已加密，消息长度：{} -> {}", content.length(), storedContent.length());
            } catch (Exception e) {
                log.error("私信内容加密失败，发送者：{}，接收者：{}，错误：{}", fromUserId, toUserId, e.getMessage());
                return;
            }
        }
        message.setMessage(storedContent);
        message.setMessage_type(type.getCode());
        message.setProject(project.getCode());
        message.setIs_read(false);
        message.setIs_delete_by_sender(false);
        message.setIs_delete_by_receiver(false);
        message.setCreate_time(LocalDateTime.now());

        // 写入数据库
        try {
            messageMapper.insert(message);
            log.info("消息发送成功，消息ID：{}，发送者：{}，接收者：{}，类型：{}，主题：{}",
                    message.getMessage_id(), fromUserId, toUserId, type.getCode(), project.getCode());
        } catch (Exception e) {
            log.error("消息发送失败，发送者：{}，接收者：{}，错误：{}", fromUserId, toUserId, e.getMessage());
            return;
        }

        // 构建推送消息
        Map<String, Object> pushData = new HashMap<>();
        pushData.put("type", "notification");
        pushData.put("data", buildMessageVO(message));

        // WebSocket 推送
        if (sessionManager.isOnline(toUserId)) {
            sessionManager.sendMessage(toUserId, JSON.toJSONString(pushData));
            log.debug("消息已推送给在线用户 {}", toUserId);
        } else {
            log.debug("用户 {} 不在线，消息已持久化，等待用户上线查询", toUserId);
        }
    }

    /**
     * 发送私信
     *
     * @param fromUserId 发送者用户ID
     * @param toUserId   接收者用户ID
     * @param content    消息内容
     */
    @Override
    public void sendPrivateMessage(Integer fromUserId, Integer toUserId, String content) {
        sendMessage(fromUserId, toUserId, content, MessageType.PRIVATE, MessageProject.PRIVATE_MESSAGE);
    }

    /**
     * 发送系统通知（from_user_id = 0）
     *
     * @param toUserId 接收者用户ID
     * @param content  消息内容
     * @param project  消息主题
     */
    @Override
    public void sendSystemNotice(Integer toUserId, String content, MessageProject project) {
        sendMessage(0, toUserId, content, MessageType.SYSTEM, project);
    }

    /**
     * 发送系统通知（from_user_id = 0，带关联ID）
     *
     * @param fromUserId 发送者用户ID（点赞/收藏时为实际用户ID，审核时为0）
     * @param toUserId   接收者用户ID
     * @param content    消息内容
     * @param project    消息主题
     * @param refId      关联实体ID（如作品ID、评论ID等）
     */
    @Override
    public void sendSystemNotice(Integer fromUserId, Integer toUserId, String content,
                                 MessageProject project, Integer refId) {
        // 参数校验
        if (toUserId == null || toUserId <= 0) {
            log.error("接收者用户ID无效：{}", toUserId);
            return;
        }
        if (content == null || content.isEmpty()) {
            log.error("消息内容为空");
            return;
        }
        if (project == null) {
            log.error("消息主题为空");
            return;
        }

        // 创建消息实体
        Message message = new Message();
        message.setFrom_user_id(fromUserId != null ? fromUserId : 0);
        message.setTo(toUserId);
        message.setMessage(content);
        message.setMessage_type(MessageType.SYSTEM.getCode());
        message.setProject(project.getCode());
        message.setRef_id(refId);
        message.setIs_read(false);
        message.setIs_delete_by_sender(false);
        message.setIs_delete_by_receiver(false);
        message.setCreate_time(LocalDateTime.now());

        // 写入数据库
        try {
            messageMapper.insert(message);
            log.info("系统通知发送成功，消息ID：{}，发送者：{}，接收者：{}，主题：{}，关联ID：{}",
                    message.getMessage_id(), fromUserId, toUserId, project.getCode(), refId);
        } catch (Exception e) {
            log.error("系统通知发送失败，发送者：{}，接收者：{}，错误：{}", fromUserId, toUserId, e.getMessage());
            return;
        }

        // 构建推送消息
        Map<String, Object> pushData = new HashMap<>();
        pushData.put("type", "notification");
        pushData.put("data", buildMessageVO(message));

        // WebSocket 推送
        if (sessionManager.isOnline(toUserId)) {
            sessionManager.sendMessage(toUserId, JSON.toJSONString(pushData));
            log.debug("系统通知已推送给在线用户 {}", toUserId);
        } else {
            log.debug("用户 {} 不在线，系统通知已持久化，等待用户上线查询", toUserId);
        }
    }

    /**
     * 获取未读消息数量
     *
     * @param userId 用户ID
     * @return 未读消息数量统计
     */
    @Override
    public Map<String, Integer> getUnreadCount(Integer userId) {
        Map<String, Integer> result = new HashMap<>();
        result.put("total", messageMapper.selectUnreadCount(userId));
        result.put("private", messageMapper.selectUnreadPrivateCount(userId));
        result.put("system", messageMapper.selectUnreadSystemCount(userId));
        return result;
    }

    /**
     * 获取与指定用户的会话未读数
     *
     * @param userId      当前用户ID
     * @param otherUserId 对方用户ID
     * @return 会话未读数统计
     */
    @Override
    public Map<String, Integer> getConversationUnreadCount(Integer userId, Integer otherUserId) {
        Map<String, Integer> result = new HashMap<>();
        result.put("conversation_unread", messageMapper.selectConversationUnreadCount(userId, otherUserId));
        return result;
    }

    /**
     * 分页查询会话列表
     *
     * @param page   分页参数
     * @param userId 用户ID
     * @param isRead 已读状态筛选（可选，false-只返回有未读消息的会话）
     * @return 会话列表
     */
    @Override
    public IPage<ConversationVO> getConversationList(Page<Message> page, Integer userId, Boolean isRead) {
        IPage<ConversationVO> result = messageMapper.selectConversationList(page, userId, isRead);

        // 填充对方用户信息并解密最后消息
        if (result != null && !result.getRecords().isEmpty()) {
            for (ConversationVO vo : result.getRecords()) {
                User user = userMapper.selectAllUserInfoById(vo.getOther_user_id());
                if (user != null) {
                    vo.setOther_username(user.getUsername());
                    vo.setOther_nickname(user.getNickname());
                    vo.setOther_avatar_url(user.getAvatar_url());
                }
                
                // 解密最后一条消息内容
                if (vo.getLast_message() != null && !vo.getLast_message().isEmpty()) {
                    try {
                        String decrypted = RSACipher.decryptToString(vo.getLast_message());
                        if (decrypted != null) {
                            vo.setLast_message(decrypted);
                        } else {
                            log.warn("会话列表最后消息解密返回null，对方用户ID：{}", vo.getOther_user_id());
                        }
                    } catch (Exception e) {
                        log.warn("会话列表最后消息解密失败，对方用户ID：{}，错误：{}", vo.getOther_user_id(), e.getMessage());
                    }
                }
            }
        }

        return result;
    }

    /**
     * 分页查询聊天记录
     *
     * @param page        分页参数
     * @param userId      当前用户ID
     * @param otherUserId 对方用户ID
     * @return 聊天记录列表
     */
    @Override
    public IPage<MessageVO> getChatHistory(Page<Message> page, Integer userId, Integer otherUserId) {
        IPage<MessageVO> result = messageMapper.selectChatHistory(page, userId, otherUserId);
        
        // 解密私信内容
        if (result != null && !result.getRecords().isEmpty()) {
            for (MessageVO vo : result.getRecords()) {
                if (vo.getMessage_type() != null && vo.getMessage_type().equals(MessageType.PRIVATE.getCode())) {
                    try {
                        String decrypted = RSACipher.decryptToString(vo.getMessage());
                        if (decrypted != null) {
                            vo.setMessage(decrypted);
                        } else {
                            log.warn("聊天记录解密返回null，消息ID：{}", vo.getMessage_id());
                        }
                    } catch (Exception e) {
                        log.warn("聊天记录解密失败，消息ID：{}，错误：{}", vo.getMessage_id(), e.getMessage());
                    }
                }
            }
        }
        
        return result;
    }

    /**
     * 分页查询系统通知
     *
     * @param page    分页参数
     * @param userId  用户ID
     * @param project 消息主题（可选）
     * @param isRead  已读状态（可选）
     * @return 系统通知列表
     */
    @Override
    public IPage<MessageVO> getSystemMessages(Page<Message> page, Integer userId,
                                              String project, Boolean isRead) {
        return messageMapper.selectSystemMessages(page, userId, project, isRead);
    }

    /**
     * 标记会话已读
     *
     * @param userId      当前用户ID
     * @param otherUserId 对方用户ID
     * @return 是否成功
     */
    @Override
    public boolean markConversationAsRead(Integer userId, Integer otherUserId) {
        try {
            int count = messageMapper.markConversationAsRead(userId, otherUserId);
            log.debug("标记会话已读，用户：{}，对方：{}，影响行数：{}", userId, otherUserId, count);

            // 有实际标记操作时，通知对方消息已被读取
            if (count > 0) {
                sendReadReceiptNotification(userId, otherUserId);
            }

            return true;
        } catch (Exception e) {
            log.error("标记会话已读失败，用户：{}，对方：{}，错误：{}", userId, otherUserId, e.getMessage());
            return false;
        }
    }

    /**
     * 批量标记消息已读
     *
     * @param userId     用户ID
     * @param messageIds 消息ID列表
     * @return 是否成功
     */
    @Override
    public boolean batchMarkAsRead(Integer userId, List<Integer> messageIds) {
        if (messageIds == null || messageIds.isEmpty()) {
            return true;
        }
        try {
            int count = messageMapper.batchMarkAsRead(userId, messageIds);
            log.debug("批量标记消息已读，用户：{}，消息数量：{}，影响行数：{}", userId, messageIds.size(), count);
            return true;
        } catch (Exception e) {
            log.error("批量标记消息已读失败，用户：{}，错误：{}", userId, e.getMessage());
            return false;
        }
    }

    /**
     * 全部标记已读
     *
     * @param userId      用户ID
     * @param messageType 消息类型（可选，为空则标记所有类型）
     * @return 是否成功
     */
    @Override
    public boolean markAllAsRead(Integer userId, String messageType) {
        try {
            // 先查询所有有未读消息的发送者
            List<Integer> senderIds = messageMapper.selectUnreadMessageSenders(userId, messageType);

            int count = messageMapper.markAllAsRead(userId, messageType);
            log.debug("全部标记已读，用户：{}，类型：{}，影响行数：{}", userId, messageType, count);

            // 有实际标记操作时，通知所有相关发送者
            if (count > 0 && senderIds != null && !senderIds.isEmpty()) {
                for (Integer senderId : senderIds) {
                    sendReadReceiptNotification(userId, senderId);
                }
            }

            return true;
        } catch (Exception e) {
            log.error("全部标记已读失败，用户：{}，错误：{}", userId, e.getMessage());
            return false;
        }
    }

    /**
     * 撤销消息
     * <p>
     * 只能撤销自己发送 2 分钟内的消息，撤销后双方都不可见。
     * </p>
     *
     * @param userId    用户ID
     * @param messageId 消息ID
     * @return 是否成功
     */
    @Override
    public boolean recallMessage(Integer userId, Integer messageId) {
        try {
            Message message = messageMapper.selectById(messageId);
            if (message == null) {
                log.warn("消息不存在，消息ID：{}", messageId);
                return false;
            }

            // 只能撤销自己发送的消息
            if (!message.getFrom_user_id().equals(userId)) {
                log.warn("无权撤销消息，不是消息发送者，用户：{}，消息ID：{}", userId, messageId);
                return false;
            }

            // 检查是否在 2 分钟内
            if (message.getCreate_time() == null) {
                log.warn("消息创建时间为空，消息ID：{}", messageId);
                return false;
            }

            long messageTime = message.getCreate_time().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
            long now = System.currentTimeMillis();
            long twoMinutesMillis = 2 * 60 * 1000L;

            if (now - messageTime > twoMinutesMillis) {
                log.warn("消息已超过 2 分钟，无法撤销，用户：{}，消息ID：{}，消息时间：{}", userId, messageId, message.getCreate_time());
                return false;
            }

            // 同时标记两个删除标签，双方都不可见
            int count = messageMapper.recallMessage(messageId, userId);
            if (count > 0) {
                log.debug("撤销消息成功，用户：{}，消息ID：{}", userId, messageId);

                // WebSocket 实时通知对方消息已被撤销
                Integer toUserId = message.getTo();
                if (sessionManager.isOnline(toUserId)) {
                    Map<String, Object> pushData = new HashMap<>();
                    pushData.put("type", "message_recall");

                    Map<String, Object> recallData = new HashMap<>();
                    recallData.put("message_id", messageId);
                    recallData.put("from_user_id", userId);
                    pushData.put("data", recallData);

                    sessionManager.sendMessage(toUserId, JSON.toJSONString(pushData));
                    log.debug("撤销通知已推送给用户 {}，消息ID：{}", toUserId, messageId);
                }
                return true;
            }
            return false;
        } catch (Exception e) {
            log.error("撤销消息失败，用户：{}，消息ID：{}，错误：{}", userId, messageId, e.getMessage());
            return false;
        }
    }

    /**
     * 批量删除消息（软删除）
     * <p>
     * 不区分消息是自己发送还是接收，都可以删除。
     * </p>
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
            // 根据用户身份设置对应的删除标记
            int successCount = 0;
            for (Integer messageId : messageIds) {
                try {
                    Message message = messageMapper.selectById(messageId);
                    if (message == null) {
                        continue;
                    }

                    boolean isSender = message.getFrom_user_id().equals(userId);
                    boolean isReceiver = message.getTo().equals(userId);

                    // 不区分发送者/接收者，都可以删除
                    if (isSender || isReceiver) {
                        int count = messageMapper.deleteMessageByUser(messageId, userId, isSender, isReceiver);
                        if (count > 0) {
                            successCount++;
                        }
                    }
                } catch (Exception e) {
                    log.warn("删除单条消息失败，消息ID：{}，错误：{}", messageId, e.getMessage());
                }
            }
            log.debug("批量删除消息成功，用户：{}，消息数量：{}，成功数量：{}", userId, messageIds.size(), successCount);
            return successCount > 0;
        } catch (Exception e) {
            log.error("批量删除消息失败，用户：{}，错误：{}", userId, e.getMessage());
            return false;
        }
    }

    /**
     * 构建 MessageVO 对象
     *
     * @param message 消息实体
     * @return MessageVO 对象
     */
    private MessageVO buildMessageVO(Message message) {
        MessageVO vo = new MessageVO();
        vo.setMessage_id(message.getMessage_id());
        
        // 私信内容解密
        String displayContent = message.getMessage();
        if (message.getMessage_type() != null && message.getMessage_type().equals(MessageType.PRIVATE.getCode())) {
            try {
                String decrypted = RSACipher.decryptToString(message.getMessage());
                if (decrypted != null) {
                    displayContent = decrypted;
                } else {
                    log.warn("私信内容解密返回null，消息ID：{}", message.getMessage_id());
                }
            } catch (Exception e) {
                log.warn("私信内容解密失败，消息ID：{}，错误：{}", message.getMessage_id(), e.getMessage());
                // 解密失败时保留加密内容，避免显示异常
            }
        }
        vo.setMessage(displayContent);
        vo.setProject(message.getProject());
        vo.setFrom_user_id(message.getFrom_user_id());
        vo.setMessage_type(message.getMessage_type());
        vo.setRef_id(message.getRef_id());
        vo.setTo(message.getTo());
        vo.setIs_read(message.getIs_read());
        vo.setIs_delete_by_sender(message.getIs_delete_by_sender());
        vo.setIs_delete_by_receiver(message.getIs_delete_by_receiver());
        vo.setCreate_time(message.getCreate_time());

        // 系统消息特殊处理
        if (message.getFrom_user_id() != null && message.getFrom_user_id() == 0) {
            vo.setFrom_username("system");
            vo.setFrom_nickname("系统通知");
            vo.setFrom_avatar_url(null);
        } else {
            // 查询发送者信息
            User user = userMapper.selectAllUserInfoById(message.getFrom_user_id());
            if (user != null) {
                vo.setFrom_username(user.getUsername());
                vo.setFrom_nickname(user.getNickname());
                vo.setFrom_avatar_url(user.getAvatar_url());
            }
        }

        return vo;
    }

    /**
     * 发送已读回执通知
     * <p>
     * 当用户标记会话已读时，通过 WebSocket 通知对方消息已被读取。
     * </p>
     *
     * @param readerId    已读者用户ID（当前用户）
     * @param senderId    发送者用户ID（对方）
     */
    private void sendReadReceiptNotification(Integer readerId, Integer senderId) {
        try {
            if (sessionManager.isOnline(senderId)) {
                Map<String, Object> pushData = new HashMap<>();
                pushData.put("type", "messages_read");

                Map<String, Object> readData = new HashMap<>();
                readData.put("reader_id", readerId);
                pushData.put("data", readData);

                sessionManager.sendMessage(senderId, JSON.toJSONString(pushData));
                log.debug("已读回执通知已推送给用户 {}，读者：{}", senderId, readerId);
            }
        } catch (Exception e) {
            log.warn("发送已读回执通知失败，读者：{}，发送者：{}，错误：{}", readerId, senderId, e.getMessage());
        }
    }

    /**
     * 更换消息加密密钥
     * <p>
     * 更换RSA密钥对，并批量更新所有私信的加密内容。
     * 此操作需要管理员权限，且执行时间较长。
     * </p>
     *
     * @return 操作结果，包含成功数量、失败数量等统计信息
     */
    @Override
    public Map<String, Object> rotateEncryptionKeys() {
        log.warn("开始更换消息加密密钥...");
        
        // 统计信息
        int totalProcessed = 0;
        int successCount = 0;
        int failCount = 0;
        int skipCount = 0;
        
        // 1. 保存旧私钥（用于解密旧数据）
        String oldPrivateKey = RSACipher.getPrivateKey();
        if (oldPrivateKey == null || oldPrivateKey.isEmpty()) {
            throw new RuntimeException("无法获取旧私钥，密钥更换失败");
        }
        
        // 2. 生成新密钥对
        try {
            log.warn("正在生成新密钥对...");
            // 调用实例方法生成新密钥对
            String[] newKeys = rsaCipher.regenerateKeys();
            log.warn("新密钥对已生成");
        } catch (Exception e) {
            log.error("生成新密钥对失败", e);
            throw new RuntimeException("生成新密钥对失败", e);
        }
        
        // 3. 分批查询所有私信
        int batchSize = 1000;
        Integer lastMessageId = 0;
        boolean hasMore = true;
        
        while (hasMore) {
            try {
                // 查询一批私信
                List<Message> messages = messageMapper.selectAllPrivateMessages(lastMessageId, batchSize);
                
                if (messages == null || messages.isEmpty()) {
                    hasMore = false;
                    break;
                }
                
                totalProcessed += messages.size();
                
                // 准备批量更新数据
                List<Integer> messageIds = new ArrayList<>();
                List<String> newContents = new ArrayList<>();
                
                for (Message message : messages) {
                    try {
                        // 用旧私钥解密
                        String decrypted = RSACipher.decryptToStringWithKey(message.getMessage(), oldPrivateKey);
                        if (decrypted == null) {
                            log.warn("解密失败，跳过消息ID：{}", message.getMessage_id());
                            skipCount++;
                            continue;
                        }
                        
                        // 用新公钥加密
                        String encrypted = RSACipher.encryptToBase64(decrypted);
                        
                        messageIds.add(message.getMessage_id());
                        newContents.add(encrypted);
                        successCount++;
                        
                    } catch (Exception e) {
                        log.warn("处理消息失败，消息ID：{}，错误：{}", message.getMessage_id(), e.getMessage());
                        failCount++;
                    }
                }
                
                // 批量更新数据库
                if (!messageIds.isEmpty()) {
                    try {
                        int updateCount = messageMapper.batchUpdateMessageContent(messageIds, newContents);
                        log.debug("批量更新成功，更新数量：{}", updateCount);
                    } catch (Exception e) {
                        log.error("批量更新失败", e);
                        failCount += messageIds.size();
                        successCount -= messageIds.size();
                    }
                }
                
                // 更新游标
                lastMessageId = messages.get(messages.size() - 1).getMessage_id();
                
                // 如果这批数量小于batchSize，说明已经处理完所有数据
                if (messages.size() < batchSize) {
                    hasMore = false;
                }
                
            } catch (Exception e) {
                log.error("查询私信失败", e);
                throw new RuntimeException("查询私信失败", e);
            }
        }
        
        // 4. 返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("total_processed", totalProcessed);
        result.put("success_count", successCount);
        result.put("fail_count", failCount);
        result.put("skip_count", skipCount);
        
        log.warn("密钥更换完成，总处理：{}，成功：{}，失败：{}，跳过：{}", 
                totalProcessed, successCount, failCount, skipCount);
        
        return result;
    }

    /**
     * 管理员分页查询私信记录（支持多条件筛选）
     * <p>
     * 查询所有私信消息，支持按用户ID、时间范围筛选。
     * 不过滤删除状态，管理员可查看所有私信。
     * 私信内容会自动解密显示。
     * </p>
     *
     * <h3>使用场景</h3>
     * <ol>
     *   <li>管理员监管用户私信内容</li>
     *   <li>审核私信消息</li>
     *   <li>按时间范围查询私信记录</li>
     * </ol>
     *
     * <h3>使用示例</h3>
     * <pre>{@code
     * // 查询用户ID为1001的所有私信
     * Page<Message> page = new Page<>(1, 10);
     * IPage<MessageVO> result = messageService.getAdminMessages(
     *     page, 1001, null, null, null, null
     * );
     *
     * // 查询所有私信，按时间正序
     * IPage<MessageVO> result = messageService.getAdminMessages(
     *     page, null, null, null, null, "oldest"
     * );
     * }</pre>
     *
     * <h3>注意事项</h3>
     * <ul>
     *   <li>所有筛选条件均为可选，可以自由组合</li>
     *   <li>查询结果包含完整的消息信息字段</li>
     *   <li>不过滤任何删除状态，管理员可查看所有私信</li>
     *   <li>私信内容会自动解密显示</li>
     * </ul>
     *
     * @param page        分页参数
     * @param username    用户名（可选，查询该用户作为发送者或接收者的消息）
     * @param participants 参与者用户名（可选，查看指定用户之间的对话，格式：'user1,user2'）
     * @param keyword     关键字（可选，模糊搜索消息内容）
     * @param startTime   开始时间（可选，格式：yyyy-MM-dd HH:mm:ss）
     * @param endTime     结束时间（可选，格式：yyyy-MM-dd HH:mm:ss）
     * @param orderBy     排序方式（可选，'oldest'-最早, 其他值-最新）
     * @return 私信记录列表
     * @author PlayerEG
     * @see MessageMapper#adminSelectMessages(IPage, String, String, String, String, String, String)
     */
    @Override
    public IPage<MessageVO> getAdminMessages(Page<Message> page, String username, String participants,
                                              String keyword, String startTime, String endTime, String orderBy) {
        log.info("管理员查询私信记录 - 用户名: {}, 参与者: {}, 关键字: {}, 时间范围: {} - {}, 排序: {}",
            username, participants, keyword, startTime, endTime, orderBy);

        // 查询私信消息
        IPage<MessageVO> result = messageMapper.adminSelectMessages(page, username, participants, keyword, startTime, endTime, orderBy);

        // 解密私信内容
        if (result != null && !result.getRecords().isEmpty()) {
            for (MessageVO vo : result.getRecords()) {
                if (vo.getMessage_type() != null && vo.getMessage_type().equals(MessageType.PRIVATE.getCode())) {
                    try {
                        String decrypted = RSACipher.decryptToString(vo.getMessage());
                        if (decrypted != null) {
                            vo.setMessage(decrypted);
                        } else {
                            log.warn("管理员查询聊天记录解密返回null，消息ID：{}", vo.getMessage_id());
                        }
                    } catch (Exception e) {
                        log.warn("管理员查询聊天记录解密失败，消息ID：{}，错误：{}", vo.getMessage_id(), e.getMessage());
                    }
                }
            }
        }

        log.info("管理员查询聊天记录完成 - 总数: {}, 当前页: {}", result.getTotal(), result.getCurrent());
        return result;
    }
}
