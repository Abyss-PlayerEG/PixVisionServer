package top.playereg.pix_vision.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import top.playereg.pix_vision.pojo.ResponsePojo;
import top.playereg.pix_vision.service.EmailService;
import top.playereg.pix_vision.service.VerificationCodeServices;

@RestController
@RequestMapping("/api/mail")
@RequiredArgsConstructor
@Api(tags = "邮件服务接口")
public class MailController {

    private final EmailService emailService;
    private final VerificationCodeServices verificationCodeServices;

    @PostMapping("/send-email-code")
    @ApiOperation(value = "发送\"验证码\"邮件", notes = "发送一封 HTML 格式的验证码邮件")
    public ResponsePojo<String> sendEmailCode(
            @ApiParam(value = "收件人邮箱地址", required = true, example = "test@example.com") @RequestParam String to,
            @ApiParam(value = "邮件主题", required = true, example = "HTML 测试邮件") @RequestParam String subject) {

        //生成验证码
        String verificationCode = verificationCodeServices.verificationCode();

        String html = "<h1>标题</h1><p style='color:red'>" + verificationCode  + "</p>"; // todo 待完善
        String emailId = emailService.sendHtmlMail(to, subject, html);//发送验证码

        verificationCodeServices.setRedisVCode( to, verificationCode ); //放进Redis中


        return ResponsePojo.success(emailId, "HTML 邮件发送成功");
    }
}
