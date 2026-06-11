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
     * 分页查询会话列表
     *
     * @param page   分页参数
     * @param userId 用户ID
     * @return 会话列表
     */
    IPage<ConversationVO> getConversationList(Page<Message> page, Integer userId);

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
     * 删除消息（软删除）
     *
     * @param userId    用户ID
     * @param messageId 消息ID
     * @return 是否成功
     */
    boolean deleteMessage(Integer userId, Integer messageId);

    /**
     * 批量删除消息（软删除）
     *
     * @param userId     用户ID
     * @param messageIds 消息ID列表
     * @return 是否成功
     */
    boolean batchDeleteMessages(Integer userId, List<Integer> messageIds);
}
