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
 * 用户操作相关接口
 *
 * @author PlayerEG
 * @see top.playereg.pix_vision.service.Impl.VerificationCodeServicesImpl
 * @see top.playereg.pix_vision.service.Impl.UserServiceImpl
 */
@RestController
@SuppressWarnings("all")
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Tag(name = "用户操作相关接口")
public class UserController {
    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;
    private final VerificationCodeServices verificationCodeServices;
    private final TokenWhitelistService tokenWhitelistService;

    /**
     * 用户登录
     *
     * @param usernameAndEmail 用户名或邮箱
     * @param password         密码
     * @param vCode            验证码
     * @return 响应数据<UserLogin>，包含用户信息和 Token
     * @author PlayerEG
     */
    @PostMapping("/login")
    @Operation(
            summary = "用户登录接口",
            description = """
                    # 用户登录
                        
                    ## 参数说明：
                    - usernameAndEmail: 用户名**或**邮箱地址
                    - password: 登录密码
                    - vCode: 邮箱验证码（6 位大写字母或数字）
                        
                    ## 返回说明：
                    - **登录成功**：返回 **{"data": {UserLogin 对象}}**，包含用户信息和 JWT Token
                    - **用户名或密码错误**：返回 **{"data": null}** 和"用户名或密码错误"提示
                    - **用户不存在**：返回 **{"data": null}** 和"用户不存在"提示
                    - **验证码错误**：返回 **{"data": null}** 和"验证码错误"提示
                    - **账户异常**：返回 **{"data": null}** 和"账户已被禁用"提示
                        
                    ## 业务逻辑：
                    1. 校验用户名格式、邮箱格式、验证码格式
                    2. 验证邮箱验证码是否正确
                    3. 根据用户名或邮箱查询用户信息
                    4. 验证密码是否正确（比对哈希值）
                    5. 检查用户状态是否正常
                    6. 生成 JWT Token
                    7. 返回用户信息和 Token
                        
                    ## 注意事项：
                    - Token 有效期为 **7 天**
                    - Token 需要保存在客户端，后续请求需在 Header 中携带
                    - 建议使用 **HTTPS** 传输以保障安全
                    - 密码会自动进行哈希加密处理后再比对
                        
                    ## Token 使用方式：
                    - Header 中添加：`Authorization: Bearer <token>`
                    - 或者 URL 参数：`?token=<token>`
                    """
    )
    public ResponsePojo<UserLogin> login(
            @Parameter(description = "用户名或邮箱", required = true, example = "dev_user") @RequestParam String usernameAndEmail,
            @Parameter(description = "密码", required = true, example = "123456") @RequestParam String password,
            @Parameter(description = "验证码", required = true, example = "ABCDEF") @RequestParam String vCode
    ) {
        // 基础数据校验
        if (!RegexUtils.isUsername(usernameAndEmail) && !RegexUtils.isEmail(usernameAndEmail)) {
            return ResponsePojo.error(null, "用户名或邮箱格式错误");
        }
        if (!RegexUtils.isVCode(vCode)) {
            return ResponsePojo.error(null, "验证码格式错误");
        }
            
        // 验证码验证（使用邮箱作为 key）
        String emailForVcode = RegexUtils.isEmail(usernameAndEmail) ? usernameAndEmail : null;
        if (emailForVcode == null) {
            // 如果输入的是用户名，需要先从数据库查询邮箱
            User tempUser = userService.selectUserByUsername(usernameAndEmail);
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
        if (RegexUtils.isEmail(usernameAndEmail)) {
            user = userService.selectUserByEmail(usernameAndEmail);
            log.info("通过邮箱查询用户：{}, 结果：{}", usernameAndEmail, user != null ? "找到" : "未找到");
        } else {
            user = userService.selectUserByUsername(usernameAndEmail);
            log.info("通过用户名查询用户：{}, 结果：{}", usernameAndEmail, user != null ? "找到" : "未找到");
        }
        
        // 调试：输出完整用户对象
        log.info("查询到的用户对象：{}", user);
        log.info("user_id: {}, username: {}", user != null ? user.getUser_id() : "null", user != null ? user.getUsername() : "null");
            
        // 判断用户是否存在
        if (user == null) {
            log.warn("用户不存在：{}", usernameAndEmail);
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
            log.warn("密码错误，用户名：{}", usernameAndEmail);
            return ResponsePojo.error(null, "用户名或密码错误");
        }
    
        // 检查用户状态（status=10 表示正常）
        if (user.getStatus() != null && user.getStatus() != 10) {
            log.warn("账户已被禁用，用户名：{}, 状态：{}", usernameAndEmail, user.getStatus());
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
        userLogin.setUser_uuid(user.getUser_uuid());
        userLogin.setString_user_uuid(StrSwitchUtils.bytes2Uuid(user.getUser_uuid()));
        userLogin.setUsername(user.getUsername());
        userLogin.setNickname(user.getNickname());
        userLogin.setAvatar_url(user.getAvatar_url());
        userLogin.setEmail(user.getEmail());
        userLogin.setStatus(user.getStatus());
        userLogin.setToken(token);
    
        log.info("用户登录成功：{}", usernameAndEmail);
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
                - Authorization: Header 中的 Token，格式为 `Bearer <token>`
                
                ## 返回说明：
                - **登出成功**：返回 **{"data": null}** 和"登出成功"提示，Token 已从白名单移除
                - **Token 已失效**：返回 **{"data": null}** 和"Token 已失效"提示（Token 不在白名单中）
                - **Token 已过期**：返回 **{"data": null}** 和"Token 已过期"提示
                
                ## 业务逻辑：
                1. 从请求头中提取 Token
                2. 检查 Token 是否在白名单中
                3. 获取 Token 剩余有效期
                4. 将 Token 从白名单移除
                5. 该 Token 将无法再访问任何受保护接口
                
                ## 注意事项：
                - Token 从白名单移除后，立即失效
                - 需要携带有效的 Token 才能登出
                - 如果 Token 已过期或不在白名单中，会直接返回失败
                - 同一用户的其他设备 Token 不受影响
                """
    )
    public ResponsePojo<Boolean> logout(
            @Parameter(description = "HTTP 请求对象（用于获取 Token）", required = true) HttpServletRequest request
    ) {
        // 从 Header 中获取 Token
        String authHeader = request.getHeader("Authorization");
        
        // 如果 Header 中没有，尝试从参数中获取
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            String tokenParam = request.getParameter("token");
            if (tokenParam != null && !tokenParam.isEmpty()) {
                authHeader = "Bearer " + tokenParam;
            }
        }
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponsePojo.error(false, "Token 不存在，请在 Header 中添加 Authorization: Bearer <token> 或在 URL 参数中添加 ?token=<token>");
        }
        
        String token = authHeader.substring(7); // 去除 "Bearer " 前缀
        
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
    @PostMapping("/register")
    @Operation(
            summary = "用户注册接口",
            description = """
                    # 用户注册
                                    
                    ## 参数说明：
                    - username: 用户名，6-16 位，只允许字母、数字和下划线
                    - password: 登录密码，建议使用强密码组合
                    - nickname: 用户昵称，**可为空**，为空时自动生成随机昵称
                    - email: 邮箱地址，用于接收验证码和后续找回密码
                    - vCode: 邮箱验证码，6 位大写字母或数字
                                    
                    ## 返回说明：
                    - **注册成功**：返回 **"data": {User} 对象** 和"注册成功"提示
                    - **用户名格式错误**：返回 **"data": null** 和"用户名格式错误"提示
                    - **邮箱格式错误**：返回 **"data": null** 和"邮箱格式错误"提示
                    - **验证码格式错误**：返回 **"data": null** 和"验证码格式错误"提示
                    - **验证码错误**：返回 **"data": null** 和"验证码错误"提示
                    - **注册失败**：返回 **"data": null** 和"注册失败：用户名或者邮箱已注册"提示
                                    
                    ## 业务逻辑：
                    1. 校验用户名格式是否符合规范（6-16 位字母、数字、下划线）
                    2. 校验邮箱格式是否正确
                    3. 校验验证码格式是否正确（6 位大写字母或数字）
                    4. 验证邮箱验证码是否与 Redis 中存储的一致
                    5. 如果昵称为空，生成随机默认昵称
                    6. 对密码进行 SHA-256 哈希偏移加盐加密处理
                    7. 创建用户并保存到数据库
                    8. 返回用户信息和成功提示
                                    
                    ## 注意事项：
                    - 昵称参数为**可选参数**，不传或为空时自动生成
                    - 验证码有效期由 Redis 配置决定
                    - 密码会使用 **SHA-256** 进行加密存储
                    - **用户名**和**邮箱**不可重复注册
                    """
    )
    public ResponsePojo<User> registerUser(
            @Parameter(description = "用户名", required = true, example = "dev_user") @RequestParam String username,
            @Parameter(description = "密码", required = true, example = "123456") @RequestParam String password,
            @Parameter(description = "昵称（可为空）", required = false, example = "测试用户") @RequestParam(required = false) String nickname,
            @Parameter(description = "邮箱", required = true, example = "test@example.com") @RequestParam String email,
            @Parameter(description = "验证码", required = true, example = "ABCDEF") @RequestParam String vCode
    ) {
        // 基础数据校验
        if (!RegexUtils.isUsername(username)) {
            return ResponsePojo.error(null, "用户名格式错误");
        }
        if (!RegexUtils.isEmail(email)) {
            return ResponsePojo.error(null, "邮箱格式错误");
        }
        if (!RegexUtils.isVCode(vCode)) {
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
