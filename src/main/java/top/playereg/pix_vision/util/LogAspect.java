package top.playereg.pix_vision.util;

import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import top.playereg.pix_vision.service.OperateLogService;
import top.playereg.pix_vision.util.Annotation.LogRecord;

/**
 * 日志切面 - 用于记录用户操作到数据库
 *
 * @author blue_sky_ks
 */
@Component
@Aspect
@SuppressWarnings("unused")
public class LogAspect {

    private static final Logger logger = LoggerFactory.getLogger(LogAspect.class);

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
                logger.warn("无法获取请求上下文，跳过日志记录");
                return result;
            }

            HttpServletRequest request = attributes.getRequest();

            // 从 Token 中获取用户 ID
            Integer userId = extractUserIdFromRequest(request);
            if (userId == null || userId <= 0) {
                logger.debug("未获取到有效的用户 ID，跳过日志记录");
                return result;
            }

            // 构建操作事件描述
            String module = logRecord.module();
            String event = logRecord.event();
            String logEvent = buildLogEvent(module, event);

            // 记录日志到数据库
            boolean success = operateLogService.recordLog(userId, logEvent);
            if (!success) {
                logger.warn("操作日志记录失败 - 用户 ID: {}, 事件: {}", userId, logEvent);
            }

        } catch (Exception e) {
            // 日志记录失败不应影响业务逻辑，仅记录错误
            logger.error("日志记录过程中发生异常: {}", e.getMessage(), e);
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
