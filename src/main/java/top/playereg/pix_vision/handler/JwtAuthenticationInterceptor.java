package top.playereg.pix_vision.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import top.playereg.pix_vision.service.TokenWhitelistService;
import top.playereg.pix_vision.util.Annotation.PublicAccess;
import top.playereg.pix_vision.util.JWTUtils;

/**
 * JWT 认证拦截器
 * <p>
 * 验证请求中的 JWT Token，支持 @PublicAccess 注解标记的公开接口
 * </p>
 *
 * @author PlayerEG
 * @see PublicAccess
 */
@Component
@SuppressWarnings("all")
public class JwtAuthenticationInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationInterceptor.class);

    @Autowired
    private TokenWhitelistService tokenWhitelistService;

    /**
     * 在请求处理之前进行调用，用于验证 JWT Token
     *
     * @param request  HTTP 请求对象
     * @param response HTTP 响应对象
     * @param handler  被调用的处理器
     * @return true-继续执行，false-中断请求
     * @throws Exception 可能的异常
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 获取请求的 URI
        String requestURI = request.getRequestURI();
        String method = request.getMethod();

        // 打印请求日志（debug 级别，避免生产环境日志过多）
        log.debug("JWT 拦截请求: {} {}", method, requestURI);

        // OPTIONS 预检请求直接放行（CORS 跨域请求需要）
        if ("OPTIONS".equalsIgnoreCase(method)) {
            log.debug("OPTIONS 预检请求，跳过认证 - URI: {}", requestURI);
            return true;
        }

        // 检查是否是公开访问接口（基于 @PublicAccess 注解）
        if (isPublicAccess(handler)) {
            log.debug("公开接口，跳过认证 - URI: {}", requestURI);
            return true;
        }

        // 从 Header 中获取 Token
        String token = request.getHeader("Authorization");

        // 如果 Header 中没有 Token，尝试从参数中获取
        if (token == null || token.isEmpty()) {
            token = request.getParameter("token");
        }

        // 检查 Token 是否存在
        if (token == null || token.isEmpty()) {
            log.warn("Token 不存在 - URI: {}", requestURI);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"未授权访问：Token 不存在\",\"data\":null}");
            return false;
        }

        // 去除 "Bearer " 前缀（如果有）
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        // 检查 Token 是否在白名单中
        if (!tokenWhitelistService.isInWhitelist(token)) {
            log.warn("Token 不在白名单中 - URI: {}", requestURI);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"未授权访问：Token 未在白名单中，请重新登录\",\"data\":null}");
            return false;
        }

        // 验证 Token 是否有效
        if (!JWTUtils.verifyToken(token)) {
            log.warn("Token 验证失败 - URI: {}", requestURI);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"未授权访问：Token 无效或已过期\",\"data\":null}");
            return false;
        }

        // 检查 Token 是否过期
        if (JWTUtils.isTokenExpired(token)) {
            log.warn("Token 已过期 - URI: {}", requestURI);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"未授权访问：Token 已过期\",\"data\":null}");
            return false;
        }

        // 从 Token 中提取用户信息并存入 request 属性，方便后续使用
        Integer userId = JWTUtils.getUserIdFromToken(token);
        String username = JWTUtils.getUsernameFromToken(token);

        if (userId != null) {
            request.setAttribute("userId", userId);
        } else {
            log.warn("未能从 Token 中提取用户 ID");
        }
        if (username != null) {
            request.setAttribute("username", username);
        } else {
            log.warn("未能从 Token 中提取用户名");
        }

        log.info("验证通过 - {}:{}", username, userId);
        return true;
    }

    /**
     * 检查是否为公开访问接口（基于 @PublicAccess 注解）
     *
     * @param handler 处理器对象
     * @return true-公开接口，false-需要认证
     */
    private boolean isPublicAccess(Object handler) {
        // 只处理方法级别的请求
        if (!(handler instanceof HandlerMethod)) {
            return false;
        }

        HandlerMethod handlerMethod = (HandlerMethod) handler;

        // 优先检查方法级别的注解
        PublicAccess publicAccess = handlerMethod.getMethodAnnotation(PublicAccess.class);

        // 如果方法上没有注解，检查类级别的注解
        if (publicAccess == null) {
            publicAccess = handlerMethod.getBeanType().getAnnotation(PublicAccess.class);
        }

        return publicAccess != null;
    }
}
