package top.playereg.pix_vision.pojo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 批量删除访问历史记录结果对象
 *
 * @author PlayerEG
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "批量删除访问历史记录结果")
public class BatchDeleteHistoryResult {
    @Schema(description = "需要删除的历史记录总数", example = "10")
    private Integer totalCount;

    @Schema(description = "成功删除的历史记录数量", example = "9")
    private Integer successCount;

    @Schema(description = "失败的作品 ID 列表", example = "[4, 7]")
    private List<Integer> failedWorkIds;
}
