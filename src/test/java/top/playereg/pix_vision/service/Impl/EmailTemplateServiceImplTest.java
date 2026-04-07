package top.playereg.pix_vision.service.Impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import top.playereg.pix_vision.service.EmailTemplateService;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 邮件模板服务测试类
 *
 * @author PlayerEG
 */
@Disabled
@SpringBootTest
class EmailTemplateServiceImplTest {

    @Autowired
    private EmailTemplateService emailTemplateService;

    @BeforeEach
    void setUp() {
        // 每次测试前清除缓存
        if (emailTemplateService instanceof EmailTemplateServiceImpl) {
            ((EmailTemplateServiceImpl) emailTemplateService).clearLogoCache();
        }
    }

    @Test
    void testRenderVerificationEmail() {
        // 测试渲染验证码邮件
        String code = "ABC123";
        String username = "test_user";
        String emailText = "注册验证";

        String html = emailTemplateService.renderVerificationEmail(code, username, emailText);

        // 验证返回结果不为空
        assertNotNull(html, "渲染结果不应为空");
        assertFalse(html.isEmpty(), "渲染结果不应为空字符串");

        // 验证占位符已被替换
        assertTrue(html.contains(code), "HTML 应包含验证码");
        assertTrue(html.contains(username), "HTML 应包含用户名");
        assertTrue(html.contains("注册验证"), "HTML 应包含邮件类型");
        assertTrue(html.contains("Pixie Vision"), "HTML 应包含系统名称");

        // 验证占位符已被完全替换（不应存在未替换的占位符）
        assertFalse(html.contains("{{username}}"), "不应存在未替换的用户名占位符");
        assertFalse(html.contains("{{code}}"), "不应存在未替换的验证码占位符");
        assertFalse(html.contains("{{systemName}}"), "不应存在未替换的系统名称占位符");
    }

    @Test
    void testRenderVerificationEmailWithNullValues() {
        // 测试空值处理
        String html = emailTemplateService.renderVerificationEmail("", "", "");

        assertNotNull(html, "即使参数为空，渲染结果也不应为空");
        // 应该用空字符串替换占位符
        assertFalse(html.contains("{{username}}"), "空用户名占位符应被替换为空字符串");
    }

    @Test
    void testRenderCustomTemplate() {
        // 测试自定义模板渲染（如果存在其他模板）
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("{{title}}", "测试标题");
        placeholders.put("{{content}}", "测试内容");

        // custom-template.html 不存在，期望抛出异常
        assertThrows(RuntimeException.class, () -> {
            emailTemplateService.renderTemplate("custom-template", placeholders);
        }, "不存在的模板应抛出异常");
    }

    @Test
    void testLogoCaching() {
        // 测试 Logo 缓存功能
        EmailTemplateServiceImpl serviceImpl = (EmailTemplateServiceImpl) emailTemplateService;

        // 第一次调用 - 应该加载并缓存
        String html1 = emailTemplateService.renderVerificationEmail("123456", "user1", "登录验证");
        assertNotNull(html1);

        // 第二次调用 - 应该使用缓存
        String html2 = emailTemplateService.renderVerificationEmail("654321", "user2", "改密");
        assertNotNull(html2);

        // 两次都应该成功渲染
        assertTrue(html1.contains("user1"));
        assertTrue(html2.contains("user2"));
    }

    @Test
    void testClearLogoCache() {
        // 测试清除缓存功能
        EmailTemplateServiceImpl serviceImpl = (EmailTemplateServiceImpl) emailTemplateService;

        // 先渲染一次以填充缓存
        emailTemplateService.renderVerificationEmail("111111", "user", "注册验证");

        // 清除缓存
        serviceImpl.clearLogoCache();

        // 再次渲染 - 应该重新加载
        String html = emailTemplateService.renderVerificationEmail("222222", "user", "登录验证");
        assertNotNull(html);
    }

    @Test
    void testMultipleEmailTypes() {
        // 测试不同类型的邮件内容
        String[] emailTypes = {"注册验证", "登录验证", "密码修改"};

        for (String emailType : emailTypes) {
            String html = emailTemplateService.renderVerificationEmail(
                    "TEST01",
                    "test_user",
                    emailType
            );

            assertNotNull(html, emailType + " 类型的邮件渲染失败");
            assertTrue(html.contains(emailType),
                    emailType + " 类型的邮件内容不正确");
        }
    }
}
