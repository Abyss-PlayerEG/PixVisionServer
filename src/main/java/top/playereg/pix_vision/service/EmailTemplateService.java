package top.playereg.pix_vision.service;

import java.util.Map;

/**
 * 邮件模板渲染服务接口
 *
 * @author PlayerEG
 */
public interface EmailTemplateService {

    /**
     * 渲染验证码邮件模板
     *
     * @param code      验证码
     * @param username  用户名
     * @param emailText 邮件内容类型（注册/登录/改密）
     * @return 渲染后的 HTML 字符串
     * @author PlayerEG
     */
    String renderVerificationEmail(String code, String username, String emailText);

    /**
     * 渲染重置密码邮件模板
     *
     * @param username 用户名
     * @param password 密码
     * @return 渲染后的 HTML字符串
     * @author blue_sky_ks
     */
    String renderResetPasswordEmail(String username, String password);

    /**
     * 使用自定义占位符渲染模板
     *
     * @param templateName 模板名称（不含扩展名）
     * @param placeholders 占位符映射表
     * @return 渲染后的 HTML 字符串
     * @author PlayerEG
     */
    String renderTemplate(String templateName, Map<String, String> placeholders);
}
