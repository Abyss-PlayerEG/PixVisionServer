package top.playereg.pix_vision.util;

import cn.hutool.jwt.JWT;
import cn.hutool.jwt.JWTUtil;
import cn.hutool.jwt.JWTValidator;
import cn.hutool.jwt.signers.JWTSigner;
import cn.hutool.jwt.signers.JWTSignerUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import top.playereg.pix_vision.config.SecureConfig;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

/**
 * JWT 工具类
 * <p>
 * 基于 Hutool 实现的 JWT Token 生成、验证和解析工具。
 * 支持用户 ID 和用户名的编码解码，Token 有效期为 7 天。
 * </p>
 *
 * <h3>使用场景</h3>
 * <ol>
 *   <li>用户登录成功后生成认证 Token</li>
 *   <li>拦截器中验证 Token 有效性</li>
 *   <li>从 Token 中提取用户身份信息</li>
 *   <li>检查 Token 是否过期或计算剩余有效期</li>
 * </ol>
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * // 示例1：生成 Token
 * String token = JWTUtils.createToken(userId, username);
 *
 * // 示例2：验证 Token
 * boolean isValid = JWTUtils.verifyToken(token);
 *
 * // 示例3：从 Token 获取用户 ID
 * Integer userId = JWTUtils.getUserIdFromToken(token);
 *
 * // 示例4：从 HTTP 请求提取 Token
 * String token = JWTUtils.extractToken(request);
 * }</pre>
 *
 * <h3>注意事项</h3>
 * <ul>
 *   <li>密钥从 SecureConfig 配置中读取，支持自定义</li>
 *   <li>Token 包含 userId 和 username 两个核心字段</li>
 *   <li>验证失败不会抛出异常，而是返回 false 或 null</li>
 *   <li>支持从 URL 参数或 Authorization Header 提取 Token</li>
 *   <li>Authorization Header 支持带 Bearer 前缀或不带前缀的格式</li>
 * </ul>
 *
 * @author PlayerEG
 * @see top.playereg.pix_vision.config.SecureConfig 安全配置
 * @since DEV-2.0.0
 */
@Component
public class JWTUtils {
    private static final PixVisionLogger log = PixVisionLogger.create(JWTUtils.class);

    /**
     * JWT 密钥 - 从配置文件中读取
     * 可在用户目录下的配置文件中自定义（优先级更高）
     */
    private static final String SECRET_KEY = SecureConfig.getJwtSecret();

    /**
     * Token 有效期（毫秒）- 7 天
     */
    private static final long TOKEN_EXPIRE_TIME = 7 * 24 * 60 * 60 * 1000L;

