package top.playereg.pix_vision.pojo.VO.admin;

import com.baomidou.mybatisplus.annotation.TableField;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import top.playereg.pix_vision.pojo.entity.ContentAuditRecord;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@Schema(description = "管理员审核记录视图（含原始内容）")
public class AdminAuditRecordVO extends ContentAuditRecord {

    @TableField(exist = false)
    @Schema(description = "审核的原始内容，作品/系列为「标题|详细信息」格式，评论/昵称直接返回字符串")
    private String original_content;
}
