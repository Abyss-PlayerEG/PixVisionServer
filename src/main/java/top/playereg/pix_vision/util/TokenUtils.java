package top.playereg.pix_vision.util;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Token 提取工具类
 * 从 HTTP 请求中提取 JWT Token（支持 Header 和 URL 参数两种方式）
 *
 * @author PlayerEG
 */
public class TokenUtils {
    private static final Logger log = LoggerFactory.getLogger(TokenUtils.class);

    /**
     * 从 HTTP 请求中提取 Token
     * 优先从 URL 参数获取，如果没有则从 Authorization Header 获取
     *
     * @param request HTTP 请求对象
     * @return Token 字符串，如果不存在则返回 null
     */
    public static String extractToken(HttpServletRequest request) {
        // 优先从 URL 参数获取 Token
        String token = request.getParameter("token");

        // 如果 URL 参数中没有，尝试从 Header 获取
        if (token == null || token.isEmpty()) {
            String authHeader = request.getHeader("Authorization");
            log.debug("从 Authorization Header 获取 Token: {}", authHeader != null ? "存在" : "不存在");

            if (authHeader != null && !authHeader.isEmpty()) {
                // 支持两种格式：带 Bearer 前缀 或 不带前缀
                if (authHeader.startsWith("Bearer ")) {
                    token = authHeader.substring(7); // 去除 "Bearer " 前缀
                    log.debug("去除 Bearer 前缀后的 Token");
                } else {
                    token = authHeader; // 直接使用
                    log.debug("直接使用 Authorization Header 作为 Token");
                }
            }
        }

        return token;
    }

    /**
     * 从 HTTP 请求中提取 Token（带日志）
     *
     * @param request   HTTP 请求对象
     * @param operation 操作名称（用于日志）
     * @return Token 字符串，如果不存在则返回 null
     */
    public static String extractTokenWithLog(HttpServletRequest request, String operation) {
        String token = extractToken(request);

        if (token != null && !token.isEmpty()) {
            String maskedToken = token.length() > 10 ? token.substring(0, 10) + "..." : token;
            log.debug("{} - 提取的 Token: {}", operation, maskedToken);
        } else {
            log.warn("{} - Token 不存在", operation);
        }

        return token;
    }
}
