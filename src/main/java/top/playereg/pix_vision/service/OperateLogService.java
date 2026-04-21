package top.playereg.pix_vision.service;

import top.playereg.pix_vision.pojo.OperateLog;

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
}
