package top.playereg.pix_vision.controller;

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
import top.playereg.pix_vision.pojo.ResponsePojo;
import top.playereg.pix_vision.pojo.userPojo.User;
import top.playereg.pix_vision.service.UserService;
import top.playereg.pix_vision.service.VerificationCodeServices;
import top.playereg.pix_vision.util.RegexUtils;
import top.playereg.pix_vision.util.StrSwitchUtils;

/**
 * 用户注册相关接口
 *
 * @author PlayerEG
 * @see top.playereg.pix_vision.service.Impl.UserServiceImpl
 */
@RestController
@SuppressWarnings("all")
@RequestMapping("/api/user/register")
@RequiredArgsConstructor
@Tag(name = "用户注册相关接口")
public class UserRegisterController {
    private static final Logger log = LoggerFactory.getLogger(UserRegisterController.class);

    private final UserService userService;
    private final VerificationCodeServices verificationCodeServices;

    /**
     * 用户注册
     *
     * @param username 用户名
     * @param password 密码
     * @param nickname 昵称
     * @param email    邮箱
     * @param vCode    验证码
     * @return 响应数据<User>
     * @author PlayerEG
     */
    @PostMapping("")
    @Operation(
        summary = "用户注册接口",
        description = """
            # 用户注册

            ## 参数说明：
            - username: 用户名，6-16 位，只允许字母、数字和下划线，字符串类型，必填
            - password: 登录密码，字符串类型，必填，建议使用强密码组合
            - nickname: 用户昵称，字符串类型，**可为空**，为空时自动生成随机昵称
            - email: 邮箱地址，字符串类型，必填，用于接收验证码和后续找回密码
            - vCode: 邮箱验证码，6 位大写字母或数字，字符串类型，必填

            ## 返回说明：
            - **注册成功**：返回 **{"data": {User 对象}}** 和"注册成功"提示
            - **用户名格式错误**：返回 **{"data": null}** 和"用户名格式错误"提示
            - **邮箱格式错误**：返回 **{"data": null}** 和"邮箱格式错误"提示
            - **验证码格式错误**：返回 **{"data": null}** 和"验证码格式错误"提示
            - **验证码错误**：返回 **{"data": null}** 和"验证码错误"提示
            - **注册失败**：返回 **{"data": null}** 和"注册失败：该用户名或邮箱已注册"提示

            ## 业务逻辑：
            1. 校验用户名格式是否符合规范（6-16 位字母、数字、下划线）
            2. 校验邮箱格式是否正确
            3. 校验验证码格式是否正确（6 位大写字母或数字）
            4. 验证邮箱验证码是否与 Redis 中存储的一致
            5. 如果昵称为空，生成随机默认昵称（格式：user+ 随机词）
            6. 对密码进行 SHA-256 哈希加密处理
            7. 创建用户并保存到数据库
            8. 返回用户信息和成功提示

            ## 注意事项：
            - 昵称参数为**可选参数**，不传或为空时自动生成
            - 验证码有效期由 Redis 配置决定（默认 5 分钟）
            - 密码会使用 **SHA-256** 进行加密存储
            - **用户名**和**邮箱**不可重复注册
            - 建议使用强密码组合（大小写字母 + 数字 + 特殊字符）
            """
    )
    public ResponsePojo<User> registerUser(
        @Parameter(description = "用户名，6-16 位字母/数字/下划线", required = true, example = "dev_user") @RequestParam String username,
        @Parameter(description = "登录密码，会使用 SHA-256 加密存储", required = true, example = "123456") @RequestParam String password,
        @Parameter(description = "用户昵称（可选，为空时自动生成）", required = false, example = "测试用户") @RequestParam(required = false) String nickname,
        @Parameter(description = "邮箱地址，用于接收验证码", required = true, example = "test@example.com") @RequestParam String email,
        @Parameter(description = "邮箱验证码，6 位大写字母或数字", required = true, example = "ABCDEF") @RequestParam String vCode
    ) {
        // 基础数据校验
        if (!RegexUtils.isUsername(username)) {
            return ResponsePojo.error(null, "用户名格式错误");
        }
        if (!RegexUtils.isEmail(email)) {
            return ResponsePojo.error(null, "邮箱格式错误");
        }
        if (!RegexUtils.isVCode(vCode, 6)) {
            return ResponsePojo.error(null, "验证码格式错误");
        }
        // 验证码验证
        boolean isTrue = verificationCodeServices.verificationCodeVerify(email, vCode);
        if (!isTrue) {
            return ResponsePojo.error(null, "验证码错误");
        }
        // 如果昵称为空，则生成一个随机昵称
        if (nickname == null || nickname.isEmpty()) {
            nickname = StrSwitchUtils.generateRandomUserDefaultNickName("user");
        }
        // 密码加密
        password = StrSwitchUtils.PasswdToHash256(password);

        // 创建用户
        User user = userService.registerUser(username, password, nickname, email);

        // 判断用户是否创建成功
        if (user == null) {
            return ResponsePojo.error(null, "注册失败：该用户名或邮箱已注册");
        }

        return ResponsePojo.success(user, "注册成功");
    }
}
