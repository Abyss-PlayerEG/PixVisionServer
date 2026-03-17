package top.playereg.pix_vision.service;

import cn.hutool.extra.mail.MailAccount;

public interface EmailService {
//    MailAccount getMailAccount();

    // 发送纯文本邮件
    public String sendTextMail(
            String to,
            String subject,
            String content
    );

    // 发送 HTML 邮件
    public String sendHtmlMail(
            String to,
            String subject,
            String htmlContent
    );

    // 批量发送邮件
    public String sendMailToMany(
            String subject,
            String content,
            String... tos
    );

    //验证码接口
    public String verificationCode();

    //验证码Redis
    public void RedisVCode( String email, String vCode );
}
