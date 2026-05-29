package top.playereg.pix_vision.service.Impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.playereg.pix_vision.mapper.StarsMapper;
import top.playereg.pix_vision.mapper.WorksMapper;
import top.playereg.pix_vision.pojo.entity.Star;
import top.playereg.pix_vision.pojo.entity.Works;
import top.playereg.pix_vision.service.StarService;
import top.playereg.pix_vision.util.PixVisionLogger;

import java.time.LocalDateTime;

/**
 * 收藏服务实现类
 *
 * @author PlayerEG
 */
@Service
public class StarServiceImpl implements StarService {

    private static final PixVisionLogger log = PixVisionLogger.create(StarServiceImpl.class);

    @Autowired
    private StarsMapper starsMapper;

    @Autowired
    private WorksMapper worksMapper;

    /**
     * 切换收藏状态（收藏或取消收藏）
     * <p>
     * 该方法实现了智能切换逻辑：如果用户未收藏则执行收藏，如果已收藏则取消收藏。
     * 同时会原子性地更新作品表中的总收藏数。
     * </p>
     *
     * <h3>使用场景</h3>
     * <ol>
     *   <li>用户在作品详情页点击"星标"按钮时调用。</li>
     *   <li>前端需要确保收藏操作幂等性时调用。</li>
     * </ol>
     *
     * <h3>注意事项</h3>
     * <ul>
     *   <li>该方法必须运行在事务环境中，以确保数据一致性。</li>
     *   <li>收藏记录采用逻辑删除方式保留历史数据。</li>
     * </ul>
     *
     * @param userId 用户 ID
     * @param workId 作品 ID
     * @return 当前是否处于收藏状态（true - 已收藏, false - 未收藏）
     * @author PlayerEG
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean toggleStar(Integer userId, Integer workId) {
        if (userId == null || workId == null) {
            return false;
        }

        // 1. 查询当前收藏记录
        Star star = starsMapper.selectByUserAndWork(userId, workId);
        boolean isCurrentlyStarred = (star != null && !star.getIs_delete());

        if (isCurrentlyStarred) {
            // 2. 如果已收藏，则取消收藏（逻辑删除）
            starsMapper.updateDeleteStatus(userId, workId, true);
            // 3. 更新作品收藏数 -1
            updateWorkStarCount(workId, -1);
            log.info("用户 {} 取消了作品 {} 的收藏", userId, workId);
            return false;
        } else {
            // 4. 如果未收藏，则执行收藏
            if (star == null) {
                // 不存在记录，插入新记录
                Star newStar = new Star();
                newStar.setUser_id(userId);
                newStar.setWork_id(workId);
                newStar.setIs_delete(false);
                newStar.setTime(LocalDateTime.now());
                starsMapper.insertStar(newStar);
            } else {
                // 存在但已删除的记录，恢复它
                starsMapper.updateDeleteStatus(userId, workId, false);
            }

            // 5. 更新作品收藏数 +1
            updateWorkStarCount(workId, 1);
            log.info("用户 {} 收藏了作品 {}", userId, workId);
            return true;
        }
    }

    /**
     * 查询用户是否已收藏某作品
     * <p>
     * 通过查询 tb_star 表中是否存在未删除的记录来判断。
     * </p>
     *
     * @param userId 用户 ID
     * @param workId 作品 ID
     * @return 是否已收藏
     * @author PlayerEG
     */
    @Override
    public Boolean isStarred(Integer userId, Integer workId) {
        if (userId == null || workId == null) {
            return false;
        }
        Star star = starsMapper.selectByUserAndWork(userId, workId);
        return star != null && !star.getIs_delete();
    }

    /**
     * 查询作品的收藏总数
     * <p>
     * 优先从 Works 表获取冗余字段，以保证查询性能。
     * </p>
     *
     * @param workId 作品 ID
     * @return 收藏数
     * @author PlayerEG
     */
    @Override
    public Integer getStarCount(Integer workId) {
        if (workId == null) {
            return 0;
        }
        // 优先从 Works 表获取冗余字段，保证性能
        return worksMapper.selectById(workId).getStar_count();
    }

    /**
     * 原子更新作品收藏数
     *
     * @param workId 作品 ID
     * @param delta  变化量（+1 或 -1）
     */
    private void updateWorkStarCount(Integer workId, int delta) {
        worksMapper.updateStarCount(workId, delta);
    }

    /**
     * 分页查询用户收藏过的作品列表（只返回审核通过的作品）
     * <p>
     * 通过关联查询 tb_star 和 tb_works 表，获取用户收藏且未删除、审核通过的作品。
     * </p>
     *
     * <h3>使用场景</h3>
     * <ol>
     *   <li>前端展示用户的“我的收藏”列表时调用。</li>
     *   <li>查看自己收藏过的作品历史记录。</li>
     * </ol>
     *
     * <h3>注意事项</h3>
     * <ul>
     *   <li>这是一个公开接口，无需登录认证。</li>
     *   <li>只返回审核通过的作品（approval_status = 10）。</li>
     *   <li>默认按收藏时间倒序排列，最新收藏的作品排在前面。</li>
     *   <li>可通过 orderBy 参数控制排序方式：'oldest' - 按最早收藏。</li>
     *   <li>如果用户没有收藏过任何作品，返回空的分页结果。</li>
     * </ul>
     *
     * @param page   分页对象
     * @param userId 用户 ID
     * @param orderBy 排序方式："oldest" - 按最早收藏，其他值或 null - 按最新收藏（默认）
     * @return 分页结果
     * @author PlayerEG
     */
    @Override
    public IPage<Works> getUserStarredWorks(Page<Works> page, Integer userId, String orderBy) {
        if (userId == null || userId <= 0) {
            log.warn("用户 ID 无效: {}", userId);
            return new Page<>(page.getCurrent(), page.getSize());
        }

        log.info("开始查询用户收藏作品，用户 ID: {}, 页码: {}, 每页大小: {}, 排序: {}",
            userId, page.getCurrent(), page.getSize(), orderBy);

        // 调用 Mapper 层查询
        IPage<Works> result = starsMapper.selectUserStarredWorks(page, userId, orderBy);

        log.info("查询用户收藏作品完成，用户 ID: {}, 总数: {}, 当前页: {}",
            userId, result.getTotal(), result.getCurrent());

        return result;
    }
}
