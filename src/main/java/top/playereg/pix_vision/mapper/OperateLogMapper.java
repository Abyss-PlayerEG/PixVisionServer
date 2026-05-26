package top.playereg.pix_vision.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;
import top.playereg.pix_vision.pojo.OperateLog;
import top.playereg.pix_vision.pojo.OperateLogVO;

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

    /**
     * 分页查询操作日志（关联用户名，支持关键字过滤）
     *
     * @param page    分页参数
     * @param keyword 关键字（可选，模糊匹配 log_event 和 username）
     * @param orderBy 排序方式：'oldest' 按最早，其他或 null 按最新（默认）
     * @return 分页日志视图列表
     * @author PlayerEG
     */
    IPage<OperateLogVO> selectOperateLogsPage(IPage<?> page,
                                               @Param("keyword") String keyword,
                                               @Param("orderBy") String orderBy);
}
