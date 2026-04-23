package top.playereg.pix_vision.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;
import top.playereg.pix_vision.pojo.Works;

/**
 * 作品数据访问层
 * <p>
 * 继承 MyBatis-Plus 的 BaseMapper，自动提供 CRUD 方法
 *
 * @author PlayerEG
 * @see Works
 */
@Mapper
@Repository
public interface WorksMapper extends BaseMapper<Works> {

    /**
     * 分页查询首页作品列表
     *
     * @param page 分页对象
     * @return 分页结果
     * @author PlayerEG
     */
    IPage<Works> selectHomepageWorks(Page<Works> page);

    /**
     * 批量逻辑删除作品（SQL 层面验证用户权限）
     *
     * @param workIds 作品 ID 列表
     * @param userId  用户 ID（确保只能删除自己的作品）
     * @return 影响的行数
     * @author PlayerEG
     */
    int batchDeleteWorks(java.util.List<Integer> workIds, Integer userId);
}
