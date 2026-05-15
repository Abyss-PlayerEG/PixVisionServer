package top.playereg.pix_vision.pojo.commentsPojo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 评论基础类
 * <p>
 * 包含所有评论类型的共同字段，用于消除代码重复
 *
 * @author PlayerEG
 */
@Data
@NoArgsConstructor
@Schema(description = "评论基础类")
public class BaseComment {

    /**
     * 评论 ID
     */
    @Schema(description = "评论 ID")
    private Integer comment_id;

    /**
     * 用户 ID
     */
    @Schema(description = "用户 ID")
    private Integer user_id;

    /**
     * 作品 ID
     */
    @Schema(description = "作品 ID")
    private Integer work_id;

    /**
     * 父评论 ID（二级评论时必填）
     */
    @Schema(description = "父评论 ID")
    private Integer parent_comment_id;

    /**
     * 所属一级评论 ID（用于快速定位根评论）
     */
    @Schema(description = "所属一级评论 ID")
    private Integer in_comment_id;

    /**
     * 评论层级：1 - 作品评论、2 - 二级评论
     */
    @Schema(description = "评论层级")
    private Integer comment_floor;

    /**
     * 评论内容，限制长度 125 字
     */
    @Schema(description = "评论内容，限制长度 125 字")
    private String comment_text;

    /**
     * 审核状态：10 - 正常、20 - 待审核、30 - 未过审
     */
    @Schema(description = "审核状态：10 - 正常、20 - 待审核、30 - 未过审")
    private Integer approval_status;

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
