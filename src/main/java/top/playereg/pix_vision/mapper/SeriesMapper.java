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
}
