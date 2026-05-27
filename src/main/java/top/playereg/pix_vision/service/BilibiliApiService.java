package top.playereg.pix_vision.service;

/**
 * B站 API 服务接口
 * <p>
 * 提供 B站账号检测和用户信息查询功能
 * </p>
 *
 * @author PlayerEG
 */
public interface BilibiliApiService {

    /**
     * 检测 B站账号是否存在
     *
     * @param userId B站用户 ID（mid）
     * @return true-账号存在，false-账号不存在
     * @throws RuntimeException 当 API 调用失败时抛出异常
     */
    Boolean checkAccountExists(String userId);

    /**
     * 获取 B站用户头像 URL
     * <p>
     * 调用 Python 辅助服务获取指定 B站用户的头像地址
     * </p>
     *
     * @param userId B站用户 ID（mid）
     * @return 用户头像 URL
     * @throws RuntimeException 当 API 调用失败或用户不存在时抛出异常
     */
    String getUserFaceUrl(String userId);
}
