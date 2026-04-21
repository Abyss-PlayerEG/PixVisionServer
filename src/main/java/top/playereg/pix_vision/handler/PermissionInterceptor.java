package top.playereg.pix_vision.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import top.playereg.pix_vision.pojo.userPojo.User;
import top.playereg.pix_vision.service.UserService;
import top.playereg.pix_vision.util.Annotation.RequireRole;

import java.util.Arrays;

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
    
    private final UserService userService;
    
    public PermissionInterceptor(UserService userService) {
        this.userService = userService;
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
        
        // 查询用户信息获取角色
        User user = userService.selectAllUserById(userId);
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
            
            String message = String.format("权限不足，需要角色:%s", Arrays.toString(allowedRoles));
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
            if (userRole == role) {
                return true;
            }
            
            // 如果允许更高权限，检查用户角色是否大于当前角色
            if (allowHigher && userRole > role) {
                return true;
            }
        }
        return false;
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
            response.getWriter().write(String.format(
                "{\"code\":403,\"message\":\"%s\",\"data\":null}",
                message
            ));
        } catch (Exception e) {
            log.error("发送 403 响应失败: {}", e.getMessage());
        }
    }
}
