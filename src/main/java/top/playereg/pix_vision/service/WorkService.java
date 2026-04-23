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
}
