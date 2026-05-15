package top.playereg.pix_vision.pojo.commentsPojo;

import com.baomidou.mybatisplus.annotation.TableLogic;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 评论数据库实体类
 * <p>
 * 继承 BaseComment，添加 MyBatis-Plus 注解用于数据库操作
 *
 * @author PlayerEG
 * @see top.playereg.pix_vision.controller.CommentController 评论控制器
 * @see top.playereg.pix_vision.service.CommentService 评论服务接口
 * @since V1.2
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@Schema(description = "评论数据库实体")
public class Comments extends BaseComment {

    /**
     * 删除标签：0 - 未删除、1 - 已删除
     */
    @TableLogic
    @Schema(description = "删除标签：0 - 未删除、1 - 已删除")
    private Boolean is_delete;
}
