package top.playereg.pix_vision.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.playereg.pix_vision.pojo.ResponsePojo;
import top.playereg.pix_vision.pojo.userPojo.User;
import top.playereg.pix_vision.service.TokenWhitelistService;
import top.playereg.pix_vision.service.UserService;
import top.playereg.pix_vision.service.VerificationCodeServices;
import top.playereg.pix_vision.util.RegexUtils;
import top.playereg.pix_vision.util.StrSwitchUtils;

/**
 * 用户密码管理相关接口（修改密码、忘记密码）
 *
 * @author blue_sky_ks, PlayerEG
 * @see top.playereg.pix_vision.service.Impl.UserServiceImpl
 */
@RestController
@SuppressWarnings("all")
@RequestMapping("/api/user/password")
@RequiredArgsConstructor
@Tag(name = "用户密码管理相关接口")
public class UserPasswordController {
    private static final Logger log = LoggerFactory.getLogger(UserPasswordController.class);

    private final UserService userService;
    private final VerificationCodeServices verificationCodeServices;
    private final TokenWhitelistService tokenWhitelistService;

    /**
     * 用户密码修改（登录后）
     *
     * @param request         HTTP请求对象，用于从Token中获取用户信息
     * @param newPassword     用户的新密码
     * @param confirmPassword 再次输入的新密码
     * @param vCode           邮箱验证码
     * @return ResponsePojo<Boolean> 修改结果
     * @throws Exception 修改失败
     * @author blue_sky_ks
     */
    @PostMapping("/change")
    @Operation(
        summary = "更换密码",
        description = """
            # 更换密码（需要登录认证）

            ## 特性
            - Token 认证（支持 Header 和 URL 参数两种方式）
            - 邮箱验证码验证
            - SHA-256 密码加密
            - 新旧密码一致性校验
            - 强制所有设备下线（移除所有 Token）

            ## 参数说明：
            - Authorization: Header 中的 Token，格式为 `Bearer <token>`，或通过 URL 参数 `?token=<token>` 传递
            - newPassword: 新密码，字符串类型，必填
            - confirmPassword: 确认新密码，字符串类型，必填，需与新密码一致
            - vCode: 邮箱验证码，6 位大写字母或数字，字符串类型，必填

            ## 返回说明：
            - **修改成功**：返回 **{"data": true}** 和"修改成功"提示，同时当前 Token 失效
            - **验证码错误**：返回 **{"data": false}** 和"验证码错误"提示
            - **新旧密码一致**：返回 **{"data": false}** 和"新旧密码不能一致"提示
            - **修改失败**：返回 **{"data": false}** 和"修改失败"提示

            ## 业务逻辑：
            1. 从请求的 Token 中提取用户 ID
            2. 根据用户 ID 从数据库查询用户信息（邮箱等）
            3. 校验验证码格式和正确性
            4. 校验两次输入的新密码是否一致
            5. 校验新密码与旧密码是否相同
            6. 对密码进行 SHA-256 哈希加密
            7. 更新数据库中的密码
            8. **移除该用户的所有 Token**（强制所有设备下线）
            9. 返回修改结果

            ## 注意事项：
            - **必须携带有效的 Token** 才能调用此接口
            - 验证码发送到用户的注册邮箱
            - 密码修改成功后，**该用户的所有 Token 会被立即移除**，所有设备需要重新登录
            - 建议使用强密码组合（大小写字母 + 数字 + 特殊字符）
            - 验证码有效期由 Redis 配置决定（默认 5 分钟）
            """
    )
    public ResponsePojo<Boolean> changeUserPassword(
        @Parameter(description = "HTTP 请求对象，用于从 Token 中获取用户信息", required = true) HttpServletRequest request,
        @Parameter(description = "新密码", required = true, example = "123456789") @RequestParam String newPassword,
        @Parameter(description = "确认新密码", required = true, example = "123456789") @RequestParam String confirmPassword,
        @Parameter(description = "邮箱验证码，6 位大写字母或数字", required = true, example = "ABCDEF") @RequestParam String vCode
    ) {
        // 从 Token 中获取用户 ID
        Integer userId = (Integer) request.getAttribute("userId");
        if (userId == null) {
            log.error("无法从 Token 中获取用户 ID");
            return ResponsePojo.error(false, "未授权访问：Token 无效");
        }

        // 根据用户 ID 查询用户信息
        User user = userService.selectAllUserById(userId);
        if (user == null) {
            log.error("用户不存在，用户 ID: {}", userId);
            return ResponsePojo.error(false, "用户不存在");
        }

        String email = user.getEmail();
        String oldPasswordHashed = user.getPassword();

        // 基础数据校验
        if (!RegexUtils.isVCode(vCode, 6)) {
            return ResponsePojo.error(false, "验证码格式错误");
        }

        // 验证码验证
        boolean isTrue = verificationCodeServices.verificationCodeVerify(email, vCode);
        if (!isTrue) {
            return ResponsePojo.error(false, "验证码错误");
        }

        // 校验两次输入的新密码是否一致
        if (!newPassword.equals(confirmPassword)) {
            return ResponsePojo.error(false, "两次输入的密码不一致");
        }

        // 密码加密
        String newPasswordHashed = StrSwitchUtils.PasswdToHash256(newPassword);

        // 校验新旧密码是否一致
        if (oldPasswordHashed.equals(newPasswordHashed)) {
            return ResponsePojo.error(false, "新旧密码不能一致");
        }

        // 修改密码
        Integer res = userService.changeUserLoginPasswordByEmail(email, oldPasswordHashed, newPasswordHashed);

        if (res != 1) {
            return ResponsePojo.error(false, "修改失败");
        }

        // 移除该用户的所有 Token（强制所有设备下线）
        int removedCount = tokenWhitelistService.removeAllUserTokens(userId, user.getUsername());
        log.info("用户密码修改成功，已移除用户所有 Token，用户 ID: {}, 用户名：{}, 移除数量：{}",
            userId, user.getUsername(), removedCount);

        return ResponsePojo.success(true, "修改成功");
    }

