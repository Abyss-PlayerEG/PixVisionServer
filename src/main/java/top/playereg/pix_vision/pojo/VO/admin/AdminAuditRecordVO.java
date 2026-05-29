package top.playereg.pix_vision.pojo.VO.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import top.playereg.pix_vision.pojo.entity.ContentAuditRecord;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@Schema(description = "管理员审核记录视图")
public class AdminAuditRecordVO extends ContentAuditRecord {
}
