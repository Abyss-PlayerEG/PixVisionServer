package top.playereg.pix_vision.config;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import lombok.Data;
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
     * 生成验证码的HTML模板
     *
     * @param verificationCode 验证码
     * @return String
     * @author PlayerEG
     */
    public static String generateVerificationEmailHtml(String verificationCode) {
        // 定义邮件模板
        // 注意：邮箱客户端对CSS支持有限，尽量使用内联样式 (inline styles)
        String htmlTemplate = "<!DOCTYPE html>" +
                "<html lang='zh-CN'>" +
                "<head>" +
                "  <meta charset='UTF-8'>" +
                "  <meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                "  <meta http-equiv='X-UA-Compatible' content='IE=edge'>" +
                "  <title>邮箱验证码</title>" +
                "  <style type='text/css'>" +
                "    body { margin: 0; padding: 0; min-width: 100%; background-color: #f4f6f9; -webkit-text-size-adjust: 100%; -ms-text-size-adjust: 100%; }" +
                "    table { border-spacing: 0; border-collapse: collapse; }" +
                "    img { border: 0; }" +
                "    .wrapper { width: 100%; table-layout: fixed; background-color: #f4f6f9; padding-bottom: 60px; }" +
                "    .main-container { background-color: #ffffff; margin: 0 auto; width: 100%; max-width: 520px; border-radius: 12px; overflow: hidden; box-shadow: 0 8px 20px rgba(0,0,0,0.06); }" +
                "    .header-bar { height: 6px; background-color: #4A90E2; }" +
                "    .content-padding { padding: 40px 30px; }" +
                "    .code-box { background-color: #f0f7ff; border: 2px dashed #b3d4fc; border-radius: 8px; padding: 24px 10px; margin: 25px 0; text-align: center; }" +
                "    .code-text { font-family: 'Courier New', Courier, monospace; font-size: 36px; font-weight: 700; color: #2c3e50; letter-spacing: 10px; display: inline-block; }" +
                "    .footer-section { background-color: #fafbfc; border-top: 1px solid #eeeeee; padding: 25px 30px; text-align: center; }" +
                "    .text-secondary { color: #8898aa; font-size: 13px; line-height: 1.6; }" +
                "    .text-primary { color: #333333; font-size: 16px; line-height: 1.6; }" +
                "    h1 { margin: 0 0 15px; font-size: 24px; font-weight: 600; color: #1a202c; }" +
                "  </style>" +
                "</head>" +
                "<body>" +
                "  <center class='wrapper'>" +
                "    <table width='100%'>" +
                "      <tr>" +
                "        <td align='center'>" +
                "          <!-- 主卡片 -->" +
                "          <div class='main-container'>" +
                "            <!-- 顶部品牌色条 -->" +
                "            <div class='header-bar'></div>" +
                "            " +
                "            <!-- 内容区域 -->" +
                "            <table width='100%'>" +
                "              <tr>" +
                "                <td class='content-padding'>" +
                "                  <h1>邮箱地址验证</h1>" +
                "                  <p class='text-primary'>您好！</p>" +
                "                  <p class='text-primary'>您正在申请邮箱验证，请使用下方的验证码完成操作。为了保障您的账号安全，请勿将验证码告知他人。</p>" +
                "                  " +
                "                  <!-- 验证码核心区域 -->" +
                "                  <div class='code-box'>" +
                "                    <span class='code-text'>{}</span>" +
                "                  </div>" +
                "                  " +
                "                  <!-- 提示信息 -->" +
                "                  <p style='margin: 0; text-align: center; font-size: 14px; color: #666666;'>" +
                "                    验证码有效期为 <strong style='color: #4A90E2;'>{}分钟</strong>，请尽快使用。" +
                "                  </p>" +
                "                </td>" +
                "              </tr>" +
                "            </table>" +
                "            " +
                "            <!-- 底部区域 -->" +
                "            <div class='footer-section'>" +
                "              <p class='text-secondary' style='margin: 0 0 10px;'>" +
                "                如果您没有请求此验证码，请忽略此邮件。" +
                "              </p>" +
                "              <p class='text-secondary' style='margin: 0;'>" +
                "                &copy; {} Pixie Vision. All rights reserved." +
                "              </p>" +
                "            </div>" +
                "          </div>" +
                "          " +
                "          <!-- 外部版权/退订链接 (可选) -->" +
                "          <p style='margin-top: 20px; font-size: 12px; color: #cbd5e0; text-align: center;'>" +
                "            此邮件由系统自动发送，请勿直接回复。" +
                "          </p>" +
                "        </td>" +
                "      </tr>" +
                "    </table>" +
                "  </center>" +
                "</body>" +
                "</html>";

        // 使用 StrUtil 格式化，填入6位验证码
        // 假设 verificationCode 已经是字符串格式，如 "839201"
        return StrUtil.format(
                htmlTemplate,                // html模板
                verificationCode,   // 将验证码插入到模板中
                "5",                        // 验证码有效时间
                DateUtil.thisYear()         // 当前年份
        );
    }
}
