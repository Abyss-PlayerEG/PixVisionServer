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
import top.playereg.pix_vision.pojo.userPojo.UserLogin;
import top.playereg.pix_vision.service.TokenWhitelistService;
import top.playereg.pix_vision.service.UserService;
import top.playereg.pix_vision.service.VerificationCodeServices;
import top.playereg.pix_vision.util.JWTUtils;
import top.playereg.pix_vision.util.RegexUtils;
import top.playereg.pix_vision.util.StrSwitchUtils;

/**
 * 用户认证相关接口（登录、登出）
 *
 * @author PlayerEG
 * @see top.playereg.pix_vision.service.Impl.UserServiceImpl
 */
@RestController
@SuppressWarnings("all")
@RequestMapping("/api/user/auth")
@RequiredArgsConstructor
@Tag(name = "用户认证相关接口")
public class UserAuthController {
    private static final Logger log = LoggerFactory.getLogger(UserAuthController.class);

    private final UserService userService;
    private final VerificationCodeServices verificationCodeServices;
    private final TokenWhitelistService tokenWhitelistService;

    /**
     * 用户登录
     *
     * @param usernameOrEmail 用户名或邮箱
     * @param password        密码
     * @param vCode           验证码
     * @return 响应数据<UserLogin>，包含用户信息和 Token
     * @author PlayerEG
     */
    @PostMapping("/login")
    @Operation(
        summary = "用户登录接口",
        description = """
            # 用户登录

            ## 参数说明：
            - usernameOrEmail: **用户名**或**邮箱地址**，字符串类型，必填
            - password: 登录密码，字符串类型，必填
            - vCode: 邮箱验证码（6 位大写字母或数字），字符串类型，必填

            ## 返回说明：
            - **登录成功**：返回 **{"data": UserLogin 对象}** ，包含用户信息和 JWT Token
            - **用户名或邮箱格式错误**：返回 **{"data": null}** 和"用户名或邮箱格式错误"提示
            - **验证码格式错误**：返回 **{"data": null}** 和"验证码格式错误"提示
            - **验证码错误**：返回 **{"data": null}** 和"验证码错误"提示
            - **用户不存在**：返回 **{"data": null}** 和"用户不存在"提示
            - **用户名或密码错误**：返回 **{"data": null}** 和"用户名或密码错误"提示
            - **账户异常**：返回 **{"data": null}** 和"账户已被禁用"提示

            ## 业务逻辑：
            1. 校验用户名格式、邮箱格式、验证码格式
            2. 验证邮箱验证码是否正确（如提供用户名则先查询邮箱）
            3. 根据用户名或邮箱查询用户信息
            4. 验证密码是否正确（比对 SHA-256 哈希值）
            5. 检查用户状态是否正常（status=10 表示正常）
            6. 生成 JWT Token（有效期 7 天）
            7. 将 Token 加入白名单
            8. 返回用户信息和 Token

            ## 注意事项：
            - Token 有效期为 **7 天**
            - Token 需要保存在客户端，后续请求需在 Header 中携带
            - 建议使用 **HTTPS** 传输以保障安全
            - 密码会自动进行 SHA-256 哈希加密处理后再比对
            - 支持使用用户名**或**邮箱登录

            ## Token 使用方式：
            - Header 中添加：`Authorization: Bearer <token>`
            - 或者 URL 参数：`?token=<token>`
            """
    )
    public ResponsePojo<UserLogin> login(
        @Parameter(description = "用户名或邮箱，6-16 位字母/数字/下划线或标准邮箱格式", required = true, example = "dev_user") @RequestParam String usernameOrEmail,
        @Parameter(description = "登录密码，会使用 SHA-256 加密后比对", required = true, example = "123456") @RequestParam String password,
        @Parameter(description = "邮箱验证码，6 位大写字母或数字", required = true, example = "ABCDEF") @RequestParam String vCode
    ) {
        // 基础数据校验
        if (!RegexUtils.isUsername(usernameOrEmail) && !RegexUtils.isEmail(usernameOrEmail)) {
            return ResponsePojo.error(null, "用户名或邮箱格式错误");
        }
        if (!RegexUtils.isVCode(vCode, 6)) {
            return ResponsePojo.error(null, "验证码格式错误");
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
                return ResponsePojo.error(null, "验证码错误");
            }
        }

        // 对密码进行加密
        String hashedPassword = StrSwitchUtils.PasswdToHash256(password);
        log.info("登录密码哈希：{}", hashedPassword);

        // 查询用户信息（支持用户名或邮箱登录）
        User user;
        if (RegexUtils.isEmail(usernameOrEmail)) {
            user = userService.selectAllUserByEmail(usernameOrEmail);
            log.info("通过邮箱查询用户：{}, 结果：{}", usernameOrEmail, user != null ? "找到" : "未找到");
        } else {
            user = userService.selectAllUserByUsername(usernameOrEmail);
            log.info("通过用户名查询用户：{}, 结果：{}", usernameOrEmail, user != null ? "找到" : "未找到");
        }

        // 调试：输出完整用户对象
        log.info("查询到的用户对象：{}", user);
        log.info("user_id: {}, username: {}", user != null ? user.getUser_id() : "null", user != null ? user.getUsername() : "null");

        // 判断用户是否存在
        if (user == null) {
            log.warn("用户不存在：{}", usernameOrEmail);
            return ResponsePojo.error(null, "用户不存在");
        }

        // 验证用户 ID 和用户名不为空
        if (user.getUser_id() == null || user.getUsername() == null) {
            log.error("用户数据不完整 - user_id: {}, username: {}", user.getUser_id(), user.getUsername());
            throw new IllegalStateException("用户数据不完整，无法生成 Token");
        }

