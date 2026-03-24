package top.playereg.pix_vision.service.Impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import top.playereg.pix_vision.config.EmailConfig;
import top.playereg.pix_vision.service.EmailService;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

/**
 * 邮件服务实现类
 *
 * @author PlayerEG
 */
@Slf4j
@Service
@SuppressWarnings("unused")
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {
    private final EmailConfig emailConfig;
    private final JavaMailSender mailSender;

    /**
     * 创建 MimeMessage
     */
    private MimeMessage createMimeMessage() {
        return mailSender.createMimeMessage();
    }

    /**
     * 发送纯文本邮件
     *
     * @param to      收件人
     * @param subject 主题
     * @param content 内容
     */
    public String sendTextMail(
            String to,
            String subject,
            String content
    ) {
        try {
            log.info("开始发送文本邮件到：{}", to);
            MimeMessage message = createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false);
            helper.setFrom(emailConfig.getFrom());
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(content);
            
            mailSender.send(message);
            log.info("文本邮件发送成功");
            return "SUCCESS";
        } catch (MessagingException e) {
            log.error("邮件发送失败：{}", e.getMessage());
            throw new RuntimeException("邮件发送失败：" + e.getMessage(), e);
        }
    }

    /**
     * 发送 HTML 邮件
     *
     * @param to          收件人
     * @param subject     主题
     * @param htmlContent HTML 内容
     */
    public String sendHtmlMail(
            String to,
            String subject,
            String htmlContent
    ) {
        try {
            log.info("开始发送 HTML 邮件到：{}", to);
            MimeMessage message = createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setFrom(emailConfig.getFrom());
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true); // true 表示 HTML 内容
            
            mailSender.send(message);
            log.info("HTML 邮件发送成功");
            return "SUCCESS";
        } catch (MessagingException e) {
            log.error("邮件发送失败：{}", e.getMessage());
            throw new RuntimeException("邮件发送失败：" + e.getMessage(), e);
        }
    }

    /**
     * 群发邮件
     *
     * @param subject 主题
     * @param content 内容
     * @param tos     收件人（多个）
     */
    public String sendMailToMany(
            String subject,
            String content,
            String... tos
    ) {
        try {
            log.info("开始群发邮件到：{}", String.join(", ", tos));
            
            for (String to : tos) {
                MimeMessage message = createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, false);
                helper.setFrom(emailConfig.getFrom());
                helper.setTo(to);
                helper.setSubject(subject);
                helper.setText(content);
                mailSender.send(message);
            }
            
            log.info("群发邮件发送成功，共 {} 封", tos.length);
            return "SUCCESS";
        } catch (MessagingException e) {
            log.error("邮件发送失败：{}", e.getMessage());
            throw new RuntimeException("邮件发送失败：" + e.getMessage(), e);
        }
    }
}
