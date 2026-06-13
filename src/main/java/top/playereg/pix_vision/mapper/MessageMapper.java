package top.playereg.pix_vision.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;
import top.playereg.pix_vision.pojo.VO.ConversationVO;
import top.playereg.pix_vision.pojo.VO.MessageVO;
import top.playereg.pix_vision.pojo.entity.Message;

import java.util.List;

/**
 * 消息 Mapper 接口
 * <p>
 * 提供消息相关的数据库操作，包括未读消息统计、会话列表查询、聊天记录查询等。
 * </p>
 *
 * @author PlayerEG
 * @see Message
 * @since V4.0
 */
@Mapper
@Repository
public interface MessageMapper extends BaseMapper<Message> {

    /**
     * 查询用户未读消息总数
     *
     * @param userId 用户ID
     * @return 未读消息数量
     */
    int selectUnreadCount(@Param("userId") Integer userId);

    /**
     * 查询用户未读私信数量
     *
     * @param userId 用户ID
     * @return 未读私信数量
     */
    int selectUnreadPrivateCount(@Param("userId") Integer userId);

    /**
     * 查询用户未读系统通知数量
     *
     * @param userId 用户ID
     * @return 未读系统通知数量
     */
    int selectUnreadSystemCount(@Param("userId") Integer userId);

    /**
     * 查询与指定用户的会话未读数
     *
     * @param userId      当前用户ID
     * @param otherUserId 对方用户ID
     * @return 会话未读数
     */
    int selectConversationUnreadCount(
        @Param("userId") Integer userId,
        @Param("otherUserId") Integer otherUserId
    );

    /**
     * 分页查询会话列表（按最后消息时间排序）
     * <p>
     * 查询用户的所有私信会话，按最后消息时间倒序排列。
     * 支持按未读状态筛选。
     * </p>
     *
     * @param page   分页参数
     * @param userId 用户ID
     * @param isRead 已读状态筛选（可选，false-只返回有未读消息的会话）
     * @return 会话列表
     */
    IPage<ConversationVO> selectConversationList(
        IPage<?> page,
        @Param("userId") Integer userId,
        @Param("isRead") Boolean isRead
    );

    /**
     * 分页查询与某用户的聊天记录
     * <p>
     * 查询两个用户之间的私信记录，按时间倒序排列。
     * </p>
     *
     * @param page        分页参数
     * @param userId      当前用户ID
     * @param otherUserId 对方用户ID
     * @return 聊天记录列表
     */
    IPage<MessageVO> selectChatHistory(
        IPage<?> page,
        @Param("userId") Integer userId,
        @Param("otherUserId") Integer otherUserId
    );

    /**
     * 分页查询系统通知列表
     * <p>
     * 查询用户的系统通知，支持按消息主题和已读状态筛选。
     * </p>
     *
     * @param page    分页参数
     * @param userId  用户ID
     * @param project 消息主题（可选）
     * @param isRead  已读状态（可选）
     * @return 系统通知列表
     */
    IPage<MessageVO> selectSystemMessages(
        IPage<?> page,
        @Param("userId") Integer userId,
        @Param("project") String project,
        @Param("isRead") Boolean isRead
    );

    /**
     * 标记与某用户的所有私信为已读
     *
     * @param userId      当前用户ID
     * @param otherUserId 对方用户ID
     * @return 影响行数
     */
    int markConversationAsRead(
        @Param("userId") Integer userId,
        @Param("otherUserId") Integer otherUserId
    );

    /**
     * 批量标记消息已读
     *
     * @param userId     用户ID
     * @param messageIds 消息ID列表
     * @return 影响行数
     */
    int batchMarkAsRead(
        @Param("userId") Integer userId,
        @Param("messageIds") List<Integer> messageIds
    );

    /**
     * 全部标记已读
     *
     * @param userId      用户ID
     * @param messageType 消息类型（可选，为空则标记所有类型）
     * @return 影响行数
     */
    int markAllAsRead(
        @Param("userId") Integer userId,
        @Param("messageType") String messageType
    );

