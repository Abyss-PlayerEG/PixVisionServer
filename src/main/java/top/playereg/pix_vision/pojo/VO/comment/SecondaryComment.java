package top.playereg.pix_vision.pojo.VO.comment;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 二级评论响应实体（用于前端展示）
 * <p>
 * 继承 BaseCommentVO，表示回复其他评论的二级评论
 *
 * @author PlayerEG
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@Schema(description = "二级评论响应实体")
public class SecondaryComment extends BaseCommentVO {
}
