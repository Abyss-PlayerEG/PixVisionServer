package top.playereg.pix_vision.service;

public interface EmailService {
    // 发送 HTML 邮件
    public String sendEMail(
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
}
