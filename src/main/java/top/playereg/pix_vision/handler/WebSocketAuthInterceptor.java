package top.playereg.pix_vision.handler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import top.playereg.pix_vision.service.TokenWhitelistService;
import top.playereg.pix_vision.util.JWTUtils;
import top.playereg.pix_vision.util.PixVisionLogger;

import java.util.Map;

/**
 * WebSocket 认证拦截器
 * <p>
 * 在 WebSocket 握手阶段验证 Token，将用户信息存入 WebSocketSession attributes。
 * </p>
 *
 * <h3>认证流程</h3>
 * <ol>
 *   <li>从 URL 参数中提取 token</li>
 *   <li>验证 Token 是否在白名单中</li>
 *   <li>验证 Token 是否有效且未过期</li>
 *   <li>从 Token 中提取用户信息并存入 session attributes</li>
 * </ol>
 *
 * <h3>使用场景</h3>
 * <ol>
 *   <li>WebSocket 连接认证</li>
 *   <li>用户身份识别</li>
 * </ol>
 *
 * @author PlayerEG
 * @since V4.0
 */
@Component
public class WebSocketAuthInterceptor implements HandshakeInterceptor {

    private static final PixVisionLogger log = PixVisionLogger.create(WebSocketAuthInterceptor.class);

    @Autowired
    private TokenWhitelistService tokenWhitelistService;

    /**
     * 握手前拦截，验证 Token
     *
     * @param request    HTTP 请求
     * @param response   HTTP 响应
     * @param wsHandler  WebSocket 处理器
     * @param attributes WebSocketSession attributes
     * @return true-允许握手，false-拒绝握手
     * @throws Exception 可能的异常
     */
    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        // 从 URL 参数中获取 token
        String token = null;
        if (request instanceof ServletServerHttpRequest servletRequest) {
            token = servletRequest.getServletRequest().getParameter("token");
        }

        // 检查 Token 是否存在
        if (token == null || token.isEmpty()) {
            log.warn("WebSocket 握手失败：Token 不存在");
            return false;
        }

        // 去除 "Bearer " 前缀（如果有）
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        // 检查 Token 是否在白名单中
        if (!tokenWhitelistService.isInWhitelist(token)) {
            log.warn("WebSocket 握手失败：Token 不在白名单中");
            return false;
        }

        // 验证 Token 是否有效
        if (!JWTUtils.verifyToken(token)) {
            log.warn("WebSocket 握手失败：Token 无效");
            return false;
        }

        // 检查 Token 是否过期
        if (JWTUtils.isTokenExpired(token)) {
            log.warn("WebSocket 握手失败：Token 已过期");
            return false;
        }

        // 从 Token 中提取用户信息
        Integer userId = JWTUtils.getUserIdFromToken(token);
        String username = JWTUtils.getUsernameFromToken(token);

        if (userId == null) {
            log.warn("WebSocket 握手失败：无法从 Token 中提取用户 ID");
            return false;
        }

        // 将用户信息存入 WebSocketSession attributes
        attributes.put("userId", userId);
        attributes.put("username", username);

        log.info("WebSocket 握手成功：用户 {} (ID: {})", username, userId);
        return true;
    }

    /**
     * 握手后回调（空实现）
     *
     * @param request   HTTP 请求
     * @param response  HTTP 响应
     * @param wsHandler WebSocket 处理器
     * @param exception 异常（如果有）
     */
    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // 空实现
    }
}
