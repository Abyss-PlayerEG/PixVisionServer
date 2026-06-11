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
     * 分页查询会话列表（按最后消息时间排序）
     * <p>
     * 查询用户的所有私信会话，按最后消息时间倒序排列。
     * </p>
     *
     * @param page   分页参数
     * @param userId 用户ID
     * @return 会话列表
     */
    IPage<ConversationVO> selectConversationList(
        IPage<?> page,
        @Param("userId") Integer userId
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
}
