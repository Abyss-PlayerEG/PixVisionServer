package top.playereg.pix_vision.service;

/**
 * 点赞服务接口
 *
 * @author PlayerEG
 */
public interface LikeService {

    /**
     * 切换点赞状态（点赞或取消点赞）
     *
     * @param userId 用户 ID
     * @param workId 作品 ID
     * @return 当前是否处于点赞状态
     */
    Boolean toggleLike(Integer userId, Integer workId);

    /**
     * 查询用户是否已点赞某作品
     *
     * @param userId 用户 ID
     * @param workId 作品 ID
     * @return 是否已点赞
     */
    Boolean isLiked(Integer userId, Integer workId);

    /**
     * 查询作品的点赞总数
     *
     * @param workId 作品 ID
     * @return 点赞数
     */
    Integer getLikeCount(Integer workId);
}
