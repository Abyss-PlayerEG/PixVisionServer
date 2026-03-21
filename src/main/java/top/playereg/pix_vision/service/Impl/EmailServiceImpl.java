package top.playereg.pix_vision.service.Impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.mail.MailAccount;
import cn.hutool.extra.mail.MailUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import top.playereg.pix_vision.config.EmailConfig;
import top.playereg.pix_vision.service.EmailService;
import top.playereg.pix_vision.util.PVSUtils;

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

    /**
     * 获取配置好的 MailAccount
     *
     * @implNote 获取配置好的 MailAccount
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

        // SSL 和 STARTTLS 配置 - 根据配置文件动态设置
        account.setSslEnable(emailConfig.isSslEnable());
        account.setStarttlsEnable(emailConfig.isStarttlsEnable());

        // 添加连接超时设置
        account.setConnectionTimeout(5000); // 5 秒连接超时
        account.setTimeout(10000); // 10 秒读取超时

        return account;
    }

    /**
     * 发送纯文本邮件
     *
     * @implNote 发送纯文本邮件
     * @param to      收件人
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
            log.error("邮件发送失败：{}", e.getMessage());
            throw new RuntimeException("邮件发送失败", e);
        }
    }

    /**
     * 发送 HTML 邮件
     *
     * @implNote 发送 HTML 邮件
     * @param to          收件人
     * @param subject     主题
     * @param htmlContent HTML 内容
     * @author PlayerEG
     */
    public String sendHtmlMail(
            String to,
            String subject,
            String htmlContent
    ) {
        try {
            log.info("开始发送 HTML 邮件到：{}", to);
            MailAccount account = getMailAccount();
            String result = MailUtil.send(account, to, subject, htmlContent, true);
            log.info("HTML 邮件发送成功，邮件 ID: {}", result);
            return result;
        } catch (Exception e) {
            log.error("邮件发送失败：{}", e.getMessage());
            throw new RuntimeException("邮件发送失败：" + e.getMessage(), e);
        }
    }

    /**
     * 群发邮件
     *
     * @implNote 群发邮件
     * @param subject 主题
     * @param content 内容
     * @param tos     收件人（多个）
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
            log.error("邮件发送失败：{}", e.getMessage());
            throw new RuntimeException("邮件发送失败", e);
        }
    }
}
