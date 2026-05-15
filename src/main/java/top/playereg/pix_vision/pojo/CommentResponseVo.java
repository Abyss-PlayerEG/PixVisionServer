package top.playereg.pix_vision.pojo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 评论响应实体（用于前端展示）
 * <p>
 * 包含用户信息和嵌套的回复结构，方便前端渲染
 *
 * @author PlayerEG
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "评论响应实体")
public class CommentResponseVo {

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
    @Schema(description = "评论层级：1 - 作品评论、2 - 二级评论")
    private Integer comment_floor;

    /**
     * 评论内容，限制长度 125 字
     */
    @Schema(description = "评论内容，限制长度 125 字")
    private String comment_text;

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
     * 子评论列表（仅一级评论有值，二级评论为空数组）
     */
    @Schema(description = "子评论列表")
    private List<CommentResponseVo> children;
}