    /**
     * 忘记密码 - 重置密码接口
     *
     * @param usernameOrEmail 用户名或邮箱地址
     * @param newPassword     新密码
     * @param confirmPassword 确认新密码
     * @param vCode           邮箱验证码
     * @return 重置结果
     * @author Playereg
     */
    @PostMapping("/forgot")
    @Operation(
        summary = "忘记密码",
        description = """
            # 忘记密码（无需登录）

            ## 特性
            - 公开接口（无需 Token 认证）
            - 支持用户名或邮箱找回
            - 邮箱验证码验证
            - SHA-256 密码加密
            - 强制所有设备下线（移除所有 Token）

            ## 参数说明：
            - usernameOrEmail: **用户名**或**邮箱地址**，字符串类型，必填
            - newPassword: 新密码，字符串类型，必填，建议使用强密码组合
            - confirmPassword: 确认新密码，字符串类型，必填，需与新密码一致
            - vCode: 邮箱验证码，6 位大写字母或数字，字符串类型，必填

            ## 返回说明：
            - **重置成功**：返回 **{"data": true}** 和"密码重置成功，请使用新密码重新登录"提示
            - **用户名或邮箱格式错误**：返回 **{"data": false}** 和"用户名或邮箱格式错误"提示
            - **验证码格式错误**：返回 **{"data": false}** 和"验证码格式错误"提示
            - **验证码错误**：返回 **{"data": false}** 和"验证码错误"提示
            - **用户不存在**：返回 **{"data": false}** 和"用户不存在"提示
            - **两次密码不一致**：返回 **{"data": false}** 和"两次输入的密码不一致"提示
            - **重置失败**：返回 **{"data": false}** 和"密码重置失败"提示

            ## 业务逻辑：
            1. 校验用户名格式、邮箱格式、验证码格式
            2. 验证邮箱验证码是否正确（如提供用户名则先查询邮箱）
            3. 根据用户名或邮箱查询用户信息
            4. 校验两次输入的新密码是否一致
            5. 对新密码进行 SHA-256 哈希加密处理
            6. 更新数据库中的密码（不验证旧密码）
            7. **移除该用户的所有 Token**（强制所有设备下线）
            8. 返回重置结果

            ## 注意事项：
            - 此接口**无需登录**即可调用
            - 验证码发送到用户的注册邮箱
            - 密码会使用 **SHA-256** 进行加密存储
            - 支持使用用户名**或**邮箱找回密码
            - 建议使用强密码组合（大小写字母 + 数字 + 特殊字符）
            - 验证码有效期由 Redis 配置决定（默认 5 分钟）
            - **密码重置后，该用户的所有 Token 会被立即移除**，所有设备需要重新登录
            """
    )
    public ResponsePojo<Boolean> forgotPassword(
        @Parameter(description = "用户名或邮箱，6-16 位字母/数字/下划线或标准邮箱格式", required = true, example = "dev_user") @RequestParam String usernameOrEmail,
        @Parameter(description = "新密码，会使用 SHA-256 加密存储", required = true, example = "123456789") @RequestParam String newPassword,
        @Parameter(description = "确认新密码，需与新密码一致", required = true, example = "123456789") @RequestParam String confirmPassword,
        @Parameter(description = "邮箱验证码，6 位大写字母或数字", required = true, example = "ABCDEF") @RequestParam String vCode
    ) {
        // 基础数据校验
        if (!RegexUtils.isUsername(usernameOrEmail) && !RegexUtils.isEmail(usernameOrEmail)) {
            return ResponsePojo.error(false, "用户名或邮箱格式错误");
        }
        if (!RegexUtils.isVCode(vCode, 6)) {
            return ResponsePojo.error(false, "验证码格式错误");
        }

        // 验证码验证（使用邮箱作为 key）
        String emailForVcode = RegexUtils.isEmail(usernameOrEmail) ? usernameOrEmail : null;
        if (emailForVcode == null) {
            // 如果输入的是用户名，需要先从数据库查询邮箱
            User tempUser = userService.selectAllUserByUsername(usernameOrEmail);
            if (tempUser != null) {
                emailForVcode = tempUser.getEmail();
            }
        }

        if (emailForVcode != null) {
            boolean isTrue = verificationCodeServices.verificationCodeVerify(emailForVcode, vCode);
            if (!isTrue) {
                return ResponsePojo.error(false, "验证码错误");
            }
        }

        // 校验两次输入的新密码是否一致
        if (!newPassword.equals(confirmPassword)) {
            return ResponsePojo.error(false, "两次输入的密码不一致");
        }

        // 对密码进行加密
        String hashedPassword = StrSwitchUtils.PasswdToHash256(newPassword);
        log.info("新密码哈希：{}", hashedPassword);

        // 重置密码
        Boolean result = userService.resetPasswordByUsernameOrEmail(usernameOrEmail, hashedPassword, hashedPassword, vCode);

        if (!result) {
            log.warn("密码重置失败，用户名或邮箱：{}", usernameOrEmail);
            return ResponsePojo.error(false, "密码重置失败");
        }

        // 查询用户信息以获取 user_id 和 username
        User user;
        if (RegexUtils.isEmail(usernameOrEmail)) {
            user = userService.selectAllUserByEmail(usernameOrEmail);
        } else {
            user = userService.selectAllUserByUsername(usernameOrEmail);
        }

        // 移除该用户的所有 Token（强制所有设备下线）
        if (user != null && user.getUser_id() != null && user.getUsername() != null) {
            int removedCount = tokenWhitelistService.removeAllUserTokens(user.getUser_id(), user.getUsername());
            log.info("密码重置成功，已移除用户所有 Token，用户 ID: {}, 用户名：{}, 移除数量：{}",
                user.getUser_id(), user.getUsername(), removedCount);
        }

        log.info("密码重置成功，用户名或邮箱：{}", usernameOrEmail);
        return ResponsePojo.success(true, "密码重置成功，请使用新密码重新登录");
    }
}
