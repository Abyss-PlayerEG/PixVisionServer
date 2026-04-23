package top.playereg.pix_vision.service.Impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.playereg.pix_vision.mapper.WorksMapper;
import top.playereg.pix_vision.pojo.Works;
import top.playereg.pix_vision.service.WorkService;

/**
 * 作品服务实现类
 *
 * @author PlayerEG
 */
@Service
public class WorkServiceImpl implements WorkService {

    @Autowired
    private WorksMapper worksMapper;

    /**
     * 分页查询首页作品列表
     *
     * @param page 分页对象
     * @return 分页结果
     * @author PlayerEG
     */
    @Override
    public IPage<Works> selectHomepageWorks(Page<Works> page) {
        return worksMapper.selectHomepageWorks(page);
    }
}
