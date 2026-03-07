package top.playereg.pix_vision.service.Impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.extra.mail.MailAccount;
import cn.hutool.extra.mail.MailUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import top.playereg.pix_vision.config.EmailConfig;
import top.playereg.pix_vision.service.EmailService;

/**
 * 邮件服务实现类
 *
 * @author PlayerEG
 * */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {
    private final EmailConfig emailConfig;

    /**
     * 获取配置好的 MailAccount
     *
     * @return MailAccount
     * @author PlayerEG
     */
    private MailAccount getMailAccount() {
        MailAccount account = new MailAccount();
        account.setHost(emailConfig.getHost());
        account.setPort(emailConfig.getPort());
        account.setAuth(true);
        account.setFrom(emailConfig.getFrom());
        account.setUser(emailConfig.getUsername());
        account.setPass(emailConfig.getPassword());

        // SSL 配置
        account.setSslEnable(emailConfig.isSslEnable());
        account.setStarttlsEnable(true);

        return account;
    }

    /**
     * 发送纯文本邮件
     *
     * @param to 收件人
     * @param subject 主题
     * @param content 内容
     * @author PlayerEG
     */
    public String sendTextMail(
            String to,
            String subject,
            String content
    ) {
        try {
            MailAccount account = getMailAccount();
            return MailUtil.send(account, to, subject, content, false);
        } catch (Exception e) {
            log.error("发送邮件失败: {}", e.getMessage());
            throw new RuntimeException("邮件发送失败", e);
        }
    }

    /**
     * 发送HTML邮件
     *
     * @param to 收件人
     * @param subject 主题
     * @param htmlContent HTML内容
     * @author PlayerEG
     */
    public String sendHtmlMail(
            String to,
            String subject,
            String htmlContent
    ) {
        try {
            MailAccount account = getMailAccount();
            return MailUtil.send(account, to, subject, htmlContent, true);
        } catch (Exception e) {
            log.error("发送HTML邮件失败: {}", e.getMessage());
            throw new RuntimeException("邮件发送失败", e);
        }
    }

    /**
     * 群发邮件
     *
     * @param subject 主题
     * @param content 内容
     * @param tos 收件人（多个）
     * @author PlayerEG
     */
    public String sendMailToMany(
            String subject,
            String content,
            String... tos
    ) {
        try {
            MailAccount account = getMailAccount();
            return MailUtil.send(account, CollUtil.newArrayList(tos),
                    subject, content, false);
        } catch (Exception e) {
            log.error("群发邮件失败: {}", e.getMessage());
            throw new RuntimeException("邮件发送失败", e);
        }
    }
}
