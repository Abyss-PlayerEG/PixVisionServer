package top.playereg.pix_vision.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import top.playereg.pix_vision.pojo.Works;

/**
 * 收藏服务接口
 *
 * @author PlayerEG
 */
public interface StarService {

    /**
     * 切换收藏状态（收藏或取消收藏）
     *
     * @param userId 用户 ID
     * @param workId 作品 ID
     * @return 当前是否处于收藏状态
     */
    Boolean toggleStar(Integer userId, Integer workId);

    /**
     * 查询用户是否已收藏某作品
     *
     * @param userId 用户 ID
     * @param workId 作品 ID
     * @return 是否已收藏
     */
    Boolean isStarred(Integer userId, Integer workId);

    /**
     * 查询作品的收藏总数
     *
     * @param workId 作品 ID
     * @return 收藏数
     */
    Integer getStarCount(Integer workId);

    /**
     * 分页查询用户收藏过的作品列表（只返回审核通过的作品）
     *
     * @param page   分页对象
     * @param userId 用户 ID
     * @param orderBy 排序方式："oldest" - 按最早收藏，其他值或 null - 按最新收藏（默认）
     * @return 分页结果
     * @author PlayerEG
     */
    IPage<Works> getUserStarredWorks(Page<Works> page, Integer userId, String orderBy);
}
