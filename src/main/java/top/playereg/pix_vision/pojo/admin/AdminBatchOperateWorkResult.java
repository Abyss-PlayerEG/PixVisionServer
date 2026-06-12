package top.playereg.pix_vision.pojo.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 管理员批量操作作品结果对象
 *
 * @author blue_sky_ks
 * @see AdminBatchOperateResult 通用批量操作结果基类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Schema(description = "管理员批量操作作品结果")
public class AdminBatchOperateWorkResult extends AdminBatchOperateResult {

    @Schema(description = "需要操作的作品总数", example = "10")
    private Integer totalCount;

    @Schema(description = "成功操作的作品数量", example = "9")
    private Integer successCount;

    @Schema(description = "失败作品的 ID 列表", example = "[4, 7]")
    private List<Integer> failedWorkIds;
}
