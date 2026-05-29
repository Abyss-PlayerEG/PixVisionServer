package top.playereg.pix_vision.service.Impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import top.playereg.pix_vision.mapper.OperateLogMapper;
import top.playereg.pix_vision.mapper.UserMapper;
import top.playereg.pix_vision.pojo.VO.OperateLogVO;
import top.playereg.pix_vision.pojo.entity.OperateLog;
import top.playereg.pix_vision.pojo.entity.user.User;
import top.playereg.pix_vision.service.OperateLogService;
import top.playereg.pix_vision.util.PixVisionLogger;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 系统操作日志服务实现类
 *
 * @author blue_sky_ks
 */
@Service
public class OperateLogServiceImpl implements OperateLogService {

    private static final PixVisionLogger log = PixVisionLogger.create(OperateLogServiceImpl.class);

    /**
     * Redis Key 前缀：用户 ID 到用户名的映射缓存
     */
    private static final String USERNAME_CACHE_PREFIX = "pix:sys:username:";

    /**
     * 用户名缓存过期时间：1 小时
     */
    private static final long USERNAME_CACHE_TTL_HOURS = 1;

    @Autowired
    private OperateLogMapper operateLogMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

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

    /**
     * 分页查询操作日志（用户名通过 Redis 缓存 + DB 回源解析）
     * <p>
     * 查询 tb_sys_logs 后，对每条日志的 user_id 优先从 Redis 获取用户名缓存；
     * 缓存未命中时查询数据库并将结果缓存 1 小时。即使用户被删除，
     * Redis 中的缓存仍可保留历史用户名信息。
     * </p>
     *
     * @param current 当前页码
     * @param size    每页大小
     * @param keyword 关键字（可选，模糊匹配 log_event）
     * @param orderBy 排序方式：'oldest' 按最早，其他或 null 按最新（默认）
     * @return 分页日志视图列表
     * @author PlayerEG
     */
    @Override
    public IPage<OperateLogVO> getOperateLogsPage(Long current, Long size, String keyword, String orderBy) {
        Page<OperateLogVO> page = new Page<>(current, size);
        IPage<OperateLogVO> result = operateLogMapper.selectOperateLogsPage(page, keyword, orderBy);

        // 解析用户名并填充到每条日志记录中
        if (result != null && !result.getRecords().isEmpty()) {
            resolveUsernames(result.getRecords());
        }

        return result;
    }

    /**
     * 批量解析用户名（Redis 缓存优先，DB 回源兜底）
     * <p>
     * 对日志列表中的所有 user_id 去重后统一查询，减少 Redis/DB 调用次数。
     * 解析结果通过 Redis 缓存 1 小时，用户删除后缓存仍可保留历史用户名。
     * </p>
     *
     * @param records 日志视图列表
     * @author PlayerEG
     */
    private void resolveUsernames(List<OperateLogVO> records) {
        // 收集所有唯一的用户 ID
        List<Integer> userIds = records.stream()
            .map(OperateLogVO::getUser_id)
            .filter(id -> id != null && id > 0)
            .distinct()
            .collect(Collectors.toList());

        if (userIds.isEmpty()) {
            return;
        }

        // 按 user_id 批量缓存/查询用户名
        Map<Integer, String> usernameMap = new HashMap<>();
        for (Integer userId : userIds) {
            String username = getUsernameFromCacheOrDb(userId);
            if (username != null) {
                usernameMap.put(userId, username);
            }
        }

        // 填充用户名到日志记录
        for (OperateLogVO record : records) {
            String username = usernameMap.get(record.getUser_id());
            if (username != null) {
                record.setUsername(username);
            }
        }
    }

    /**
     * 从 Redis 缓存或数据库获取用户名
     * <p>
     * 优先查询 Redis 缓存，未命中时查询数据库并将结果缓存。
     * 即使数据库中用户已被删除（查询返回 null），也不会覆盖已有的 Redis 缓存，
     * 确保历史日志中仍能展示当时的用户名。
     * </p>
     *
     * @param userId 用户 ID
     * @return 用户名，如果缓存和数据库均无记录则返回 null
     * @author PlayerEG
     */
    private String getUsernameFromCacheOrDb(Integer userId) {
        String cacheKey = USERNAME_CACHE_PREFIX + userId;

        // 优先从 Redis 获取
        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                return cached.toString();
            }
        } catch (Exception e) {
            log.warn("从 Redis 获取用户名缓存失败，用户 ID: {}, 错误: {}", userId, e.getMessage());
        }

        // 缓存未命中，查询数据库
        try {
            User user = userMapper.selectUserRoleById(userId);
            if (user != null && user.getUsername() != null) {
                String username = user.getUsername();
                // 缓存到 Redis，有效期 1 小时
                try {
                    redisTemplate.opsForValue().set(cacheKey, username, USERNAME_CACHE_TTL_HOURS, TimeUnit.HOURS);
                } catch (Exception e) {
                    log.warn("缓存用户名到 Redis 失败，用户 ID: {}, 错误: {}", userId, e.getMessage());
                }
                return username;
            }
        } catch (Exception e) {
            log.warn("从数据库查询用户名失败，用户 ID: {}, 错误: {}", userId, e.getMessage());
        }

        return null;
    }
}