    /**
     * 查询用户未读消息的所有发送者ID
     * <p>
     * 用于全部标记已读时，通知相关发送者。
     * </p>
     *
     * @param userId      用户ID
     * @param messageType 消息类型（可选）
     * @return 发送者ID列表
     */
    List<Integer> selectUnreadMessageSenders(
        @Param("userId") Integer userId,
        @Param("messageType") String messageType
    );

    /**
     * 批量软删除消息
     *
     * @param userId     用户ID
     * @param messageIds 消息ID列表
     * @return 影响行数
     */
    int batchSoftDelete(
        @Param("userId") Integer userId,
        @Param("messageIds") List<Integer> messageIds
    );

    /**
     * 撤销消息（同时标记两个删除标签）
     *
     * @param messageId 消息ID
     * @param userId    用户ID（用于权限验证）
     * @return 影响行数
     */
    int recallMessage(
        @Param("messageId") Integer messageId,
        @Param("userId") Integer userId
    );

    /**
     * 删除消息（按用户标记）
     * <p>
     * 根据当前用户是发送者还是接收者，设置对应的删除标记。
     * 只有当双方都标记删除后，消息才真正不可见。
     * </p>
     *
     * @param messageId   消息ID
     * @param userId      用户ID
     * @param isSender    是否是发送者
     * @param isReceiver  是否是接收者
     * @return 影响行数
     */
    int deleteMessageByUser(
        @Param("messageId") Integer messageId,
        @Param("userId") Integer userId,
        @Param("isSender") boolean isSender,
        @Param("isReceiver") boolean isReceiver
    );

    /**
     * 批量删除消息（按用户标记）
     * <p>
     * 根据当前用户是发送者还是接收者，设置对应的删除标记。
     * </p>
     *
     * @param userId      用户ID
     * @param messageIds  消息ID列表
     * @param isSender    是否是发送者
     * @param isReceiver  是否是接收者
     * @return 影响行数
     */
    int batchDeleteMessageByUser(
        @Param("userId") Integer userId,
        @Param("messageIds") List<Integer> messageIds,
        @Param("isSender") boolean isSender,
        @Param("isReceiver") boolean isReceiver
    );

    /**
     * 分批查询所有私信消息（用于密钥更换）
     * <p>
     * 使用游标分页，基于message_id进行分批查询，避免OFFSET性能问题。
     * </p>
     *
     * @param lastMessageId 上一批最后一条消息的ID（第一批传0）
     * @param batchSize     每批大小
     * @return 私信消息列表
     */
    List<Message> selectAllPrivateMessages(
        @Param("lastMessageId") Integer lastMessageId,
        @Param("batchSize") Integer batchSize
    );

    /**
     * 批量更新消息内容（用于密钥更换）
     * <p>
     * 根据消息ID批量更新消息内容字段。
     * </p>
     *
     * @param messageIds 消息ID列表
     * @param contents   对应的新内容列表（与messageIds一一对应）
     * @return 影响行数
     */
    int batchUpdateMessageContent(
        @Param("messageIds") List<Integer> messageIds,
        @Param("contents") List<String> contents
    );

    /**
     * 管理员分页查询私信记录（支持多条件筛选）
     * <p>
     * 查询所有私信消息，支持按用户ID、时间范围筛选。
     * 不过滤删除状态，管理员可查看所有私信。
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
     * IPage<MessageVO> result = messageMapper.adminSelectMessages(
     *     page, 1001, null, null, null, null
     * );
     *
     * // 查询所有私信，按时间正序
     * IPage<MessageVO> result = messageMapper.adminSelectMessages(
     *     page, null, null, null, null, "oldest"
     * );
     * }</pre>
     *
     * <h3>注意事项</h3>
     * <ul>
     *   <li>所有筛选条件均为可选，可以自由组合</li>
     *   <li>查询结果包含完整的消息信息字段</li>
     *   <li>不过滤任何删除状态，管理员可查看所有私信</li>
     *   <li>私信内容需要在 Service 层解密</li>
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
    IPage<MessageVO> adminSelectMessages(
        IPage<?> page,
        @Param("username") String username,
        @Param("participants") String participants,
        @Param("keyword") String keyword,
        @Param("startTime") String startTime,
        @Param("endTime") String endTime,
        @Param("orderBy") String orderBy
    );
}
