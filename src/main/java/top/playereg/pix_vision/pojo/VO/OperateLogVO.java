package top.playereg.pix_vision.pojo.VO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import top.playereg.pix_vision.pojo.entity.OperateLog;

/**
 * 操作日志视图对象
 * <p>
 * 继承 OperateLog，扩展关联用户名信息，用于日志查询接口返回。
 * </p>
 *
 * @author PlayerEG
 * @see OperateLog
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "操作日志视图对象（含用户名）")
public class OperateLogVO extends OperateLog {

    @Schema(description = "操作者用户名", example = "li_hua")
    private String username;
}
