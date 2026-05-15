package top.playereg.pix_vision.pojo.adminPojo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 管理员批量删除用户结果对象
 *
 * @author PlayerEG
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "管理员批量删除用户结果")
public class AdminBatchDeleteUserResult {
    @Schema(description = "需要删除的用户总数", example = "10")
    private Integer totalCount;

    @Schema(description = "成功删除的用户数量", example = "9")
    private Integer successCount;

    @Schema(description = "失败用户的 ID 列表", example = "[4, 7]")
    private List<Integer> failedUserIds;
}
