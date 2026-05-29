package top.playereg.pix_vision.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import top.playereg.pix_vision.pojo.VO.OperateLogVO;
import top.playereg.pix_vision.pojo.entity.OperateLog;

/**
 * 系统操作日志服务接口
 *
 * @author blue_sky_ks
 */
public interface OperateLogService {

    /**
     * 记录操作日志
     *
     * @param userId   用户 ID
     * @param logEvent 操作事件描述
     * @return 是否记录成功
     * @author blue_sky_ks
     */
    boolean recordLog(Integer userId, String logEvent);

    /**
     * 记录操作日志（使用实体对象）
     *
     * @param operateLog 操作日志实体
     * @return 是否记录成功
     * @author blue_sky_ks
     */
    boolean recordLog(OperateLog operateLog);

    /**
     * 分页查询操作日志（关联用户名，支持关键字过滤）
     *
     * @param current 当前页码
     * @param size    每页大小
     * @param keyword 关键字（可选，模糊匹配 log_event 和 username）
     * @param orderBy 排序方式：'oldest' 按最早，其他或 null 按最新（默认）
     * @return 分页日志视图列表
     * @author PlayerEG
     */
    IPage<OperateLogVO> getOperateLogsPage(Long current, Long size, String keyword, String orderBy);
}