        // 输出数据库中的密码用于调试
        log.info("数据库密码：{}", user.getPassword());

        // 验证密码
        if (!hashedPassword.equals(user.getPassword())) {
            log.warn("密码错误，用户名：{}", usernameOrEmail);
            return ResponsePojo.error(null, "用户名或密码错误");
        }

        // 检查用户状态（status=10 表示正常）
        if (user.getStatus() != null && user.getStatus() != 10) {
            log.warn("账户已被禁用，用户名：{}, 状态：{}", usernameOrEmail, user.getStatus());
            return ResponsePojo.error(null, "账户已被禁用");
        }

        // 生成 JWT Token
        String token = JWTUtils.createToken(
            user.getUser_id(),
            user.getUsername()
        );
        log.info("生成 Token: {}", token);

        // 将 Token 加入白名单
        long tokenExpireTime = 7 * 24 * 60 * 60 * 1000L; // 7 天（毫秒）
        tokenWhitelistService.addToWhitelist(token, user.getUser_id(), user.getUsername(), tokenExpireTime);

        // 创建返回对象
        UserLogin userLogin = new UserLogin();
        userLogin.setUser_id(user.getUser_id());
        userLogin.setString_user_uuid(StrSwitchUtils.bytes2Uuid(user.getUser_uuid()));
        userLogin.setUsername(user.getUsername());
        userLogin.setNickname(user.getNickname());
        userLogin.setAvatar_url(user.getAvatar_url());
        userLogin.setEmail(user.getEmail());
        userLogin.setStatus(user.getStatus());
        userLogin.setUser_role(user.getUser_role());
        userLogin.setToken(token);

        log.info("用户登录成功：{}", usernameOrEmail);
        return ResponsePojo.success(userLogin, "登录成功");
    }

    /**
     * 用户登出（将 Token 从白名单移除）
     *
     * @param token JWT Token（从 Header 中获取）
     * @return 响应数据
     * @author PlayerEG
     */
    @PostMapping("/logout")
    @Operation(
        summary = "用户登出接口",
        description = """
            # 用户登出，将 Token 从白名单移除

            ## 参数说明：
            - Authorization: Header 中的 Token，格式为 `Bearer <token>`，或通过 URL 参数 `?token=<token>` 传递

            ## 返回说明：
            - **登出成功**：返回 **{"data": true}** 和"登出成功，Token 已被禁用"提示
            - **Token 不存在**：返回 **{"data": false}** 和"Token 不存在"提示
            - **Token 已失效**：返回 **{"data": false}** 和"Token 已失效"提示（Token 不在白名单中）
            - **Token 已过期**：返回 **{"data": false}** 和"Token 已过期"提示

            ## 业务逻辑：
            1. 从请求头或 URL 参数中提取 Token（支持 Bearer 前缀）
            2. 检查 Token 是否在白名单中
            3. 获取 Token 剩余有效期
            4. 将 Token 从白名单移除
            5. 记录登出日志
            6. 该 Token 将无法再访问任何受保护接口

            ## 注意事项：
            - Token 从白名单移除后，立即失效
            - 需要携带有效的 Token 才能登出
            - 如果 Token 已过期或不在白名单中，会返回相应提示
            - 同一用户的其他设备 Token 不受影响
            - Token 有效期为 **7 天**
            """
    )
    public ResponsePojo<Boolean> logout(
        @Parameter(description = "HTTP 请求对象，用于从 Header 或 URL 参数中获取 Token", required = true) HttpServletRequest request
    ) {
        // 优先从 URL 参数获取 Token
        String token = request.getParameter("token");

        // 如果 URL 参数中没有，尝试从 Header 获取
        if (token == null || token.isEmpty()) {
            String authHeader = request.getHeader("Authorization");
            log.debug("登出接口 - Authorization Header: {}", authHeader);

            if (authHeader != null && !authHeader.isEmpty()) {
                // 支持两种格式：带 Bearer 前缀 或 不带前缀
                if (authHeader.startsWith("Bearer ")) {
                    token = authHeader.substring(7); // 去除 "Bearer " 前缀
                } else {
                    token = authHeader; // 直接使用（假设就是 Token）
                }
            }
        }

        log.debug("登出接口 - 提取的 Token: {}", token != null ? (token.length() > 10 ? token.substring(0, 10) + "..." : token) : "null");

        if (token == null || token.isEmpty()) {
            log.error("登出失败 - Token 不存在，Authorization: {}, Token 参数：{}", request.getHeader("Authorization"), request.getParameter("token"));
            return ResponsePojo.error(false, "Token 不存在，请在 Header 中添加 Authorization: Bearer <token> 或在 URL 参数中添加 ?token=<token>");
        }

        // 检查 Token 是否在白名单中
        if (!tokenWhitelistService.isInWhitelist(token)) {
            log.warn("Token 不在白名单中，可能已过期或被移除");
            return ResponsePojo.success(false, "Token 已失效");
        }

        // 获取 Token 剩余有效期
        long remainingTime = JWTUtils.getTokenRemainingTime(token);
        if (remainingTime <= 0) {
            log.warn("Token 已过期，无需从白名单移除");
            tokenWhitelistService.removeFromWhitelist(token);
            return ResponsePojo.success(false, "Token 已过期");
        }

        // 将 Token 从白名单移除
        tokenWhitelistService.removeFromWhitelist(token);

        // 提取用户信息用于日志
        Integer userId = JWTUtils.getUserIdFromToken(token);
        String username = JWTUtils.getUsernameFromToken(token);
        log.info("用户登出，用户名：{}, 用户 ID: {}, Token 剩余时间：{}ms", username, userId, remainingTime);

        return ResponsePojo.success(true, "登出成功，Token 已被禁用");
    }
}
