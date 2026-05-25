package top.playereg.pix_vision.service.Impl;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import top.playereg.pix_vision.config.EmailConfig;
import top.playereg.pix_vision.service.EmailService;
import top.playereg.pix_vision.service.EmailTemplateService;
import top.playereg.pix_vision.service.VerificationCodeServices;
import top.playereg.pix_vision.util.PixVisionLogger;

/**
 * 邮件服务实现类
 *
 * @author PlayerEG
 */
@Service
@SuppressWarnings("unused")
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {
    private static final PixVisionLogger log = PixVisionLogger.create(EmailServiceImpl.class);

    private final EmailConfig emailConfig;
    private final JavaMailSender mailSender;
    private final String devSuccessLog = "模拟邮箱发送已开启";

    @Autowired
    private VerificationCodeServices verificationCodeServices;
    @Autowired
    private EmailTemplateService emailTemplateService;

    /**
     * 创建 MimeMessage
     *
     * @return MimeMessage
     * @author PlayerEG
     */
    private MimeMessage createMimeMessage() {
        return mailSender.createMimeMessage();
    }

    /**
     * 发送 HTML 邮件
     *
     * @param to      收件人
     * @param subject 主题
     * @param content HTML 内容
     * @return String
     * @author PlayerEG
     */
    public String sendEMail(
        String to,
        String subject,
        String content
    ) {
        // 开发模式
        if (emailConfig.devMode) {
            log.info(devSuccessLog);
            return "SUCCESS";
        }
        try {
            log.info("开始发送 HTML 邮件到: {}", to);
            MimeMessage message = createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setFrom(emailConfig.getFrom());
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(content, true); // true 表示 HTML 内容

            mailSender.send(message);
            log.info("HTML 邮件发送成功: {}", to);
            return "SUCCESS";
        } catch (MessagingException e) {
            log.error("邮件发送失败: {}", e.getMessage());
            throw new RuntimeException("邮件发送失败: " + e.getMessage(), e);
        }
    }

    /**
     * 群发邮件
     *
     * @param subject 主题
     * @param content 内容
     * @param tos     收件人（多个）
     * @return String
     * @author PlayerEG
     * @deprecated 系统设计暂不支持群发邮件
     */
    @Deprecated
    public String sendMailToMany(
        String subject,
        String content,
        String... tos
    ) {
        // 开发者模式
        if (emailConfig.devMode) {
            log.info(devSuccessLog);
            return "SUCCESS";
        }
        try {
            log.info("开始群发邮件到: {}", String.join(", ", tos));

            for (String to : tos) {
                MimeMessage message = createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, false);
                helper.setFrom(emailConfig.getFrom());
                helper.setTo(to);
                helper.setSubject(subject);
                helper.setText(content);
                mailSender.send(message);
                log.info("邮件发送成功: {}", to);
            }

            log.info("群发邮件发送成功，共 {} 封", tos.length);
            return "SUCCESS";
        } catch (MessagingException e) {
            log.error("邮件发送失败: {}", e.getMessage());
            throw new RuntimeException("邮件发送失败: " + e.getMessage(), e);
        }
    }

    /**
     * 生成验证码并发送验证邮件（一站式方法）
     * <p>
     * 封装了验证码发送的完整流程：检查已存在验证码 → 生成验证码 → 渲染邮件模板 → 发送邮件 → 存入 Redis。
     * </p>
     *
     * @param email        收件人邮箱
     * @param username     用户名
     * @param action       操作类型
     * @param emailSubject 邮件主题
     * @return 发送结果
     * @author PlayerEG
     */
    @Override
    public VerificationEmailResult sendVerificationEmail(String email, String username, String action, String emailSubject) {
        // 检查是否已有未过期的验证码
        if (verificationCodeServices.hasRedisVCode(email)) {
            Long remainingTime = verificationCodeServices.getRedisVCodeRemainingTime(email);
            log.warn("该邮箱已有未过期的验证码，邮箱：{}，剩余时间：{}秒", email, remainingTime);
            return VerificationEmailResult.existingCode(remainingTime);
        }

        // 生成验证码
        String verificationCode = verificationCodeServices.verificationCode();

        // 使用模板服务渲染邮件 HTML
        String html = emailTemplateService.renderVerificationEmail(verificationCode, username, action);

        // 发送邮件
        try {
            sendEMail(email, emailSubject, html);
        } catch (Exception e) {
            log.error("{}验证码邮件发送失败，邮箱：{}，错误：{}", action, email, e.getMessage());
            return VerificationEmailResult.failure("邮件发送失败");
        }

        // 将验证码存入 Redis
        verificationCodeServices.setRedisVCode(email, verificationCode);

        log.info("{}验证码邮件发送成功，邮箱：{}", action, email);
        return VerificationEmailResult.success();
    }
}
