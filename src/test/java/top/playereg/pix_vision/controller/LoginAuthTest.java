package top.playereg.pix_vision.controller;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import top.playereg.pix_vision.pojo.ResponsePojo;
import top.playereg.pix_vision.pojo.UserLogin;
import top.playereg.pix_vision.util.JWTUtils;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 登录鉴权功能测试
 * 
 * @author PlayerEG
 */
@SpringBootTest
class LoginAuthTest {
    
    private static final Logger log = LoggerFactory.getLogger(LoginAuthTest.class);
    
    @Autowired
    private UserController userController;
    
    /**
     * 测试 JWT Token 生成和验证
     */
    @Test
    void testJwtToken() {
        // 生成 Token
        String token = JWTUtils.createToken(1, "test_user");
        assertNotNull(token, "Token 不应为空");
        log.info("生成的 Token: {}", token);
        
        // 验证 Token
        assertTrue(JWTUtils.verifyToken(token), "Token 验证应该通过");
        
        // 获取用户信息
        assertEquals(1, JWTUtils.getUserIdFromToken(token));
        assertEquals("test_user", JWTUtils.getUsernameFromToken(token));
        
        log.info("JWT Token 测试通过");
    }
    
    /**
     * 测试登录接口（需要先注册）
     * 注意：此测试需要数据库中已有用户数据
     */
    @Test
    void testLogin() {
        // 测试数据
        String username = "dev_user";
        String password = "123456";
        String vCode = "ABCDEF"; // 需要使用正确的验证码
        
        // 调用登录接口
        ResponsePojo<UserLogin> response = userController.login(username, password, vCode);
        
        // 验证响应
        assertNotNull(response);
        assertEquals(200, response.getRecode());
        assertNotNull(response.getData());
        assertNotNull(response.getData().getToken());
        
        log.info("登录成功，Token: {}", response.getData().getToken());
    }
}
