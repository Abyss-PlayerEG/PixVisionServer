package top.playereg.pix_vision.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("tb_content_audit_record")
@Schema(description = "AI内容审核记录实体")
public class ContentAuditRecord {

    @TableId
    @Schema(description = "审核记录 ID")
    Integer record_id;

    @Schema(description = "内容类型：100-作品、200-评论、300-系列、400-昵称")
    Integer content_type;

    @Schema(description = "对应内容 ID（work_id/comment_id/series_id/lock_id）")
    Integer content_id;

    @Schema(description = "审核结果：10-通过、20-待审核、30-违规")
    Integer approval_status;

    @Schema(description = "AI 审核判断依据")
    String audit_reason;

    @Schema(description = "命中敏感词，JSON数组字符串")
    String insult_words;

    @Schema(description = "审核的原始内容")
    String original_content;

    @Schema(description = "审核时间")
    Timestamp create_time;
}
