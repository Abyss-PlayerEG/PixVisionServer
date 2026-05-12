package top.playereg.pix_vision.pojo.commentsPojo.VO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import top.playereg.pix_vision.pojo.commentsPojo.BaseComment;

/**
 * 评论基础 VO 类
 * <p>
 * 继承 BaseComment，添加用户信息字段（昵称、头像）
 *
 * @author PlayerEG
 */
@Data
@NoArgsConstructor
@Schema(description = "评论基础 VO")
public class BaseCommentVO extends BaseComment {

    /**
     * 用户昵称
     */
    @Schema(description = "用户昵称")
    private String nickname;

    /**
     * 用户头像路径
     */
    @Schema(description = "用户头像路径")
    private String user_avatar;

    /**
     * 被回复者的用户昵称（仅二级评论有值，一级评论为 null）
     */
    @Schema(description = "被回复者的用户昵称")
    private String replied_nickname;

    /**
     * 格式化后的时间显示（如：刚刚、5分钟前、2小时前、3天前等）
     */
    @Schema(description = "格式化后的时间显示", example = "5分钟前")
    private String formatted_time;
}
