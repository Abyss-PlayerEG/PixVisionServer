package top.playereg.pix_vision.util;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JWT 工具类测试
 *
 * @author PlayerEG
 */
@SpringBootTest
class JWTUtilsTest {

    private static final Logger log = LoggerFactory.getLogger(JWTUtilsTest.class);

    /**
     * 测试生成 Token
     */
    @Test
    void testCreateToken() {
        // 准备测试数据
        Integer userId = 1;
        String username = "test_user";

        // 生成 Token
        String token = JWTUtils.createToken(userId, username);

        // 验证 Token 不为空
        assertNotNull(token, "生成的 Token 不应为空");
        log.info("生成的 Token: {}", token);

        // 验证 Token 格式（JWT 通常由三部分组成，用点分隔）
        assertTrue(token.contains("."), "Token 应该包含点号分隔符");
    }

    /**
     * 测试验证 Token
     */
    @Test
    void testVerifyToken() {
        // 生成一个有效的 Token
        String token = JWTUtils.createToken(1, "test_user");

        // 验证 Token
        boolean isValid = JWTUtils.verifyToken(token);

        // 断言验证结果
        assertTrue(isValid, "有效的 Token 应该通过验证");
        log.info("Token 验证结果：{}", isValid);
    }

    /**
     * 测试从 Token 中获取 Payload
     */
    @Disabled
    @Test
    void testGetTokenPayload() {
        // 生成 Token
        String token = JWTUtils.createToken(1, "test_user");

        // 获取 Payload
        Map<String, Object> payload = JWTUtils.getTokenPayload(token);

        // 验证 Payload 不为空
        assertNotNull(payload, "Payload 不应为空");

        // 验证 Payload 中包含用户信息
        assertEquals(1, payload.get("userId"), "userId 应该匹配");
        assertEquals("test_user", payload.get("username"), "username 应该匹配");

        log.info("Token Payload: {}", payload);
    }

    /**
     * 测试从 Token 中获取用户 ID
     */
    @Test
    void testGetUserIdFromToken() {
        // 生成 Token
        String token = JWTUtils.createToken(999, "test_user_id");

        // 获取用户 ID
        Integer userId = JWTUtils.getUserIdFromToken(token);

        // 验证用户 ID
        assertEquals(999, userId, "获取的用户 ID 应该与生成时的一致");
        log.info("从 Token 中获取的用户 ID: {}", userId);
    }

    /**
     * 测试从 Token 中获取用户名
     */
    @Test
    void testGetUsernameFromToken() {
        // 生成 Token
        String token = JWTUtils.createToken(1, "get_username_test");

        // 获取用户名
        String username = JWTUtils.getUsernameFromToken(token);

        // 验证用户名
        assertEquals("get_username_test", username, "获取的用户名应该与生成时的一致");
        log.info("从 Token 中获取的用户名：{}", username);
    }

    /**
     * 测试 Token 过期检测
     */
    @Test
    void testIsTokenExpired() {
        // 生成一个正常的 Token（7 天有效期）
        String token = JWTUtils.createToken(1, "expire_test");

        // 验证 Token 是否过期
        boolean isExpired = JWTUtils.isTokenExpired(token);

        // 新创建的 Token 不应该过期
        assertFalse(isExpired, "新生成的 Token 不应该过期");
        log.info("Token 是否过期：{}", isExpired);
    }

    /**
     * 测试无效 Token 验证
     */
    @Test
    void testInvalidToken() {
        // 使用无效的 Token
        String invalidToken = "invalid.token.here";

        // 验证应该失败
        boolean isValid = JWTUtils.verifyToken(invalidToken);
        assertFalse(isValid, "无效的 Token 不应该通过验证");

        // 获取 Payload 应该返回 null
        Map<String, Object> payload = JWTUtils.getTokenPayload(invalidToken);
        assertNull(payload, "无效 Token 的 Payload 应该为 null");

        log.info("无效 Token 验证通过测试");
    }

    /**
     * 测试空 Token
     */
    @Test
    void testEmptyToken() {
        // 测试空字符串
        assertFalse(JWTUtils.verifyToken(""), "空字符串不应该通过验证");
        assertFalse(JWTUtils.verifyToken(null), "null 不应该通过验证");

        log.info("空 Token 测试通过");
    }

    /**
     * 测试自定义 Payload 生成 Token
     */
    @Test
    void testCreateTokenWithCustomPayload() {
        // 创建自定义 Payload
        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", 100);
        payload.put("username", "custom_user");
        payload.put("role", "admin");
        payload.put("customField", "customValue");

        // 生成 Token
        String token = JWTUtils.createToken(payload);

        // 验证 Token
        assertTrue(JWTUtils.verifyToken(token), "带有自定义 Payload 的 Token 应该有效");

        // 获取 Payload
        Map<String, Object> resultPayload = JWTUtils.getTokenPayload(token);
        assertNotNull(resultPayload);
        
        // Hutool JWT 返回的数值类型可能是 NumberWithFormat，需要转换
        Object userIdObj = resultPayload.get("userId");
        assertNotNull(userIdObj);
        assertEquals(100, ((Number) userIdObj).intValue(), "userId 应该等于 100");
        
        assertEquals("admin", resultPayload.get("role"));
        assertEquals("customValue", resultPayload.get("customField"));

        log.info("自定义 Payload Token 测试通过");
    }
}
