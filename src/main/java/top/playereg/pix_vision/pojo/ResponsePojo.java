package top.playereg.pix_vision.pojo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.playereg.pix_vision.enums.LogColor;

/**
 * 响应数据
 *
 * @author PlayerEG
 */
@Data
@SuppressWarnings("unused")
@Schema(description = "响应数据类型")
public class ResponsePojo<T> {

    private static final Logger log = LoggerFactory.getLogger(ResponsePojo.class);
    @Schema(description = "响应数据")
    private T data;
    @Schema(description = "响应说明信息")
    private String message;
    @Schema(description = "响应状态码（200-成功；500-失败）")
    private int recode;

    private byte status;

    public ResponsePojo(T t) {
        this.data = t;
        this.recode = 200;
        this.message = "";
    }

    private ResponsePojo(
            T t,
            String message,
            int recode
    ) {
        this.data = t;
        this.recode = recode;
        this.message = message;
    }

    @NotNull
    @Contract("_, _ -> new")
    @Schema(description = "成功响应")
    public static <T> ResponsePojo<T> success(T t, String message) {
        log.info(LogColor.colorize("200 {}",LogColor.GREEN), message);
        return new ResponsePojo<>(t, message, 200);
    }

    @NotNull
    @Contract("_, _ -> new")
    @Schema(description = "失败响应")
    public static <T> ResponsePojo<T> error(T t, String message) {
        log.error(LogColor.colorize("500 {}",LogColor.RED), message);
        return new ResponsePojo<>(t, message, 500);
    }
}

