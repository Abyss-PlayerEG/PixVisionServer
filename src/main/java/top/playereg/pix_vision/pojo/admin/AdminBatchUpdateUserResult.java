package top.playereg.pix_vision.pojo.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 管理员批量更新用户信息结果对象
 *
 * @author PlayerEG
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "管理员批量更新用户信息结果")
public class AdminBatchUpdateUserResult {

    @Schema(description = "需要更新的用户总数", example = "10")
    private Integer totalCount;

    @Schema(description = "成功更新的用户数量", example = "9")
    private Integer successCount;

    @Schema(description = "失败用户的 ID 列表", example = "[4, 7]")
    private List<Integer> failedUserIds;
}
