package top.playereg.pix_vision.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import top.playereg.pix_vision.pojo.Works;

/**
 * 作品服务接口
 *
 * @author PlayerEG
 */
public interface WorkService {

    /**
     * 分页查询首页作品列表
     *
     * @param page 分页对象
     * @return 分页结果
     * @author PlayerEG
     */
    IPage<Works> selectHomepageWorks(Page<Works> page);

    /**
     * 批量删除作品（支持单条和批量删除）
     *
     * @param workIds 要删除的作品 ID 列表
     * @param userId  当前用户 ID（用于权限验证）
     * @return 删除结果
     * @author PlayerEG
     */
    Boolean batchDeleteWorks(java.util.List<Integer> workIds, Integer userId);
}
