package top.playereg.pix_vision.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import top.playereg.pix_vision.pojo.entity.Works;

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

    /**
     * 分页查询用户点赞过的作品列表（只返回审核通过的作品）
     *
     * @param page   分页对象
     * @param userId 用户 ID
     * @param orderBy 排序方式："oldest" - 按最早点赞，其他值或 null - 按最新点赞（默认）
     * @return 分页结果
     * @author PlayerEG
     */
    IPage<Works> getUserLikedWorks(Page<Works> page, Integer userId, String orderBy);
}
