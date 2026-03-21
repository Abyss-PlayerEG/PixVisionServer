package top.playereg.pix_vision.config;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.StrUtil;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

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
     * 渲染邮箱验证码HTML模板
     *
     * @param code 验证码
     * @return 渲染后的完整HTML字符串
     * @author PlayerEG
     */
    public static String renderVerificationEmailTemplate(String code) {
        // 从 classpath 读取模板文件
        String template = ResourceUtil.readUtf8Str("template/email-verification.html");
        // 替换模板中的占位符 {{code}}, {{expireTime}}, {{year}}
        String result = template.replace("{{code}}", code);
        result = result.replace("{{expireTime}}", "5");
        result = result.replace("{{year}}", String.valueOf(DateUtil.thisYear()));
        result = result.replace("{{systemName}}", "Pixie Vision");
        return result;
    }
}
