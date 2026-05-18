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
}
