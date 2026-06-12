package top.playereg.pix_vision.pojo.VO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 会话列表视图对象
 * <p>
 * 用于私信会话列表展示，包含对方用户信息、最后一条消息和未读数量。
 * </p>
 *
 * <h3>使用场景</h3>
 * <ol>
 *   <li>私信会话列表查询</li>
 *   <li>未读消息数量统计</li>
 * </ol>
 *
 * @author PlayerEG
 * @since V4.0
 */
@Data
@NoArgsConstructor
@Schema(description = "会话列表视图对象")
public class ConversationVO {

    /**
     * 对方用户ID
     */
    @Schema(description = "对方用户ID", example = "1001")
    private Integer other_user_id;

    /**
     * 对方用户名
     */
    @Schema(description = "对方用户名", example = "li_hua")
    private String other_username;

    /**
     * 对方昵称
     */
    @Schema(description = "对方昵称", example = "李华")
    private String other_nickname;

    /**
     * 对方头像
     */
    @Schema(description = "对方头像", example = "/avatar/uuid.png")
    private String other_avatar_url;

    /**
     * 最后一条消息内容
     */
    @Schema(description = "最后一条消息内容", example = "你好！")
    private String last_message;

    /**
     * 最后消息时间
     */
    @Schema(description = "最后消息时间", example = "2026-06-11T10:30:00")
    private LocalDateTime last_message_time;

    /**
     * 未读消息数量
     */
    @Schema(description = "未读消息数量", example = "5")
    private Integer unread_count;
}
