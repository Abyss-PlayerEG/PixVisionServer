package top.playereg.pix_vision.pojo.external;

import lombok.Data;

/**
 * B站用户信息查询结果
 * <p>
 * 用于接收 Python API {@code GET /accounts/bilibili/{user_id}/info} 返回的响应数据，
 * 包含平台信息、用户 ID 和用户详细信息（头像 URL、用户名等）。
 * </p>
 *
 * @author PlayerEG
 * @see top.playereg.pix_vision.service.BilibiliApiService
 */
@Data
public class BilibiliUserInfoResult {

    /**
     * 平台名称（固定为 bilibili）
     */
    private String platform;

    /**
     * B站用户 ID（mid）
     */
    private String userId;

    /**
     * B站用户详细信息
     */
    private BilibiliUserDetail info;

    /**
     * B站用户详细信息
     * <p>
     * 包含头像 URL、用户名等关键字段
     * </p>
     */
    @Data
    public static class BilibiliUserDetail {

        /**
         * 用户头像 URL
         */
        private String face;

        /**
         * 用户昵称
         */
        private String name;
    }
}
