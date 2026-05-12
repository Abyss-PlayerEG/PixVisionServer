package top.playereg.pix_vision.pojo;

import com.baomidou.mybatisplus.annotation.TableLogic;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 评论实体类
 * <p>
 * 用于存储用户对作品的评论信息，支持一级评论（对作品评论）和二级评论（回复其他评论）。
 *
 * <h3>使用场景</h3>
 * <ol>
 *   <li>用户对作品发表一级评论</li>
 *   <li>用户回复其他用户的评论（二级评论）</li>
 *   <li>查询作品的评论列表</li>
 *   <li>管理评论数据（逻辑删除）</li>
 * </ol>
 *
 * <h3>注意事项</h3>
 * <ul>
 *   <li>评论内容限制最多 125 个汉字</li>
 *   <li>评论层级只能为 1（作品评论）或 2（二级评论）</li>
 *   <li>二级评论必须提供回复的评论 ID</li>
 *   <li>采用逻辑删除方式，is_delete=0 表示未删除，is_delete=1 表示已删除</li>
 * </ul>
 *
 * @author PlayerEG
 * @see top.playereg.pix_vision.controller.CommentController 评论控制器
 * @see top.playereg.pix_vision.service.CommentService 评论服务接口
 * @since V1.2
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "评论实体")
public class Comments {

    /**
     * 评论 ID
     */
    @Schema(description = "评论 ID")
    Integer conmment_id;

    /**
     * 用户 ID
     */
    @Schema(description = "用户 ID")
    Integer user_id;

    /**
     * 作品 ID
     */
    @Schema(description = "作品 ID")
    Integer work_id;

    /**
     * 回复的评论 ID（二级评论时必填）
     */
    @Schema(description = "回复的评论 ID")
    Integer parent_comment_id;

    /**
     * 所属一级评论 ID（用于二级评论快速定位根评论）
     */
    @Schema(description = "所属一级评论 ID")
    Integer in_comment_id;

    /**
     * 评论层级：1 - 作品评论、2 - 二级评论
     */
    @Schema(description = "评论层级：1 - 作品评论、2 - 二级评论")
    Integer conmment_floor;

    /**
     * 评论内容，限制长度 125 字
     */
    @Schema(description = "评论内容，限制长度 125 字")
    String conmment_text;

    /**
     * 删除标签：0 - 未删除、1 - 已删除
     */
    @TableLogic
    @Schema(description = "删除标签：0 - 未删除、1 - 已删除")
    Boolean is_delete;

    /**
     * 评论时间
     */
    @Schema(description = "评论时间")
    java.time.LocalDateTime time;
}
