package top.playereg.pix_vision.config;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.StrUtil;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import top.playereg.pix_vision.util.ImageUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * 邮件配置类
 *
 * @author PlayerEG
 */
@Data
@Component
@ConfigurationProperties(prefix = "spring.mail")
public class EmailConfig {
    private String host;
    private Integer port;
    private String from;
    private String username;
    private String password;
    private String protocol;
    private String defaultEncoding = "UTF-8";

    // 自定义属性 - 从 properties.mail.smtp.ssl.enable 读取
    private boolean sslEnable = true;

    // 自定义属性 - 从 properties.mail.smtp.starttls.enable 读取
    private boolean starttlsEnable = false;

    /**
     * 渲染邮箱验证码 HTML 模板
     *
     * @param code      验证码
     * @param username  用户名
     * @param emailText 邮件内容类型
     * @return 渲染后的完整 HTML 字符串
     * @author PlayerEG
     */
    public static String renderVerificationEmailTemplate(String code, String username, String emailText) {
        // 构建占位符映射
        Map<String, String> placeholders = buildPlaceholders(code, username, emailText);
        
        // 读取模板文件
        String template = loadTemplate();
        
        // 执行占位符替换
        return replacePlaceholders(template, placeholders);
    }

    /**
     * 构建占位符映射表
     *
     * @param code      验证码
     * @param username  用户名
     * @param emailText 邮件内容类型
     * @return 占位符映射表
     * @author PlayerEG
     */
    private static Map<String, String> buildPlaceholders(String code, String username, String emailText) {
        Map<String, String> placeholders = new HashMap<>();
        
        // 基础占位符
        placeholders.put("{{username}}", username);
        placeholders.put("{{email_text}}", emailText);
        placeholders.put("{{code}}", code);
        placeholders.put("{{expireTime}}", "5");
        placeholders.put("{{year}}", String.valueOf(DateUtil.thisYear()));
        placeholders.put("{{systemName}}", "Pixie Vision");
        
        // Logo 占位符（Base64 编码）
        placeholders.put("{{logoUriLight}}", loadLogoBase64("light.png"));
        placeholders.put("{{logoUriDark}}", loadLogoBase64("dark.png"));
        
        return placeholders;
    }

    /**
     * 加载模板文件
     *
     * @return 模板内容字符串
     * @author PlayerEG
     */
    private static String loadTemplate() {
        String templatePath = StrUtil.format(
                "{}/email-verification.html",
                FilePathConfig.EmailHtmlPath
        );
        
        try {
            return ResourceUtil.readUtf8Str(templatePath);
        } catch (Exception e) {
            throw new RuntimeException("加载邮件模板失败: " + templatePath, e);
        }
    }

    /**
     * 加载 Logo 图片并转换为 Base64
     *
     * @param logoFileName Logo 文件名
     * @return Base64 编码的 Data URI
     * @author PlayerEG
     */
    private static String loadLogoBase64(String logoFileName) {
        String logoPath = StrUtil.format("{}/{}", FilePathConfig.LogoPath, logoFileName);
        
        try {
            return ImageUtils.imageToBase64(logoPath);
        } catch (Exception e) {
            throw new RuntimeException("加载 Logo 图片失败: " + logoPath, e);
        }
    }

    /**
     * 执行占位符替换
     *
     * @param template     模板字符串
     * @param placeholders 占位符映射表
     * @return 替换后的字符串
     * @author PlayerEG
     */
    private static String replacePlaceholders(String template, Map<String, String> placeholders) {
        String result = template;
        
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            String placeholder = entry.getKey();
            String value = entry.getValue();
            
            if (value != null) {
                result = result.replace(placeholder, value);
            }
        }
        
        return result;
    }
}
