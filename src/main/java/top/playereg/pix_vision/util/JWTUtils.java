package top.playereg.pix_vision.util;

import cn.hutool.jwt.JWT;
import cn.hutool.jwt.JWTUtil;
import cn.hutool.jwt.JWTValidator;
import cn.hutool.jwt.signers.JWTSigner;
import cn.hutool.jwt.signers.JWTSignerUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

/**
 * JWT 工具类 - 基于 Hutool
 * 
 * @author PlayerEG
 */
@Component
public class JWTUtils {
    private static final Logger log = LoggerFactory.getLogger(JWTUtils.class);
    
    /**
     * JWT 密钥 - 从配置文件读取会更安全
     * 建议在生产环境中使用更复杂的密钥并存储在配置文件中
     */
    private static final String SECRET_KEY = "PixVision_Secret_Key_2026_SpringBoot3_JWT_Auth";
    
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
     * 
     * @param payload 载荷数据，包含用户信息等
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
     * 
     * @param userId 用户 ID
     * @param username 用户名
     * @return 生成的 JWT 字符串
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
     * 
     * @param token JWT 字符串
     * @return true-有效，false-无效
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
}
