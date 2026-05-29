package top.playereg.pix_vision.pojo.VO.comment;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 一级评论响应实体（用于前端展示）
 * <p>
 * 继承 BaseCommentVO，添加二级评论列表
 *
 * @author PlayerEG
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@Schema(description = "一级评论响应实体")
public class PrimaryComment extends BaseCommentVO {

    /**
     * 二级评论列表
     */
    @Schema(description = "二级评论列表")
    private List<SecondaryComment> children;
}
