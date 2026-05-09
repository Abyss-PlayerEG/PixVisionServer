package top.playereg.pix_vision.service.Impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import top.playereg.pix_vision.config.EmailConfig;
import top.playereg.pix_vision.config.FilePathConfig;
import top.playereg.pix_vision.service.EmailTemplateService;
import top.playereg.pix_vision.util.ImageUtils;
import top.playereg.pix_vision.util.PixVisionLogger;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 邮件模板渲染服务实现类
 * <p>
 * 特性：
 * 1. 支持多种邮件模板
 * 2. Logo 图片缓存（避免重复读取）
 * 3. 统一的占位符替换逻辑
 * 4. 易于扩展新模板
 *
 * @author PlayerEG
 */
@Service
@RequiredArgsConstructor
public class EmailTemplateServiceImpl implements EmailTemplateService {
    private static final PixVisionLogger log = PixVisionLogger.create(EmailTemplateServiceImpl.class);

    private final EmailConfig emailConfig;

    /**
     * Logo Base64 缓存（线程安全）
     * Key: 文件名, Value: Base64 Data URI
     */
    private static final Map<String, String> LOGO_CACHE = new ConcurrentHashMap<>();

    /**
     * 系统名称常量
     */
    private static final String SYSTEM_NAME = "Pixie Vision";

    /**
     * 验证码过期时间（分钟）
     */
    private static final String EXPIRE_TIME = "5";

    @Override
    public String renderVerificationEmail(String code, String username, String emailText) {
        log.debug("开始渲染验证码邮件模板 - 用户: {}, 类型: {}", username, emailText);

        // 构建占位符
        Map<String, String> placeholders = buildVerificationPlaceholders(code, username, emailText);

        // 渲染模板
        return renderTemplate("email-verification", placeholders);
    }

    @Override
    public String renderResetPasswordEmail(String username, String password){
        log.debug("开始渲染重置密码邮件模板 - 用户: {}", username);

        // 构建占位符
        Map<String, String> placeholders = buildVerificationPlaceholders(password, username, "用户密码重置");

        return renderTemplate("email-reset-password", placeholders);
    }

    @Override
    public String renderTemplate(String templateName, Map<String, String> placeholders) {
        // 加载模板文件
        String template = loadTemplate(templateName);

        // 执行占位符替换
        String result = replacePlaceholders(template, placeholders);

        log.debug("模板渲染完成: {}", templateName);
        return result;
    }

    /**
     * 构建验证码邮件的占位符映射
     *
     * @param code      验证码
     * @param username  用户名
     * @param emailText 邮件内容类型
     * @return 占位符映射表
     */
    private Map<String, String> buildVerificationPlaceholders(String code, String username, String emailText) {
        // 开发模式
        if (emailConfig.devMode) {
            log.info("控制台模拟验证码发送：{}", code);
        }
        Map<String, String> placeholders = new HashMap<>();

        // 基础信息
        placeholders.put("{{username}}", StrUtil.nullToEmpty(username));
        placeholders.put("{{email_text}}", StrUtil.nullToEmpty(emailText));
        placeholders.put("{{code}}", StrUtil.nullToEmpty(code));
        placeholders.put("{{expireTime}}", EXPIRE_TIME);
        placeholders.put("{{year}}", String.valueOf(DateUtil.thisYear()));
        placeholders.put("{{systemName}}", SYSTEM_NAME);

        // Logo 图片（带缓存）
        placeholders.put("{{logoUriLight}}", getCachedLogoBase64("light.png"));
        placeholders.put("{{logoUriDark}}", getCachedLogoBase64("dark.png"));

        return placeholders;
    }

    /**
     * 从缓存或文件系统获取 Logo 的 Base64 编码
     *
     * @param logoFileName Logo 文件名
     * @return Base64 Data URI
     */
    private String getCachedLogoBase64(String logoFileName) {
        // 先从缓存中获取
        if (LOGO_CACHE.containsKey(logoFileName)) {
            log.trace("从缓存加载 Logo: {}", logoFileName);
            return LOGO_CACHE.get(logoFileName);
        }

        // 缓存未命中，从文件系统加载
        String base64 = loadLogoBase64(logoFileName);
        LOGO_CACHE.put(logoFileName, base64);
        log.info("Logo 已加载并缓存: {}", logoFileName);

        return base64;
    }

    /**
     * 加载模板文件（从 classpath 资源加载）
     *
     * @param templateName 模板名称（不含扩展名）
     * @return 模板内容
     */
    private String loadTemplate(String templateName) {
        // 从 classpath 的 template/email-html/ 目录加载
        String templatePath = StrUtil.format("template/email-html/{}.html", templateName);

        try {
            // 从 classpath 加载资源文件
            String template = ResourceUtil.readUtf8Str(templatePath);
            log.debug("模板加载成功: {}", templatePath);
            return template;
        } catch (Exception e) {
            log.error("模板加载失败: {}", templatePath, e);
            throw new RuntimeException("加载邮件模板失败: " + templatePath, e);
        }
    }

    /**
     * 加载 Logo 图片并转换为 Base64
     *
     * @param logoFileName Logo 文件名
     * @return Base64 Data URI
     */
    private String loadLogoBase64(String logoFileName) {
        String logoPath = StrUtil.format("{}{}{}",
            FilePathConfig.LogoPath,
            File.separator,
            logoFileName
        );
        log.debug("开始加载 Logo: {}", logoPath);

        try {
            return ImageUtils.imageToBase64(logoPath);
        } catch (Exception e) {
            log.error("Logo 加载失败: {}", logoPath, e);
            throw new RuntimeException("加载 Logo 图片失败: " + logoPath, e);
        }
    }

    /**
     * 执行占位符替换
     *
     * @param template     模板字符串
     * @param placeholders 占位符映射表
     * @return 替换后的字符串
     */
    private String replacePlaceholders(String template, Map<String, String> placeholders) {
        if (template == null || template.isEmpty()) {
            return "";
        }

        if (placeholders == null || placeholders.isEmpty()) {
            return template;
        }

        String result = template;

        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            String placeholder = entry.getKey();
            String value = entry.getValue();

            // 只替换非空值
            if (value != null) {
                result = result.replace(placeholder, value);
            }
        }

        return result;
    }

    /**
     * 清除 Logo 缓存（用于测试或重新加载）
     */
    public void clearLogoCache() {
        LOGO_CACHE.clear();
        log.info("Logo 缓存已清除");
    }
}
