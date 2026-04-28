package top.playereg.pix_vision.handler;

import cn.hutool.core.util.StrUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import top.playereg.pix_vision.pojo.userPojo.User;
import top.playereg.pix_vision.service.UserService;
import top.playereg.pix_vision.util.Annotation.RequireRole;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * 权限验证拦截器
 * <p>
 * 基于 @RequireRole 注解进行细粒度的角色权限控制
 * 需要在 JWT 认证拦截器之后执行
 * </p>
 *
 * @author PlayerEG
 * @see RequireRole
 * @see JwtAuthenticationInterceptor
 */
@Component
@SuppressWarnings("all")
public class PermissionInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(PermissionInterceptor.class);

    /**
     * Redis Key 前缀：role:{user_id}
     */
    private static final String ROLE_CACHE_PREFIX = "role:";

    /**
     * 缓存过期时间：60 秒
     */
    private static final long CACHE_EXPIRE_SECONDS = 24 * 60 * 60; // 24 小时

    private final UserService userService;
    private final RedisTemplate<String, Object> redisTemplate;

    public PermissionInterceptor(UserService userService, RedisTemplate<String, Object> redisTemplate) {
        this.userService = userService;
        this.redisTemplate = redisTemplate;
    }

    /**
     * 在请求处理之前进行权限验证
     *
     * @param request  HTTP 请求对象
     * @param response HTTP 响应对象
     * @param handler  被调用的处理器
     * @return true-继续执行，false-中断请求
     * @throws Exception 可能的异常
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 只处理方法级别的请求
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        HandlerMethod handlerMethod = (HandlerMethod) handler;

        // 优先检查方法级别的注解
        RequireRole requireRole = handlerMethod.getMethodAnnotation(RequireRole.class);

        // 如果方法上没有注解，检查类级别的注解
        if (requireRole == null) {
            requireRole = handlerMethod.getBeanType().getAnnotation(RequireRole.class);
        }

        // 没有权限注解，直接放行（默认所有已认证用户可访问）
        if (requireRole == null) {
            return true;
        }

        // 获取用户 ID（由 JWT 认证拦截器设置）
        Integer userId = (Integer) request.getAttribute("userId");
        if (userId == null) {
            log.warn("权限验证失败 - 无法获取用户 ID, URI: {}", request.getRequestURI());
            sendForbiddenResponse(response, "无法获取用户信息，请重新登录");
            return false;
        }

        // 尝试从 Redis 缓存中获取用户角色
        User user = getUserFromCache(userId);

        // 缓存未命中，查询数据库
        if (user == null) {
            user = userService.selectUserRoleById(userId);
            if (user != null) {
                // 将用户角色信息存入缓存
                cacheUserRole(userId, user);
                log.debug("角色信息已缓存，用户 ID: {}", userId);
            }
        } else {
            log.debug("从缓存获取角色信息，用户 ID: {}", userId);
        }

        if (user == null) {
            log.warn("权限验证失败 - 用户不存在, 用户ID: {}, URI: {}", userId, request.getRequestURI());
            sendForbiddenResponse(response, "用户不存在");
            return false;
        }

        int userRole = user.getUser_role();
        int[] allowedRoles = requireRole.value();

        // 检查用户角色是否在允许列表中
        boolean hasPermission = checkPermission(userRole, allowedRoles, requireRole.allowHigher());

        if (!hasPermission) {
            log.warn("权限不足 - 用户ID:{}, 用户名:{}, 用户角色:{}, 需要角色:{}, URI:{}",
                userId, user.getUsername(), userRole, Arrays.toString(allowedRoles), request.getRequestURI());

            String message = StrUtil.format("权限不足，需要角色:{}", Arrays.toString(allowedRoles));
            sendForbiddenResponse(response, message);
            return false;
        }

        log.debug("权限验证通过 - 用户ID:{}, 用户名:{}, 角色:{}", userId, user.getUsername(), userRole);
        return true;
    }

    /**
     * 检查用户是否具有所需权限
     *
     * @param userRole     用户角色代码
     * @param allowedRoles 允许的角色代码列表
     * @param allowHigher  是否允许更高权限的角色
     * @return true-有权限，false-无权限
     */
    private boolean checkPermission(int userRole, int[] allowedRoles, boolean allowHigher) {
        for (int role : allowedRoles) {
            // 精确匹配
            if (userRole == role) return true;
            // 如果允许更高权限，检查用户角色是否大于当前角色
            if (allowHigher && userRole > role) return true;
        }
        return false;
    }

    /**
     * 从缓存中获取用户角色信息
     *
     * @param userId 用户 ID
     * @return 用户对象（包含 id, username, user_role），缓存未命中返回 null
     */
    private User getUserFromCache(Integer userId) {
        try {
            String key = ROLE_CACHE_PREFIX + userId;
            Object cachedUser = redisTemplate.opsForValue().get(key);

            if (cachedUser instanceof User) {
                return (User) cachedUser;
            }

            return null;
        } catch (Exception e) {
            log.error("从缓存获取用户角色失败，用户 ID: {}, 错误: {}", userId, e.getMessage());
            return null;
        }
    }

    /**
     * 将用户角色信息存入缓存
     *
     * @param userId 用户 ID
     * @param user   用户对象（包含 id, username, user_role）
     */
    private void cacheUserRole(Integer userId, User user) {
        try {
            String key = ROLE_CACHE_PREFIX + userId;
            redisTemplate.opsForValue().set(key, user, CACHE_EXPIRE_SECONDS, TimeUnit.SECONDS);
            log.debug("用户角色缓存成功，Key: {}, 过期时间: {}s", key, CACHE_EXPIRE_SECONDS);
        } catch (Exception e) {
            log.error("缓存用户角色失败，用户 ID: {}, 错误: {}", userId, e.getMessage());
        }
    }

    /**
     * 清除用户角色缓存
     * <p>
     * 当用户角色变更时调用此方法清除缓存
     * </p>
     *
     * @param userId 用户 ID
     */
    public void clearUserRoleCache(Integer userId) {
        try {
            String key = ROLE_CACHE_PREFIX + userId;
            Boolean deleted = redisTemplate.delete(key);
            if (deleted != null && deleted) {
                log.info("用户角色缓存已清除，用户 ID: {}", userId);
            }
        } catch (Exception e) {
            log.error("清除用户角色缓存失败，用户 ID: {}, 错误: {}", userId, e.getMessage());
        }
    }

    /**
     * 发送 403 禁止访问响应
     *
     * @param response HTTP 响应对象
     * @param message  错误消息
     */
    private void sendForbiddenResponse(HttpServletResponse response, String message) {
        try {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(
                StrUtil.format(
                    """
                        {
                            "code":403,
                            "message":"{}",
                            "data":null
                        }
                        """, message
                )
            );
        } catch (Exception e) {
            log.error("发送 403 响应失败: {}", e.getMessage());
        }
    }
}
