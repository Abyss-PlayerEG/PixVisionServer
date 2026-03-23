package top.playereg.pix_vision.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
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
import top.playereg.pix_vision.util.PVSUtils;

@RestController
@RequestMapping("/api/mail")
@RequiredArgsConstructor
@Api(tags = "邮件服务接口")
public class MailController {

    private static final Logger log = LoggerFactory.getLogger(MailController.class);
    private final EmailService emailService;
    private final VerificationCodeServices verificationCodeServices;

    /**
     * 发送"验证码"邮件
     *
     * @implNote 发送"验证码"邮件
     * @param to      收件人邮箱
     * @param subject 邮件标题
     * @param username 用户名
     * @param emailText 邮件内容类型
     * @return 响应结果
     * @apiNote 发送一封 HTML 格式的验证码邮件
     * @author PlayerEG
     */
    @PostMapping("/send-email-code")
    @ApiOperation(
            value = "发送\"验证码\"邮件",
            notes = "发送一封 HTML 格式的验证码邮件。" +
                    "参数说明：<br/>" +
                    "• to: 收件人邮箱地址，格式为标准邮箱格式<br/>" +
                    "• subject: 邮件主题，字符串类型<br/>" +
                    "• username: 用户昵称，用于邮件模板中个性化显示<br/>" +
                    "• emailText: 邮件内容类型，可选值：注册、登录、修改密码")
    public ResponsePojo<Boolean> sendEmailCode(
            @ApiParam(value = "收件人邮箱地址", required = true, example = "test@example.com") @RequestParam String to,
            @ApiParam(value = "邮件标题", required = true, example = "PixVision验证码邮件") @RequestParam String subject,
            @ApiParam(value = "用户名", required = true, example = "dev-username") @RequestParam String username,
            @ApiParam(value = "邮件内容类型，可选值：注册、登录、改密", required = true, example = "注册") @RequestParam String emailText
    ) {
        if (!PVSUtils.isEmail(to)) {
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
                return ResponsePojo.error(false, "邮件内容错误（可选值：注册、登录、修改密码）");
        };

        //生成验证码
        String verificationCode = verificationCodeServices.verificationCode();
        // 渲染模板
        String html = EmailConfig.renderVerificationEmailTemplate(
                verificationCode,
                username,
                content
        );
        String emailId = emailService.sendHtmlMail(to, subject, html);//发送验证码

        verificationCodeServices.setRedisVCode(to, verificationCode); //放进Redis中


        return ResponsePojo.success(true, "邮件发送成功");
    }

    /**
     * 验证"验证码"
     *
     * @implNote 验证"验证码"
     * @param email      用户邮箱
     * @param inputVCode 验证码
     * @return ResponsePojo<Boolean>
     * @apiNote 验证用户输入的验证码
     * @author blue_sky_ks
     */
    @PostMapping("/verify-email-code-test")
    @ApiOperation(value = "验证\"验证码\" - 测试", notes = "验证用户输入的验证码")
    public ResponsePojo<Boolean> verifyEmailCode(
            @ApiParam(value = "用户邮箱", required = true, example = "test@example.com") @RequestParam String email,
            @ApiParam(value = "验证码", required = true, example = "ABCDEF") @RequestParam String inputVCode
    ) {
        if (!PVSUtils.isEmail(email)) {
            return ResponsePojo.error(false, "邮箱格式错误");
        }
        if (!PVSUtils.isVCode(inputVCode)) {
            return ResponsePojo.error(false, "验证码格式错误");
        }

        boolean isTrue = verificationCodeServices.verificationCodeVerify(email, inputVCode);

        if (!isTrue) {
            return ResponsePojo.error(false, "验证失败");
        }

        return ResponsePojo.success(true, "验证成功");
    }
}
