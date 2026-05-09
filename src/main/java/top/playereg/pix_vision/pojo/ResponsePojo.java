package top.playereg.pix_vision.pojo;

import cn.hutool.core.date.DateUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import top.playereg.pix_vision.util.PixVisionLogger;

import java.util.Date;

/**
 * 响应数据
 *
 * @author PlayerEG
 * @version 3.0
 */
@Data
@SuppressWarnings("unused")
@Schema(description = "响应数据类型")
public class ResponsePojo<T> {

    private static final PixVisionLogger log = PixVisionLogger.create(ResponsePojo.class);

    @Schema(description = "响应数据")
    private T data;
    @Schema(description = "响应说明信息")
    private String message;
    @Schema(description = "响应状态码（200-成功；500-失败）")
    private int recode;
    @Schema(description = "响应状态")
    private String status;
    @Schema(description = "响应时间")
    private String time_str;


    public ResponsePojo(T T) {
        this.data = T;
        this.recode = 200;
        this.message = "";
        this.status = "Success";
        this.time_str = null;
    }

    private ResponsePojo(
        T T,
        String message,
        int recode,
        String status,
        String time_str
    ) {
        this.data = T;
        this.recode = recode;
        this.message = message;
        this.status = status;
        this.time_str = time_str;
    }

    @NotNull
    @Contract("_, _ -> new")
    @Schema(description = "成功响应")
    public static <T> ResponsePojo<T> success(T t, String message) {
        return buildResponse(t, message, 200, "SUCCESS", true);
    }

    @NotNull
    @Contract("_, _ -> new")
    @Schema(description = "失败响应")
    public static <T> ResponsePojo<T> error(T t, String message) {
        return buildResponse(t, message, 500, "ERROR", false);
    }

    /**
     * 构建响应对象的通用模板方法
     *
     * @param data      响应数据
     * @param message   响应消息
     * @param code      状态码
     * @param status    状态字符串
     * @param isSuccess 是否为成功响应（用于决定日志级别）
     * @return ResponsePojo 对象
     * @author PlayerEG
     */
    private static <T> ResponsePojo<T> buildResponse(T data, String message, int code, String status, boolean isSuccess) {
        String logString = code + " : " + message;
        String ts = DateUtil.format(new Date(), "yyyy/MM/dd HH:mm:ss");
        if (isSuccess) log.info(logString);
        else log.error(logString);
        return new ResponsePojo<>(data, message, code, status, ts);
    }
}

