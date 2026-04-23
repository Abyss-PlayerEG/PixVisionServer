package top.playereg.pix_vision.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
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
public interface WorksMapper extends BaseMapper<Works> {
}
