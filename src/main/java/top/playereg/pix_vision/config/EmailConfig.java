package top.playereg.pix_vision.config;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Base64;

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
     * @param code 验证码
     * @return 渲染后的完整 HTML 字符串
     * @author PlayerEG
     */
    public static String renderVerificationEmailTemplate(String code) {
        // 从 classpath 读取模板文件
        String template = ResourceUtil.readUtf8Str("template/email-verification.html");
            
        // 读取 logo 图片并转换为 Base64 - 浅色 logo
        byte[] lightLogoBytes = ResourceUtil.readBytes("template/logo/light.png"); // logo 图片路径
        String base64LightLogo = Base64.getEncoder().encodeToString(lightLogoBytes);
        String logoDataUri = "data:image/png;base64," + base64LightLogo;

        // 读取 logo 图片并转换为 Base64 - 深色 logo
        byte[] darkLogoBytes = ResourceUtil.readBytes("template/logo/dark.png");
        String base64DarkLogo = Base64.getEncoder().encodeToString(darkLogoBytes);
        String darkLogoDataUri = "data:image/png;base64," + base64DarkLogo;
            
        // 替换模板中的占位符 {{code}}, {{expireTime}}, {{year}}, {{logoUri}}
        String result = template.replace("{{code}}", code);
        result = result.replace("{{expireTime}}", "5");
        result = result.replace("{{year}}", String.valueOf(DateUtil.thisYear()));
        result = result.replace("{{systemName}}", "Pixie Vision");
        result = result.replace("{{logoUriLight}}", logoDataUri);
        result = result.replace("{{logoUriDark}}", darkLogoDataUri);

        return result;
    }
}
