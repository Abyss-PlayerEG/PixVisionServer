package top.playereg.pix_vision.pojo;

import com.baomidou.mybatisplus.annotation.TableLogic;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "评论实体")
public class Comments {

    @Schema(description = "评论 ID")
    Integer conmment_id;

    @Schema(description = "用户 ID")
    Integer user_id;

    @Schema(description = "作品 ID")
    Integer work_id;

    @Schema(description = "回复的评论 ID")
    Integer answer_conmment_id;

    @Schema(description = "评论层级：1 - 作品评论、2 - 二级评论")
    String conmment_floor;

    @Schema(description = "评论内容，限制长度 125 字")
    String conmment_text;

    @Schema(description = "评论状态：10 - 正常、20 - 待审核、30 - 封禁")
    String status;

    @TableLogic
    @Schema(description = "删除标签：0 - 未删除、1 - 已删除")
    Boolean is_delete;
}
