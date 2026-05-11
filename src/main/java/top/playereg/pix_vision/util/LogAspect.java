package top.playereg.pix_vision.util;

import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import top.playereg.pix_vision.service.OperateLogService;
import top.playereg.pix_vision.util.Annotation.LogRecord;

/**
 * 操作日志记录切面
 * <p>
 * 基于 AOP 实现的用户操作日志自动记录功能。
 * 通过 @LogRecord 注解标记需要记录的方法，自动捕获用户 ID、IP 地址和操作事件。
 * </p>
 *
 * <h3>使用场景</h3>
 * <ol>
 *   <li>记录用户敏感操作（修改密码、删除数据）</li>
 *   <li>审计管理员行为（封禁用户、审核内容）</li>
 *   <li>追踪系统关键业务流程</li>
 * </ol>
 *
 * <h3>注意事项</h3>
 * <ul>
 *   <li>仅记录方法执行成功的操作，异常不会记录</li>
 *   <li>日志记录失败不会影响业务逻辑执行</li>
 *   <li>优先从 request attribute 获取用户 ID，其次从 Token 解析</li>
 *   <li>未获取到有效用户 ID 时会跳过日志记录</li>
 * </ul>
 *
 * @author blue_sky_ks, PlayerEG
 * @see top.playereg.pix_vision.util.Annotation.LogRecord 日志记录注解
 * @see top.playereg.pix_vision.service.OperateLogService 操作日志服务
 * @since DEV-2.0.0
 */
@Component
@Aspect
@SuppressWarnings("unused")
public class LogAspect {

    private static final PixVisionLogger log = PixVisionLogger.create(LogAspect.class);

    @Autowired
    private OperateLogService operateLogService;

    /**
     * 环绕通知 - 记录用户操作日志
     *
     * @param pjp       切点
     * @param logRecord 注解
     * @return 返回结果
     * @throws Throwable 抛出异常
     * @author blue_sky_ks
     */
    @Around("@annotation(logRecord)")
    public Object record(ProceedingJoinPoint pjp, LogRecord logRecord) throws Throwable {
        // 执行目标方法
        Object result = pjp.proceed();

        try {
            // 获取当前请求
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) {
                log.warn("无法获取请求上下文，跳过日志记录");
                return result;
            }

            HttpServletRequest request = attributes.getRequest();

            // 从 Token 中获取用户 ID
            Integer userId = extractUserIdFromRequest(request);
            if (userId == null || userId <= 0) {
                log.debug("未获取到有效的用户 ID，跳过日志记录");
                return result;
            }

            // 构建操作事件描述
            String module = logRecord.module();
            String event = logRecord.event();
            String logEvent = buildLogEvent(module, event);

            // 记录日志到数据库
            boolean success = operateLogService.recordLog(userId, logEvent);
            if (!success) {
                log.warn("操作日志记录失败 - 用户 ID: {}, 事件: {}", userId, logEvent);
            }

        } catch (Exception e) {
            // 日志记录失败不应影响业务逻辑，仅记录错误
            log.error("日志记录过程中发生异常: {}", e.getMessage(), e);
        }

        return result;
    }

    /**
     * 从请求中提取用户 ID
     *
     * @param request HTTP 请求对象
     * @return 用户 ID，如果无法提取则返回 null
     * @author blue_sky_ks
     */
    private Integer extractUserIdFromRequest(HttpServletRequest request) {
        // 优先从 request attribute 中获取（由拦截器设置）
        Integer userId = (Integer) request.getAttribute("userId");
        if (userId != null && userId > 0) {
            return userId;
        }

        // 如果 attribute 中没有，尝试从 Token 中解析
        String token = JWTUtils.extractToken(request);
        if (token != null && !token.isEmpty()) {
            return JWTUtils.getUserIdFromToken(token);
        }

        return null;
    }

    /**
     * 构建操作事件描述
     *
     * @param module 模块名称
     * @param event  事件描述
     * @return 完整的操作事件描述
     * @author blue_sky_ks
     */
    private String buildLogEvent(String module, String event) {
        StringBuilder sb = new StringBuilder();
        if (module != null && !module.isEmpty()) {
            sb.append("[").append(module).append("] ");
        }
        if (event != null && !event.isEmpty()) {
            sb.append(event);
        } else {
            sb.append("未知操作");
        }
        return sb.toString();
    }
}
