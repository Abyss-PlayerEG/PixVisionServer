package top.playereg.pix_vision.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;
import top.playereg.pix_vision.pojo.OperateLog;

/**
 * 系统操作日志 Mapper 接口
 *
 * @author blue_sky_ks
 */
@Mapper
@Repository
public interface OperateLogMapper extends BaseMapper<OperateLog> {

    /**
     * 插入操作日志记录
     *
     * @param operateLog 操作日志实体
     * @return 影响行数
     * @author blue_sky_ks
     */
    int insertOperateLog(OperateLog operateLog);
}
