package top.playereg.pix_vision.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import top.playereg.pix_vision.enums.MessageProject;
import top.playereg.pix_vision.enums.MessageType;
import top.playereg.pix_vision.pojo.VO.ConversationVO;
import top.playereg.pix_vision.pojo.VO.MessageVO;
import top.playereg.pix_vision.pojo.entity.Message;

import java.util.List;
import java.util.Map;

/**
 * 消息服务接口
 * <p>
 * 提供站内消息的核心业务功能，包括消息发送、查询、标记已读等。
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
public interface MessageService {

    /**
     * 发送消息（通用方法）
     * <p>
     * 写入数据库并通过 WebSocket 推送给在线用户。
     * </p>
     *
     * @param fromUserId 发送者用户ID（系统消息传 0）
     * @param toUserId   接收者用户ID
     * @param content    消息内容
     * @param type       消息类型
     * @param project    消息主题
     */
    void sendMessage(Integer fromUserId, Integer toUserId, String content,
                     MessageType type, MessageProject project);

    /**
     * 发送私信
     *
     * @param fromUserId 发送者用户ID
     * @param toUserId   接收者用户ID
     * @param content    消息内容
     */
    void sendPrivateMessage(Integer fromUserId, Integer toUserId, String content);

    /**
     * 发送系统通知（from_user_id = 0）
     *
     * @param toUserId 接收者用户ID
     * @param content  消息内容
     * @param project  消息主题
     */
    void sendSystemNotice(Integer toUserId, String content, MessageProject project);

    /**
     * 发送系统通知（from_user_id = 0，带关联ID）
     *
     * @param fromUserId 发送者用户ID（点赞/收藏时为实际用户ID，审核时为0）
     * @param toUserId   接收者用户ID
     * @param content    消息内容
     * @param project    消息主题
     * @param refId      关联实体ID（如作品ID、评论ID等）
     */
    void sendSystemNotice(Integer fromUserId, Integer toUserId, String content,
                          MessageProject project, Integer refId);

    /**
     * 获取未读消息数量
     * <p>
     * 返回 Map 结构：
     * - total: 总未读数
     * - private: 未读私信数
     * - system: 未读系统通知数
     * </p>
     *
     * @param userId 用户ID
     * @return 未读消息数量统计
     */
    Map<String, Integer> getUnreadCount(Integer userId);

    /**
     * 获取与指定用户的会话未读数
     * <p>
     * 返回 Map 结构：
     * - conversation_unread: 与该用户的会话未读数
     * </p>
     *
     * @param userId      当前用户ID
     * @param otherUserId 对方用户ID
     * @return 会话未读数统计
     */
    Map<String, Integer> getConversationUnreadCount(Integer userId, Integer otherUserId);

    /**
     * 分页查询会话列表
     *
     * @param page   分页参数
     * @param userId 用户ID
     * @param isRead 已读状态筛选（可选，false-只返回有未读消息的会话）
     * @return 会话列表
     */
    IPage<ConversationVO> getConversationList(Page<Message> page, Integer userId, Boolean isRead);

    /**
     * 分页查询聊天记录
     *
     * @param page        分页参数
     * @param userId      当前用户ID
     * @param otherUserId 对方用户ID
     * @return 聊天记录列表
     */
    IPage<MessageVO> getChatHistory(Page<Message> page, Integer userId, Integer otherUserId);

    /**
     * 分页查询系统通知
     *
     * @param page    分页参数
     * @param userId  用户ID
     * @param project 消息主题（可选）
     * @param isRead  已读状态（可选）
     * @return 系统通知列表
     */
    IPage<MessageVO> getSystemMessages(Page<Message> page, Integer userId,
                                       String project, Boolean isRead);

    /**
     * 标记会话已读
     *
     * @param userId      当前用户ID
     * @param otherUserId 对方用户ID
     * @return 是否成功
     */
    boolean markConversationAsRead(Integer userId, Integer otherUserId);

    /**
     * 批量标记消息已读
     *
     * @param userId     用户ID
     * @param messageIds 消息ID列表
     * @return 是否成功
     */
    boolean batchMarkAsRead(Integer userId, List<Integer> messageIds);

    /**
     * 全部标记已读
     *
     * @param userId      用户ID
     * @param messageType 消息类型（可选，为空则标记所有类型）
     * @return 是否成功
     */
    boolean markAllAsRead(Integer userId, String messageType);

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
    boolean recallMessage(Integer userId, Integer messageId);

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
    boolean batchDeleteMessages(Integer userId, List<Integer> messageIds);

    /**
     * 更换消息加密密钥
     * <p>
     * 更换RSA密钥对，并批量更新所有私信的加密内容。
     * 此操作需要管理员权限，且执行时间较长。
     * </p>
     *
     * @return 操作结果，包含成功数量、失败数量等统计信息
     */
    Map<String, Object> rotateEncryptionKeys();

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
     * @see top.playereg.pix_vision.service.Impl.MessageServiceImpl#getAdminMessages
     */
    IPage<MessageVO> getAdminMessages(Page<Message> page, String username, String participants,
                                       String keyword, String startTime, String endTime, String orderBy);
}