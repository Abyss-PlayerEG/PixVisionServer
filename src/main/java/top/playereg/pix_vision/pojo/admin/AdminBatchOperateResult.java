package top.playereg.pix_vision.pojo.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 管理员批量操作通用结果对象
 * <p>
 * 用于所有批量操作的结果返回，包含总数、成功数和失败ID列表
 * </p>
 *
 * @author PlayerEG
 * @since DevBeta-3.4
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "管理员批量操作结果")
public class AdminBatchOperateResult {

    @Schema(description = "操作总数", example = "10")
    private Integer totalCount;

    @Schema(description = "成功数量", example = "9")
    private Integer successCount;

    @Schema(description = "失败的 ID 列表", example = "[4, 7]")
    private List<Integer> failedIds;
}