    /**
     * 创建 JWT 签名器
     *
     * @return JWTSigner 签名器实例
     * @author PlayerEG
     */
    private static JWTSigner getSigner() {
        return JWTSignerUtil.hs256(SECRET_KEY.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成 JWT Token
     * <p>
     * 根据提供的载荷数据生成包含标准声明（过期时间、签发时间、签发者）的 JWT Token。
     * Token 有效期为 7 天，使用 HS256 算法签名。
     * </p>
     *
     * @param payload 载荷数据 Map，包含用户信息等自定义字段
     * @return 生成的 JWT 字符串
     * @author PlayerEG
     */
    public static String createToken(Map<String, Object> payload) {
        // 创建 JWT Builder
        JWT jwt = JWT.create();

        // 添加标准声明
        jwt.setExpiresAt(new Date(System.currentTimeMillis() + TOKEN_EXPIRE_TIME)); // 过期时间
        jwt.setIssuedAt(new Date()); // 签发时间
        jwt.setIssuer("PixVisionServer"); // 签发者

        // 添加自定义载荷
        if (payload != null) {
            payload.forEach(jwt::setPayload);
        }

        // 签名并生成 Token
        String token = jwt.sign(getSigner());
        log.info("Token 生成成功，用户：{}", payload != null ? payload.get("username") : "unknown");
        return token;
    }

    /**
     * 生成 JWT Token（简化版）
     * <p>
     * 便捷方法，仅需提供用户 ID 和用户名即可生成 Token。
     * 内部自动构建包含 userId 和 username 的载荷数据。
     * </p>
     *
     * @param userId   用户 ID（不能为 null）
     * @param username 用户名（不能为 null）
     * @return 生成的 JWT 字符串
     * @throws IllegalArgumentException 如果 userId 或 username 为 null
     * @author PlayerEG
     */
    public static String createToken(Integer userId, String username) {
        // 参数验证
        if (userId == null || username == null) {
            throw new IllegalArgumentException("用户 ID 和用户名不能为空");
        }

        Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("userId", userId);
        payload.put("username", username);
        return createToken(payload);
    }

    /**
     * 验证 JWT Token 是否有效
     * <p>
     * 验证内容包括：签名有效性、Token 是否过期。
     * 任何验证失败都会返回 false，不会抛出异常。
     * </p>
     *
     * @param token JWT 字符串
     * @return true-有效，false-无效（包括 null、空字符串、签名错误、已过期）
     * @author PlayerEG
     */
    public static boolean verifyToken(String token) {
        try {
            if (token == null || token.isEmpty()) {
                return false;
            }

            JWT jwt = JWTUtil.parseToken(token);
            boolean isValid = jwt.setKey(SECRET_KEY.getBytes(StandardCharsets.UTF_8)).verify();

            if (isValid) {
                // 验证过期时间
                JWTValidator validator = JWTValidator.of(jwt);
                validator.validateDate(); // 检查是否过期
                log.debug("Token 验证通过");
                return true;
            } else {
                log.warn("Token 签名验证失败");
                return false;
            }
        } catch (Exception e) {
            log.error("Token 验证失败：{}", e.getMessage());
            return false;
        }
    }

    /**
     * 从 Token 中获取 Payload 数据
     *
     * @param token JWT 字符串
     * @return Payload 载荷数据
     * @author PlayerEG
     */
    public static Map<String, Object> getTokenPayload(String token) {
        try {
            if (token == null || token.isEmpty()) {
                return null;
            }

            JWT jwt = JWTUtil.parseToken(token);
            return jwt.getPayloads();
        } catch (Exception e) {
            log.error("解析 Token 失败：{}", e.getMessage());
            return null;
        }
    }

    /**
     * 从 Token 中获取用户 ID
     *
     * @param token JWT 字符串
     * @return 用户 ID，如果解析失败则返回 null
     * @author PlayerEG
     */
    public static Integer getUserIdFromToken(String token) {
        Map<String, Object> payload = getTokenPayload(token);
        if (payload != null && payload.containsKey("userId")) {
            Object userIdObj = payload.get("userId");
            if (userIdObj instanceof Integer) {
                return (Integer) userIdObj;
            } else if (userIdObj instanceof Number) {
                return ((Number) userIdObj).intValue();
            } else if (userIdObj instanceof String) {
                try {
                    return Integer.parseInt((String) userIdObj);
                } catch (NumberFormatException e) {
                    log.error("用户 ID 格式错误：{}", userIdObj);
                }
            }
        }
        return null;
    }

    /**
     * 从 Token 中获取用户名
     *
     * @param token JWT 字符串
     * @return 用户名，如果解析失败则返回 null
     * @author PlayerEG
     */
    public static String getUsernameFromToken(String token) {
        Map<String, Object> payload = getTokenPayload(token);
        if (payload != null && payload.containsKey("username")) {
            return (String) payload.get("username");
        }
        return null;
    }

    /**
     * 检查 Token 是否已过期
     *
     * @param token JWT 字符串
     * @return true-已过期，false-未过期
     * @author PlayerEG
     */
    public static boolean isTokenExpired(String token) {
        try {
            if (token == null || token.isEmpty()) {
                return true;
            }

            JWT jwt = JWTUtil.parseToken(token);
            JWTValidator validator = JWTValidator.of(jwt);
            validator.validateDate();
            return false; // 未过期
        } catch (Exception e) {
            log.warn("Token 已过期或无效：{}", e.getMessage());
            return true; // 过期或无效
        }
    }

    /**
     * 获取 Token 的剩余有效期（毫秒）
     *
     * @param token JWT 字符串
     * @return 剩余毫秒数，如果 Token 无效则返回 0
     * @author PlayerEG
     */
    public static long getTokenRemainingTime(String token) {
        try {
            if (token == null || token.isEmpty()) {
                return 0;
            }

            JWT jwt = JWTUtil.parseToken(token);
            Object expObj = jwt.getPayload("exp");
            if (expObj instanceof Number) {
                long expireTime = ((Number) expObj).longValue() * 1000L; // 转换为毫秒
                long currentTime = System.currentTimeMillis();
                long remainingTime = expireTime - currentTime;
                return Math.max(0, remainingTime); // 确保不为负数
            }
            return 0;
        } catch (Exception e) {
            log.error("获取 Token 剩余时间失败：{}", e.getMessage());
            return 0;
        }
    }

    /**
     * 从 HTTP 请求中提取 Token
     * <p>
     * 优先从 URL 参数获取，如果没有则从 Authorization Header 获取。
     * 支持两种 Header 格式：带 "Bearer " 前缀或不带前缀。
     * </p>
     *
     * @param request HTTP 请求对象
     * @return Token 字符串，如果不存在则返回 null
     * @author PlayerEG
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
