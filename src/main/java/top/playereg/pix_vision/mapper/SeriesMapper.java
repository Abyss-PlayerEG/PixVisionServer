package top.playereg.pix_vision.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import top.playereg.pix_vision.pojo.Series;

/**
 * 系列数据访问层
 * <p>
 * 继承 MyBatis-Plus 的 BaseMapper，自动提供 CRUD 方法
 *
 * @author PlayerEG
 * @see Series
 */
@Mapper
public interface SeriesMapper extends BaseMapper<Series> {

    /**
     * 新增作品系列
     *
     * @param series 系列对象（包含用户 ID、标题、描述等）
     * @return 影响的行数
     */
    int insertSeries(Series series);
}
