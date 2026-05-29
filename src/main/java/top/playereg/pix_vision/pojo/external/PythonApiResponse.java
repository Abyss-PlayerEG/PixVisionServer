package top.playereg.pix_vision.pojo.external;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * Python API 统一响应包装类
 * <p>
 * 用于接收像素视觉 Python 辅助服务的 API 响应
 * </p>
 *
 * @param <T> 数据类型
 * @author PlayerEG
 */
@Data
public class PythonApiResponse<T> {
    /**
     * 业务状态码
     * 0 - 成功
     * 400 - 参数错误
     * 404 - 资源不存在
     * 502 - 上游服务错误（限流等）
     * 500 - 服务器内部错误
     */
    private Integer code;

    /**
     * 响应消息
     */
    private String message;

    /**
     * 响应数据
     */
    private T data;

    /**
     * 时间戳
     */
    private LocalDateTime timestamp;

    /**
     * 判断请求是否成功
     *
     * @return true-成功，false-失败
     */
    public boolean isSuccess() {
        return code != null && code == 0;
    }
}
