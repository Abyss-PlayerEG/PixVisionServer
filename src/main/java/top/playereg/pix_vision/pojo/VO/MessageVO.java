package top.playereg.pix_vision.pojo.VO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import top.playereg.pix_vision.pojo.entity.Message;

/**
 * 消息详情视图对象
 * <p>
 * 继承 Message，扩展发送者用户信息，用于消息查询接口返回。
 * </p>
 *
 * <h3>使用场景</h3>
 * <ol>
 *   <li>聊天记录查询</li>
 *   <li>系统通知列表查询</li>
 *   <li>消息详情展示</li>
 * </ol>
 *
 * @author PlayerEG
 * @see Message
 * @since V4.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@Schema(description = "消息详情视图对象（含发送者信息）")
public class MessageVO extends Message {

    /**
     * 发送者用户名，系统消息时为 "system"
     */
    @Schema(description = "发送者用户名", example = "li_hua")
    private String from_username;

    /**
     * 发送者昵称，系统消息时为 "系统通知"
     */
    @Schema(description = "发送者昵称", example = "李华")
    private String from_nickname;

    /**
     * 发送者头像，系统消息时为 null
     */
    @Schema(description = "发送者头像", example = "/avatar/uuid.png")
    private String from_avatar_url;
}
