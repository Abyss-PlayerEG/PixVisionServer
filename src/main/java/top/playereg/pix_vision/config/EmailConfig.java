package top.playereg.pix_vision.config;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.StrUtil;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import top.playereg.pix_vision.util.StrSwitchUtils;

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
        // 从 classpath 读取模板文件
        String template = ResourceUtil.readUtf8Str(
                StrUtil.format("{}/email-verification.html", FilePathConfig.EmailHtmlPath)
        );

        // 读取 logo 图片并转换为 Base64 - 浅色 logo
        String lightLogoDataUri = StrSwitchUtils.imageToBase64(StrUtil.format("{}/light.png", FilePathConfig.LogoPath));

        // 读取 logo 图片并转换为 Base64 - 深色 logo
        String darkLogoDataUri = StrSwitchUtils.imageToBase64(StrUtil.format("{}/dark.png", FilePathConfig.LogoPath));

        // 替换模板中的占位符
        String result = template.replace("{{username}}", username);
        result = result.replace("{{email_text}}", emailText);
        result = result.replace("{{code}}", code);
        result = result.replace("{{expireTime}}", "5");
        result = result.replace("{{year}}", String.valueOf(DateUtil.thisYear()));
        result = result.replace("{{systemName}}", "Pixie Vision");
        result = result.replace("{{logoUriLight}}", lightLogoDataUri);
        result = result.replace("{{logoUriDark}}", darkLogoDataUri);

        return result;
    }
}
