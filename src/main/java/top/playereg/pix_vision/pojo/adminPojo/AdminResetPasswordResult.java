package top.playereg.pix_vision.pojo.adminPojo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 管理员批量重置密码结果对象
 *
 * @author PlayerEG
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "管理员批量重置密码结果")
public class AdminResetPasswordResult {

    @Schema(description = "需要重置的用户总数", example = "10")
    private Integer totalCount;

    @Schema(description = "成功重置密码的用户数量", example = "9")
    private Integer successCount;

    @Schema(description = "成功发送邮件的数量", example = "8")
    private Integer emailSentCount;

    @Schema(description = "失败用户的 ID 列表", example = "[4, 7]")
    private java.util.List<Integer> failedUserIds;
}
