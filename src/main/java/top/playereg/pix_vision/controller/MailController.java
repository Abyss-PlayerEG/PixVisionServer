package top.playereg.pix_vision.controller;

import cn.hutool.core.util.StrUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
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

    private final EmailService emailService;
    private final VerificationCodeServices verificationCodeServices;

    /**
     * 发送"验证码"邮件
     *
     * @apiNote 发送一封 HTML 格式的验证码邮件
     * @param to 收件人邮箱
     * @param subject ResponsePojo<Boolean>
     * @return 响应结果
     * @author PlayerEG
     */
    @PostMapping("/send-email-code")
    @ApiOperation(value = "发送\"验证码\"邮件", notes = "发送一封 HTML 格式的验证码邮件")
    public ResponsePojo<Boolean> sendEmailCode(
            @ApiParam(value = "收件人邮箱地址", required = true, example = "test@example.com") @RequestParam String to,
            @ApiParam(value = "邮件主题", required = true, example = "PixVision验证码邮件") @RequestParam String subject
    ) {
        if (!PVSUtils.isEmail(to)) {
            return ResponsePojo.error(false, "邮箱格式错误");
        }
        //生成验证码
        String verificationCode = verificationCodeServices.verificationCode();
        String html = EmailConfig.generateVerificationEmailHtml(verificationCode);
//        String html = StrUtil.format("<h1>验证码</h1><p style='color:red'> {} </p>", verificationCode); // todo 待完善
        String emailId = emailService.sendHtmlMail(to, subject, html);//发送验证码

        verificationCodeServices.setRedisVCode( to, verificationCode ); //放进Redis中


        return ResponsePojo.success(true, "邮件发送成功");
    }

    /**
     * 验证"验证码"
     *
     * @apiNote 验证用户输入的验证码
     * @param email 用户邮箱
     * @param inputVCode 验证码
     * @return ResponsePojo<Boolean>
     * @author blue_sky_ks
     */
    @PostMapping( "/verify-email-code-test" )
    @ApiOperation( value = "验证\"验证码\" - 测试", notes = "验证用户输入的验证码" )
    public ResponsePojo<Boolean> verifyEmailCode(
        @ApiParam( value = "用户邮箱", required = true, example = "test@example.com") @RequestParam String email,
        @ApiParam( value = "验证码", required = true, example = "ABCDEF") @RequestParam String inputVCode
    ){
        if (!PVSUtils.isEmail(email)){
            return ResponsePojo.error(false, "邮箱格式错误");
        }
        if (!PVSUtils.isVCode(inputVCode)){
            return ResponsePojo.error(false, "验证码格式错误");
        }

        boolean isTrue = verificationCodeServices.verificationCodeVerify( email, inputVCode);

        if(!isTrue){
            return ResponsePojo.error( false, "验证失败" );
        }

        return ResponsePojo.success(true, "验证成功" );
    }
}
