package top.playereg.pix_vision.pojo;

/**
 * 响应数据
 *
 * @author PlayerEG
 */

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Data
@SuppressWarnings("unused")
@ApiModel("响应数据类型")
public class ResponsePojo<T> {

    private static final Logger log = LoggerFactory.getLogger(ResponsePojo.class);
    @ApiModelProperty(value = "响应数据")
    private T data;
    @ApiModelProperty(value = "响应说明信息")
    private String message;
    @ApiModelProperty(value = "响应状态码（200-成功；500-失败）")
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
    @ApiModelProperty(value = "成功响应")
    public static <T> ResponsePojo<T> success(T t, String message) {
        log.info("200 {}", message);
        return new ResponsePojo<>(t, message, 200);
    }

    @NotNull
    @Contract("_, _ -> new")
    @ApiModelProperty(value = "失败响应")
    public static <T> ResponsePojo<T> error(T t, String message) {
        log.error("500 {}", message);
        return new ResponsePojo<>(t, message, 500);
    }
}

