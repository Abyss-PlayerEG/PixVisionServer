package top.playereg.pix_vision.service.Impl;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.junit4.SpringRunner;
import top.playereg.pix_vision.service.VerificationCodeServices;

import static org.junit.Assert.*;

/**
 * 验证码服务测试类
 * 
 * @author PlayerEG
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class VerificationCodeServicesImplTest {

    @Autowired
    private VerificationCodeServices verificationCodeServices;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 测试验证码生成
     * 验证生成的验证码长度和格式是否正确
     */
    @Test
    public void verificationCode() {
        // 生成验证码
        String code = verificationCodeServices.verificationCode();
        
        // 断言：验证码不为空
        assertNotNull("生成的验证码不能为空", code);
        
        // 断言：验证码长度为 6 位
        assertEquals("验证码长度应为 6 位", 6, code.length());
        
        // 断言：验证码只包含数字和大写字母
        assertTrue("验证码应只包含数字和大写字母", code.matches("^[0-9A-Z]{6}$"));
        
        System.out.println("生成的验证码：" + code);
    }

    /**
     * 测试设置验证码缓存
     * 验证是否能正确将验证码存储到 Redis
     */
    @Test
    public void setRedisVCode() {
        String testEmail = "test@example.com";
        String testCode = "123456";
        
        // 设置验证码
        verificationCodeServices.setRedisVCode(testEmail, testCode);
        
        // 从 Redis 中获取验证码
        String key = "userEmailCode:" + testEmail;
        String storedCode = (String) redisTemplate.opsForValue().get(key);
        
        // 断言：存储的验证码与设置的一致
        assertNotNull("Redis 中应存在验证码", storedCode);
        assertEquals("存储的验证码应与设置的验证码一致", testCode, storedCode);
        
        System.out.println("测试邮箱：" + testEmail);
        System.out.println("设置的验证码：" + testCode);
        System.out.println("Redis 中存储的验证码：" + storedCode);
    }

    /**
     * 测试删除验证码缓存
     * 验证是否能正确删除 Redis 中的验证码
     */
    @Test
    public void deleteRedisVCode() {
        String testEmail = "test@example.com";
        String testCode = "123456";
        String key = "userEmailCode:" + testEmail;
        
        // 先设置验证码
        verificationCodeServices.setRedisVCode(testEmail, testCode);
        
        // 确认验证码已存储
        String storedCode = (String) redisTemplate.opsForValue().get(key);
        assertNotNull("验证码应该已存储", storedCode);
        
        // 删除验证码
        verificationCodeServices.deleteRedisVCode(testEmail);
        
        // 断言：验证码已被删除
        String deletedCode = (String) redisTemplate.opsForValue().get(key);
        assertNull("验证码应该已被删除", deletedCode);
        
        System.out.println("测试删除验证码 - 邮箱：" + testEmail);
        System.out.println("删除前验证码：" + storedCode);
        System.out.println("删除后验证码：" + deletedCode);
    }

    /**
     * 测试验证码验证功能
     * 验证验证码验证逻辑是否正确
     */
    @Test
    public void verificationCodeVerify() {
        String testEmail = "test@example.com";
        String testCode = "123456";
        
        // 验证码验证
        boolean verificationStatus = verificationCodeServices.verificationCodeVerify(testEmail, testCode);

        // 断言：验证码验证成功
        assertTrue("验证码验证成功", verificationStatus);

        System.out.println("测试验证码验证 - 邮箱：" + testEmail);
        System.out.println("验证结果：" + verificationStatus);
    }
}