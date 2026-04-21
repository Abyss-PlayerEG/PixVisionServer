package top.playereg.pix_vision.service.Impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.playereg.pix_vision.mapper.OperateLogMapper;
import top.playereg.pix_vision.pojo.OperateLog;
import top.playereg.pix_vision.service.OperateLogService;

import java.sql.Timestamp;

/**
 * 系统操作日志服务实现类
 *
 * @author blue_sky_ks
 */
@Service
public class OperateLogServiceImpl implements OperateLogService {

    private static final Logger log = LoggerFactory.getLogger(OperateLogServiceImpl.class);

    @Autowired
    private OperateLogMapper operateLogMapper;

    /**
     * 记录操作日志
     *
     * @param userId   用户 ID
     * @param logEvent 操作事件描述
     * @return 是否记录成功
     * @author blue_sky_ks
     */
    @Override
    public boolean recordLog(Integer userId, String logEvent) {
        // 参数校验
        if (userId == null || userId <= 0) {
            log.warn("用户 ID 无效: {}", userId);
            return false;
        }

        if (logEvent == null || logEvent.isEmpty()) {
            log.warn("操作事件描述为空");
            return false;
        }

        // 创建日志实体
        OperateLog operateLog = new OperateLog();
        operateLog.setUser_id(userId);
        operateLog.setLog_event(logEvent);
        operateLog.setLog_datetime(new Timestamp(System.currentTimeMillis()));

        // 调用重载方法
        return recordLog(operateLog);
    }

    /**
     * 记录操作日志（使用实体对象）
     *
     * @param operateLog 操作日志实体
     * @return 是否记录成功
     * @author blue_sky_ks
     */
    @Override
    public boolean recordLog(OperateLog operateLog) {
        // 参数校验
        if (operateLog == null) {
            log.error("操作日志实体为空");
            return false;
        }

        if (operateLog.getUser_id() == null || operateLog.getUser_id() <= 0) {
            log.warn("用户 ID 无效: {}", operateLog.getUser_id());
            return false;
        }

        if (operateLog.getLog_event() == null || operateLog.getLog_event().isEmpty()) {
            log.warn("操作事件描述为空");
            return false;
        }

        // 设置默认时间戳（如果未设置）
        if (operateLog.getLog_datetime() == null) {
            operateLog.setLog_datetime(new Timestamp(System.currentTimeMillis()));
        }

        try {
            // 插入数据库
            int result = operateLogMapper.insertOperateLog(operateLog);

            if (result > 0) {
                log.debug("操作日志记录成功 - 用户 ID: {}, 事件: {}",
                    operateLog.getUser_id(), operateLog.getLog_event());
                return true;
            } else {
                log.error("操作日志记录失败 - 用户 ID: {}, 事件: {}",
                    operateLog.getUser_id(), operateLog.getLog_event());
                return false;
            }
        } catch (Exception e) {
            log.error("操作日志记录异常 - 用户 ID: {}, 事件: {}, 错误: {}",
                operateLog.getUser_id(), operateLog.getLog_event(), e.getMessage());
            return false;
        }
    }
}
