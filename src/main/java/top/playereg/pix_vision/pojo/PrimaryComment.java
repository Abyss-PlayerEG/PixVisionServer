package top.playereg.pix_vision.pojo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 一级评论响应实体（用于前端展示）
 * <p>
 * 表示对作品的直接评论，包含二级评论列表
 *
 * @author PlayerEG
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "一级评论响应实体")
public class PrimaryComment {

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
     * 父评论 ID（一级评论为 null）
     */
    @Schema(description = "父评论 ID")
    private Integer parent_comment_id;

    /**
     * 所属一级评论 ID（一级评论指向自己或为 null）
     */
    @Schema(description = "所属一级评论 ID")
    private Integer in_comment_id;

    /**
     * 评论层级（固定为 1）
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

    /**
     * 二级评论列表
     */
    @Schema(description = "二级评论列表")
    private List<SecondaryComment> children;
}
