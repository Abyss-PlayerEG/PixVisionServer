package top.playereg.pix_vision.pojo.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 管理员批量操作作品结果对象
 *
 * @author blue_sky_ks
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "管理员批量操作评论结果")
public class AdminBatchOperateCommentResult {

    @Schema(description = "需要操作的评论总数", example = "10")
    private Integer totalCount;

    @Schema(description = "成功操作的评论数量", example = "9")
    private Integer successCount;

    @Schema(description = "失败评论的 ID 列表", example = "[4, 7]")
    private List<Integer> failedWorkIds;

}
