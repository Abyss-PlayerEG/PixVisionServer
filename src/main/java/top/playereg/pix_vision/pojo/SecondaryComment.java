package top.playereg.pix_vision.pojo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 二级评论响应实体（用于前端展示）
 * <p>
 * 表示回复其他评论的二级评论，不包含子评论列表
 *
 * @author PlayerEG
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "二级评论响应实体")
public class SecondaryComment {

    /**
     * 评论 ID
     */
    @Schema(description = "评论 ID")
    private Integer conmment_id;

    /**
     * 用户 ID
     */
    @Schema(description = "用户 ID")
    private Integer user_id;

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
     * 作品 ID
     */
    @Schema(description = "作品 ID")
    private Integer work_id;

    /**
     * 父评论 ID（所回复的评论ID）
     */
    @Schema(description = "父评论 ID")
    private Integer parent_comment_id;

    /**
     * 所属一级评论 ID（用于快速定位根评论）
     */
    @Schema(description = "所属一级评论 ID")
    private Integer in_comment_id;

    /**
     * 评论层级（固定为 2）
     */
    @Schema(description = "评论层级")
    private Integer conmment_floor;

    /**
     * 评论内容，限制长度 125 字
     */
    @Schema(description = "评论内容，限制长度 125 字")
    private String conmment_text;

    /**
     * 删除标签：0 - 未删除、1 - 已删除
     */
    @Schema(description = "删除标签：0 - 未删除、1 - 已删除")
    private Boolean is_delete;

    /**
     * 评论时间
     */
    @Schema(description = "评论时间")
    private LocalDateTime time;
}
