package top.playereg.pix_vision.pojo;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "待审核数据实体")
public class PendingReviews {

    @TableId
    @Schema(description = "待审核记录 ID")
    Integer pending_reviews_id;

    @Schema(description = "数据类型：作品-100、评论-200、头像-300")
    Integer data_type;

    @Schema(description = "审核状态：10 - 正常、20 - 待审核、30 - 封禁")
    Integer status;

    @Schema(description = "待审核作品 ID，用于作品审核记录")
    Integer work_id;

    @Schema(description = "待审核评论 ID，用于评论审核记录")
    Integer comment_id;

    @Schema(description = "待审核用户 ID，用于用户信息审核记录")
    Integer user_id;

    @TableLogic
    @Schema(description = "删除标签：0 - 未删除、1 - 已删除")
    Boolean is_delete;

    @Schema(description = "数据条目更新时间戳")
    Timestamp update_time;

    @Schema(description = "修改者 id，系统修改为 0")
    Integer update_user;

    @Schema(description = "数据条目创建时间戳")
    Timestamp create_time;

    @Schema(description = "存储创建者 id，系统创建为 0")
    Integer create_user;
}
