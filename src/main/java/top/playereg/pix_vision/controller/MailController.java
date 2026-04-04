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
import top.playereg.pix_vision.pojo.userPojo.User;
import top.playereg.pix_vision.service.EmailService;
import top.playereg.pix_vision.service.EmailTemplateService;
import top.playereg.pix_vision.service.UserService;
import top.playereg.pix_vision.service.VerificationCodeServices;
import top.playereg.pix_vision.util.Aspect.LogRecord;
import top.playereg.pix_vision.util.RegexUtils;

/**
 * 邮件服务接口
 *
 * @see ResponsePojo
 * @see top.playereg.pix_vision.service.Impl.VerificationCodeServicesImpl
 * @see top.playereg.pix_vision.service.Impl.EmailServiceImpl
 * @author PlayerEG
 */
@RestController
@RequestMapping("/api/mail")
@RequiredArgsConstructor
@Tag(name = "邮件服务接口")
@SuppressWarnings("all")
public class MailController {

    private static final Logger log = LoggerFactory.getLogger(MailController.class);
    private final EmailService emailService;
    private final VerificationCodeServices verificationCodeServices;
    private final UserService userService;
    private final EmailTemplateService emailTemplateService;

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
    @LogRecord( module = "\"验证码\"模块", event = "发送\"验证码\"")
    @Operation(
            summary = "发送\"验证码\"邮件",
            description = """
                    # 发送一封 HTML 格式的验证码邮件
                                
                    ## 参数说明：
                    - to: 收件人邮箱地址，格式为标准邮箱格式
                    - subject: 邮件主题，字符串类型
                    - username: 用户昵称（条件必填），**注册时必须传入**，登录和改密时**不能传入**
                    - emailText: 邮件内容类型，可选值：注册、登录、改密
                                
                    ## 返回说明：
                    - **发送成功**：返回 **{"data": true}** 和"邮件发送成功"提示
                    - **发送失败**：返回 **{"data": false}** 和"邮件发送失败"提示
                    - **注册时未传用户名**：返回 **{"data": false}** 和"注册时必须传入用户名"提示
                    - **登录/改密时传入用户名**：返回 **{"data": false}** 和"登录和改密时不能传入用户名，将自动从数据库获取"提示
                    - **用户不存在**：返回 **{"data": false}** 和"用户不存在，请先注册"提示
                    - **格式错误**：返回 **{"data": false}** 和相应的"邮箱或内容类型错误"提示
                                
                    ## 业务逻辑：
                    1. 校验邮箱格式是否合法
                    2. 根据 emailText 类型校验 username 参数：
                       - **注册**：username 必须传入
                       - **登录/改密**：username 不能传入，自动从数据库查询用户名
                    3. 登录/改密时从数据库查询用户信息，获取用户名
                    4. 根据 emailText 类型生成对应的邮件内容（注册验证/登录验证/密码修改）
                    5. 生成 6 位随机验证码并存入 Redis
                    6. 使用 HTML 邮件模板渲染邮件内容
                    7. 发送邮件并将验证码与邮箱绑定存储
                                
                    ## 注意事项：
                    - 验证码默认有效期由 Redis 配置决定
                    - emailText 仅支持：**注册**、**登录**、**改密** 三种类型
                    - **注册场景**：必须传入 username 参数
                    - **登录/改密场景**：禁止传入 username 参数，系统会自动从数据库查询
                    """
    )
    public ResponsePojo<Boolean> sendEmailCode(
            @Parameter(description = "收件人邮箱地址", required = true, example = "test@example.com") @RequestParam String to,
            @Parameter(description = "邮件标题", required = true, example = "PixVision 验证码邮件") @RequestParam String subject,
            @Parameter(description = "用户昵称（可选，不传则自动从数据库查询或使用邮箱前缀）", required = false, example = "dev-username") @RequestParam(required = false) String username,
            @Parameter(description = "邮件内容类型，可选值：注册、登录、改密", required = true, example = "注册") @RequestParam String emailText
    ) {
        if (!RegexUtils.isEmail(to)) {
            return ResponsePojo.error(false, "邮箱格式错误");
        }
        
        // 根据邮件类型校验 username 参数
        if ("注册".equals(emailText)) {
            // 注册时 username 不能为空
            if (username == null || username.isEmpty()) {
                return ResponsePojo.error(false, "注册时必须传入用户名");
            }
            log.info("注册场景，使用传入的用户名：{}", username);
        } else if ("登录".equals(emailText) || "改密".equals(emailText)) {
            // 登录和改密时 username 不能传入，从数据库查询
            if (username != null && !username.isEmpty()) {
                return ResponsePojo.error(false, "登录和改密时不能传入用户名，将自动从数据库获取");
            }
            // 从数据库查询用户昵称
            User user = userService.selectAllUserByEmail(to);
            if (user != null) {
                username = user.getUsername();
                log.info("{}场景，从数据库查询到昵称：{}", emailText, username);
            } else {
                return ResponsePojo.error(false, "用户不存在，请先注册");
            }
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
                return ResponsePojo.error(false, "邮件内容类型错误 - 可选值：注册、登录、改密");
        }

        log.info(StrUtil.format("{} {}", username, content));

        // 生成验证码
        String verificationCode = verificationCodeServices.verificationCode();
        
        // 使用模板服务渲染邮件 HTML
        String html = emailTemplateService.renderVerificationEmail(
                verificationCode,
                username,
                content
        );
        try {
            String emailId = emailService.sendEMail(to, subject, html);//发送验证码
        } catch (Exception e) {
            log.error("邮件发送失败：{}", e.getMessage());
            return ResponsePojo.error(false, "邮件发送失败");
        }

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
    @Operation(
            summary = "验证\"验证码\" - 测试", 
            description = """
                    # 验证用户输入的邮箱验证码是否正确（测试用）
                    
                    ## 参数说明：
                    - email: 用户邮箱地址，格式为标准邮箱格式
                    - inputVCode: 用户输入的验证码，通常为 6 位大写字母或数字组合
                    
                    ## 返回说明：
                    - **验证成功**：返回 **"data": true** 和"验证成功"提示
                    - **验证失败**：返回 **"data": false** 和"验证失败"提示
                    - **格式错误**：返回 **"data": false** 和相应的"邮箱或验证码格式错误"提示
                    
                    ## 业务逻辑：
                    1. 校验邮箱格式是否合法
                    2. 校验验证码格式是否合法
                    3. 从 Redis 中获取该邮箱对应的验证码进行比对
                    4. 验证成功后会自动清除 Redis 中的验证码
                    """
    )
    public ResponsePojo<Boolean> verifyEmailCode(
            @Parameter(description = "用户邮箱", required = true, example = "test@example.com") @RequestParam String email,
            @Parameter(description = "验证码", required = true, example = "ABCDEF") @RequestParam String inputVCode
    ) {
        if (!RegexUtils.isEmail(email)) {
            return ResponsePojo.error(false, "邮箱格式错误");
        }
        if (!RegexUtils.isVCode(inputVCode,6)) {
            return ResponsePojo.error(false, "验证码格式错误");
        }

        boolean isTrue = verificationCodeServices.verificationCodeVerify(email, inputVCode);

        if (!isTrue) {
            return ResponsePojo.error(false, "验证失败");
        }

        return ResponsePojo.success(true, "验证成功");
    }
}
