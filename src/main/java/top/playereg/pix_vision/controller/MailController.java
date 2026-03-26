package top.playereg.pix_vision.controller;

import cn.hutool.core.util.StrUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.playereg.pix_vision.config.EmailConfig;
import top.playereg.pix_vision.pojo.ResponsePojo;
import top.playereg.pix_vision.service.EmailService;
import top.playereg.pix_vision.service.VerificationCodeServices;
import top.playereg.pix_vision.util.RegexUtils;

@RestController
@RequestMapping("/api/mail")
@RequiredArgsConstructor
@Tag(name = "邮件服务接口")
@SuppressWarnings("all")
public class MailController {

    private static final Logger log = LoggerFactory.getLogger(MailController.class);
    private final EmailService emailService;
    private final VerificationCodeServices verificationCodeServices;

    /**
     * 发送"验证码"邮件
     *
     * @param to
     * @param subject
     * @param username
     * @param emailText
     * @return 响应结果
     * @implNote 发送"验证码"邮件
     * @apiNote 发送一封 HTML 格式的验证码邮件
     * @author PlayerEG
     */
    @PostMapping("/send-email-code")
    @Operation(
            summary = "发送\"验证码\"邮件",
            description = """
                    发送一封 HTML 格式的验证码邮件。
                    参数说明：<br/>
                    • to: 收件人邮箱地址，格式为标准邮箱格式<br/>
                    • subject: 邮件主题，字符串类型<br/>
                    • username: 用户昵称，用于邮件模板中个性化显示<br/>
                    • emailText: 邮件内容类型，可选值：注册、登录、修改密码
                    """
    )
    public ResponsePojo<Boolean> sendEmailCode(
            @Parameter(description = "收件人邮箱地址", required = true, example = "test@example.com") @RequestParam String to,
            @Parameter(description = "邮件标题", required = true, example = "PixVision 验证码邮件") @RequestParam String subject,
            @Parameter(description = "用户名", required = true, example = "dev-username") @RequestParam String username,
            @Parameter(description = "邮件内容类型，可选值：注册、登录、改密", required = true, example = "注册") @RequestParam String emailText
    ) {
        if (RegexUtils.isEmail(to)) {
            return ResponsePojo.error(false, "邮箱格式错误");
        }
        // 邮箱内容
        String content = "";
        switch (emailText) {
            case "登录":
                content = "登录验证";
                break;
            case "注册":
                content = "注册验证";
                break;
            case "改密":
                content = "密码修改";
                break;
            default:
                return ResponsePojo.error(false, "邮件内容类型错误 - 可选值：注册、登录、修改 ");
        }

        log.info(StrUtil.format("{} {}", username, content));

        //生成验证码
        String verificationCode = verificationCodeServices.verificationCode();
        // 渲染模板
        String html = EmailConfig.renderVerificationEmailTemplate(
                verificationCode,
                username,
                content
        );
        String emailId = emailService.sendEMail(to, subject, html);//发送验证码

        verificationCodeServices.setRedisVCode(to, verificationCode); //放进Redis中


        return ResponsePojo.success(true, "邮件发送成功");
    }

    /**
     * 验证"验证码"
     *
     * @param email
     * @param inputVCode
     * @return ResponsePojo<Boolean>
     * @implNote 验证"验证码"
     * @apiNote 验证用户输入的验证码
     * @author blue_sky_ks
     */
    @PostMapping("/verify-email-code-test")
    @Operation(summary = "验证\"验证码\" - 测试", description = "验证用户输入的验证码")
    public ResponsePojo<Boolean> verifyEmailCode(
            @Parameter(description = "用户邮箱", required = true, example = "test@example.com") @RequestParam String email,
            @Parameter(description = "验证码", required = true, example = "ABCDEF") @RequestParam String inputVCode
    ) {
        if (RegexUtils.isEmail(email)) {
            return ResponsePojo.error(false, "邮箱格式错误");
        }
        if (!RegexUtils.isVCode(inputVCode)) {
            return ResponsePojo.error(false, "验证码格式错误");
        }

        boolean isTrue = verificationCodeServices.verificationCodeVerify(email, inputVCode);

        if (!isTrue) {
            return ResponsePojo.error(false, "验证失败");
        }

        return ResponsePojo.success(true, "验证成功");
    }
}
