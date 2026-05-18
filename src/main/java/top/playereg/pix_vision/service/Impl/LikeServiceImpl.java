package top.playereg.pix_vision.service.Impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.playereg.pix_vision.mapper.LikesMapper;
import top.playereg.pix_vision.mapper.WorksMapper;
import top.playereg.pix_vision.pojo.Like;
import top.playereg.pix_vision.pojo.Works;
import top.playereg.pix_vision.service.LikeService;
import top.playereg.pix_vision.util.PixVisionLogger;

import java.time.LocalDateTime;

/**
 * 点赞服务实现类
 *
 * @author PlayerEG
 */
@Service
public class LikeServiceImpl implements LikeService {

    private static final PixVisionLogger log = PixVisionLogger.create(LikeServiceImpl.class);

    @Autowired
    private LikesMapper likesMapper;

    @Autowired
    private WorksMapper worksMapper;

    /**
     * 切换点赞状态（点赞或取消点赞）
     * <p>
     * 该方法实现了智能切换逻辑：如果用户未点赞则执行点赞，如果已点赞则取消点赞。
     * 同时会原子性地更新作品表中的总点赞数。
     * </p>
     *
     * <h3>使用场景</h3>
     * <ol>
     *   <li>用户在作品详情页点击“红心”按钮时调用。</li>
     *   <li>前端需要确保点赞操作幂等性时调用。</li>
     * </ol>
     *
     * <h3>注意事项</h3>
     * <ul>
     *   <li>该方法必须运行在事务环境中，以确保数据一致性。</li>
     *   <li>点赞记录采用逻辑删除方式保留历史数据。</li>
     * </ul>
     *
     * @param userId 用户 ID
     * @param workId 作品 ID
     * @return 当前是否处于点赞状态（true - 已点赞, false - 未点赞）
     * @author PlayerEG
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean toggleLike(Integer userId, Integer workId) {
        if (userId == null || workId == null) {
            return false;
        }

        // 1. 查询当前点赞记录
        Like like = likesMapper.selectByUserAndWork(userId, workId);
        boolean isCurrentlyLiked = (like != null && !like.getIs_delete());

        if (isCurrentlyLiked) {
            // 2. 如果已点赞，则取消点赞（逻辑删除）
            likesMapper.updateDeleteStatus(userId, workId, true);
            // 3. 更新作品点赞数 -1
            updateWorkLikeCount(workId, -1);
            log.info("用户 {} 取消了作品 {} 的点赞", userId, workId);
            return false;
        } else {
            // 4. 如果未点赞，则执行点赞
            if (like == null) {
                // 不存在记录，插入新记录
                Like newLike = new Like();
                newLike.setUser_id(userId);
                newLike.setWork_id(workId);
                newLike.setIs_delete(false);
                newLike.setTime(LocalDateTime.now());
                likesMapper.insertLike(newLike);
            } else {
                // 存在但已删除的记录，恢复它
                likesMapper.updateDeleteStatus(userId, workId, false);
            }

            // 5. 更新作品点赞数 +1
            updateWorkLikeCount(workId, 1);
            log.info("用户 {} 点赞了作品 {}", userId, workId);
            return true;
        }
    }

    /**
     * 查询用户是否已点赞某作品
     * <p>
     * 通过查询 tb_like 表中是否存在未删除的记录来判断。
     * </p>
     *
     * @param userId 用户 ID
     * @param workId 作品 ID
     * @return 是否已点赞
     * @author PlayerEG
     */
    @Override
    public Boolean isLiked(Integer userId, Integer workId) {
        if (userId == null || workId == null) {
            return false;
        }
        Like like = likesMapper.selectByUserAndWork(userId, workId);
        return like != null && !like.getIs_delete();
    }

    /**
     * 查询作品的点赞总数
     * <p>
     * 优先从 Works 表获取冗余字段，以保证查询性能。
     * </p>
     *
     * @param workId 作品 ID
     * @return 点赞数
     * @author PlayerEG
     */
    @Override
    public Integer getLikeCount(Integer workId) {
        if (workId == null) {
            return 0;
        }
        // 优先从 Works 表获取冗余字段，保证性能
        return worksMapper.selectById(workId).getLike_count();
    }

    /**
     * 原子更新作品点赞数
     *
     * @param workId 作品 ID
     * @param delta  变化量（+1 或 -1）
     */
    private void updateWorkLikeCount(Integer workId, int delta) {
        worksMapper.updateLikeCount(workId, delta);
    }

    /**
     * 分页查询用户点赞过的作品列表（只返回审核通过的作品）
     * <p>
     * 通过关联查询 tb_like 和 tb_works 表，获取用户点赞且未删除、审核通过的作品。
     * </p>
     *
     * <h3>使用场景</h3>
     * <ol>
     *   <li>前端展示用户的“我喜欢”列表时调用。</li>
     *   <li>查看自己点赞过的作品历史记录。</li>
     * </ol>
     *
     * <h3>注意事项</h3>
     * <ul>
     *   <li>这是一个公开接口，无需登录认证。</li>
     *   <li>只返回审核通过的作品（approval_status = 10）。</li>
     *   <li>默认按点赞时间倒序排列，最新点赞的作品排在前面。</li>
     *   <li>可通过 orderBy 参数控制排序方式：'oldest' - 按最早点赞。</li>
     *   <li>如果用户没有点赞过任何作品，返回空的分页结果。</li>
     * </ul>
     *
     * @param page   分页对象
     * @param userId 用户 ID
     * @param orderBy 排序方式："oldest" - 按最早点赞，其他值或 null - 按最新点赞（默认）
     * @return 分页结果
     * @author PlayerEG
     */
    @Override
    public IPage<Works> getUserLikedWorks(Page<Works> page, Integer userId, String orderBy) {
        if (userId == null || userId <= 0) {
            log.warn("用户 ID 无效: {}", userId);
            return new Page<>(page.getCurrent(), page.getSize());
        }

        log.info("开始查询用户点赞作品，用户 ID: {}, 页码: {}, 每页大小: {}, 排序: {}",
            userId, page.getCurrent(), page.getSize(), orderBy);

        // 调用 Mapper 层查询
        IPage<Works> result = likesMapper.selectUserLikedWorks(page, userId, orderBy);

        log.info("查询用户点赞作品完成，用户 ID: {}, 总数: {}, 当前页: {}",
            userId, result.getTotal(), result.getCurrent());

        return result;
    }
}
