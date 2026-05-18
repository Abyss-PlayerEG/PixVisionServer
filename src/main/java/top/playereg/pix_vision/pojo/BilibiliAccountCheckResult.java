package top.playereg.pix_vision.pojo;

import lombok.Data;

/**
 * B站账号检测结果
 * <p>
 * 用于接收 Python API 返回的账号检测数据
 * </p>
 *
 * @author PlayerEG
 */
@Data
public class BilibiliAccountCheckResult {
    /**
     * 平台名称
     */
    private String platform;

    /**
     * 用户 ID
     */
    private String userId;

    /**
     * 账号是否存在
     */
    private Boolean exists;
}
