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
@Schema(description = "管理员审核记录视图")
public class AdminAuditRecordVO extends ContentAuditRecord {

    @TableField(exist = false)
    @Schema(description = "用户 ID")
    private Integer user_id;

    @TableField(exist = false)
    @Schema(description = "用户昵称")
    private String nickname;
}
