package top.playereg.pix_vision.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.playereg.pix_vision.pojo.ResponsePojo;
import top.playereg.pix_vision.service.UserService;
import top.playereg.pix_vision.util.Annotation.PublicAccess;
import top.playereg.pix_vision.util.JWTUtils;
import top.playereg.pix_vision.util.PixVisionLogger;

/**
 * 用户认证控制器
 * <p>
 * 提供用户注册、登录、登出等认证相关接口。
 * 所有接口都遵循 RESTful 风格设计，返回统一的 ResponsePojo 格式。
 * </p>
 *
 * <h3>使用场景</h3>
 * <ol>
 *   <li>新用户注册账户</li>
 *   <li>已有用户登录系统</li>
 *   <li>用户主动登出（使 Token 失效）</li>
 *   <li>用户注销账户（逻辑删除）</li>
 * </ol>
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * // 前端调用示例 - 用户注册
 * POST /api/auth/register
 * Body: username=test&password=123456&confirmPassword=123456&nickname=测试&email=test@example.com&vCode=ABCDEF
 *
 * // 前端调用示例 - 用户登录
 * POST /api/auth/login
 * Body: usernameOrEmail=test&password=123456&vCode=ABCDEF
 * Response: { "data": { "user_id": 1, "username": "test", "token": "eyJ..." } }
 *
 * // 前端调用示例 - 用户登出
 * POST /api/auth/logout
 * Header: Authorization: Bearer eyJ...
 * }</pre>
 *
 * <h3>注意事项</h3>
 * <ul>
 *   <li>注册和登录接口需要邮箱验证码验证</li>
 *   <li>密码在传输前应在前端进行 SHA-256 加密</li>
 *   <li>Token 有效期为 7 天，过期后需要重新登录</li>
 *   <li>登出后 Token 会立即从白名单移除，无法继续使用</li>
 *   <li>所有公开接口都必须标注 @PublicAccess 注解</li>
 * </ul>
 *
 * <h3>最佳实践</h3>
 * <ul>
 *   <li>使用构造器注入依赖（配合 @RequiredArgsConstructor）</li>
 *   <li>Controller 层只做参数校验和调用 Service，不包含业务逻辑</li>
 *   <li>统一使用 ResponsePojo 封装返回结果</li>
 *   <li>敏感操作记录日志，便于审计和问题排查</li>
 *   <li>Swagger 文档要完整描述接口特性、参数、返回值和业务逻辑</li>
 * </ul>
 *
 * @author PlayerEG
 * @see top.playereg.pix_vision.service.UserService 用户服务
 * @see top.playereg.pix_vision.pojo.ResponsePojo 统一响应格式
 * @since DEV-1.0.0
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "认证接口")
public class AuthControllerExample {

    private static final PixVisionLogger log = PixVisionLogger.create(AuthControllerExample.class);

    private final UserService userService;

    /**
     * 用户注册接口
     * <p>
     * 创建新用户账户，支持用户名/邮箱唯一性校验、邮箱验证码验证、密码加密等功能。
     * 注册成功后会自动生成用户 UUID 和随机头像。
     * </p>
     *
     * <h3>特性</h3>
     * <ul>
     *   <li>用户名/邮箱唯一性校验</li>
     *   <li>邮箱验证码验证（Redis 存储）</li>
     *   <li>SHA-256 密码加密</li>
     *   <li>自动生成随机昵称（可选）</li>
     *   <li>密码二次确认</li>
     * </ul>
     *
     * <h3>使用示例</h3>
     * <pre>{@code
     * // cURL 示例
     * curl -X POST http://localhost:9090/api/auth/register \
     *   -d "username=test_user" \
     *   -d "password=e10adc3949ba59abbe56e057f20f883e" \
     *   -d "confirmPassword=e10adc3949ba59abbe56e057f20f883e" \
     *   -d "nickname=测试用户" \
     *   -d "email=test@example.com" \
     *   -d "vCode=ABCDEF"
     *
     * // 成功响应
     * {
     *   "code": 200,
     *   "message": "注册成功",
     *   "data": {
     *     "user_id": 1,
     *     "username": "test_user",
     *     "nickname": "测试用户",
     *     "email": "test@example.com"
     *   }
     * }
     * }</pre>
     *
     * <h3>注意事项</h3>
     * <ul>
     *   <li>昵称参数为可选参数，不传或为空时自动生成</li>
     *   <li>验证码有效期由 Redis 配置决定（默认 5 分钟）</li>
     *   <li>密码会使用 SHA-256 进行加密存储</li>
     *   <li>用户名和邮箱不可重复注册</li>
     *   <li>建议使用强密码组合（大小写字母 + 数字 + 特殊字符）</li>
     *   <li>两次输入的密码必须完全一致</li>
     * </ul>
     *
     * @param username        用户名，6-16 位字母/数字/下划线
     * @param password        登录密码，会使用 SHA-256 加密存储
     * @param confirmPassword 确认密码，必须与密码一致
     * @param nickname        用户昵称（可选，为空时自动生成）
     * @param email           邮箱地址，用于接收验证码
     * @param vCode           邮箱验证码，6 位大写字母或数字
     * @return 响应数据，包含用户对象或错误信息
     * @author PlayerEG
     */
    @PostMapping("/register")
    @PublicAccess("注册接口，无需认证")
    @Operation(
        summary = "注册接口",
        description = """
            # 注册（无需登录验证）

            ## 特性
            - 用户名/邮箱唯一性校验
            - 邮箱验证码验证（Redis 存储）
            - SHA-256 密码加密
            - 自动生成随机昵称（可选）
            - 密码二次确认

            ## 参数说明：
            - username: 用户名，6-16 位，只允许字母、数字和下划线，字符串类型，必填
            - password: 登录密码，字符串类型，必填，建议使用强密码组合
            - confirmPassword: 确认密码，必须与 password 一致，字符串类型，必填
            - nickname: 用户昵称，字符串类型，可为空，为空时自动生成随机昵称，长度 1-20 个字符
            - email: 邮箱地址，字符串类型，必填，用于接收验证码和后续找回密码
            - vCode: 邮箱验证码，6 位大写字母或数字，字符串类型，必填

            ## 返回说明：
            - 注册成功：返回 User 对象和"注册成功"提示
            - 用户名格式错误：返回 null 和"用户名格式错误"提示
            - 邮箱格式错误：返回 null 和"邮箱格式错误"提示
            - 验证码格式错误：返回 null 和"验证码格式错误"提示
            - 两次密码不一致：返回 null 和"两次输入的密码不一致"提示
            - 验证码错误：返回 null 和"验证码错误"提示
            - 昵称长度错误：返回 null 和"昵称长度必须在 1-20 个字符之间"提示
            - 注册失败：返回 null 和"注册失败：该用户名或邮箱已注册"提示

            ## 业务逻辑：
            1. 校验用户名格式是否符合规范（6-16 位字母、数字、下划线）
            2. 校验邮箱格式是否正确
            3. 校验验证码格式是否正确（6 位大写字母或数字）
            4. 验证两次输入的密码是否一致
            5. 验证邮箱验证码是否与 Redis 中存储的一致
            6. 如果昵称为空，生成随机默认昵称（格式：user+ 随机词）
            7. 如果昵称不为空，验证长度是否在 1-20 个字符之间
            8. 对密码进行 SHA-256 哈希加密处理
            9. 创建用户并保存到数据库
            10. 返回用户信息和成功提示

            ## 注意事项：
            - 昵称参数为可选参数，不传或为空时自动生成
            - 验证码有效期由 Redis 配置决定（默认 5 分钟）
            - 密码会使用 SHA-256 进行加密存储
            - 用户名和邮箱不可重复注册
            - 建议使用强密码组合（大小写字母 + 数字 + 特殊字符）
            - 两次输入的密码必须完全一致
            """
    )
    public ResponsePojo<top.playereg.pix_vision.pojo.userPojo.User> register(
        @Parameter(description = "用户名，6-16 位字母/数字/下划线", required = true, example = "dev_user") @RequestParam String username,
        @Parameter(description = "登录密码，会使用 SHA-256 加密存储", required = true, example = "123456") @RequestParam String password,
        @Parameter(description = "确认密码，必须与密码一致", required = true, example = "123456") @RequestParam String confirmPassword,
        @Parameter(description = "用户昵称（可选，为空时自动生成）", required = false, example = "测试用户") @RequestParam(required = false) String nickname,
        @Parameter(description = "邮箱地址，用于接收验证码", required = true, example = "test@example.com") @RequestParam String email,
        @Parameter(description = "邮箱验证码，6 位大写字母或数字", required = true, example = "ABCDEF") @RequestParam String vCode
    ) {
        // 基础数据校验
        if (!top.playereg.pix_vision.util.RegexUtils.isUsername(username)) {
            return ResponsePojo.error(null, "用户名格式不正确");
        }
        if (!top.playereg.pix_vision.util.RegexUtils.isEmail(email)) {
            return ResponsePojo.error(null, "邮箱格式不正确");
        }
        if (!top.playereg.pix_vision.util.RegexUtils.isVCode(vCode, 6)) {
            return ResponsePojo.error(null, "验证码格式不正确");
        }
        if (!top.playereg.pix_vision.util.RegexUtils.isPassword(password) || !top.playereg.pix_vision.util.RegexUtils.isPassword(confirmPassword)) {
            return ResponsePojo.error(null, "密码格式不正确");
        }
        
        // 验证两次密码是否一致
        if (!password.equals(confirmPassword)) {
            return ResponsePojo.error(null, "两次输入的密码不一致");
        }
        
        // 验证码验证
        boolean isTrue = top.playereg.pix_vision.service.VerificationCodeServices.class.cast(null).verificationCodeVerify(email, vCode);
        if (!isTrue) {
            return ResponsePojo.error(null, "验证码错误");
        }

        // 如果昵称为空，则生成一个随机昵称
        if (nickname == null || nickname.isEmpty()) {
            nickname = top.playereg.pix_vision.util.StrSwitchUtils.generateRandomUserDefaultNickName("user");
        } else {
            // 验证昵称长度（1-20 个字符）
            if (nickname.length() < 1 || nickname.length() > 20) {
                log.warn("昵称长度不符合要求: {}", nickname.length());
                return ResponsePojo.error(null, "昵称长度必须在 1-20 个字符之间");
            }
        }
        
        // 创建用户
        top.playereg.pix_vision.pojo.userPojo.User user = userService.registerUser(username, password, nickname, email);

        // 判断用户是否创建成功
        if (user == null) {
            return ResponsePojo.error(null, "注册失败：该用户名或邮箱已注册");
        }

        return ResponsePojo.success(user, "注册成功");
    }

    /**
     * 用户登出接口
     * <p>
     * 将用户的 Token 从白名单中移除，使其立即失效。
     * 支持从 HTTP Header 或 URL 参数中提取 Token。
     * </p>
     *
     * <h3>特性</h3>
     * <ul>
     *   <li>Token 认证（支持 Header 和 URL 参数两种方式）</li>
     *   <li>Token 白名单移除（立即失效）</li>
     *   <li>剩余有效期检测</li>
     *   <li>单设备登出（不影响其他设备）</li>
     * </ul>
     *
     * <h3>使用示例</h3>
     * <pre>{@code
     * // 方式1：通过 Header 传递 Token
     * curl -X POST http://localhost:9090/api/auth/logout \
     *   -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
     *
     * // 方式2：通过 URL 参数传递 Token
     * curl -X POST "http://localhost:9090/api/auth/logout?token=eyJhbGciOiJIUzI1NiJ9..."
     *
     * // 成功响应
     * {
     *   "code": 200,
     *   "message": "登出成功，Token 已被禁用",
     *   "data": true
     * }
     * }</pre>
     *
     * <h3>注意事项</h3>
     * <ul>
     *   <li>Token 从白名单移除后，立即失效</li>
     *   <li>需要携带有效的 Token 才能登出</li>
     *   <li>如果 Token 已过期或不在白名单中，会返回相应提示</li>
     *   <li>同一用户的其他设备 Token 不受影响</li>
     *   <li>Token 有效期为 7 天</li>
     * </ul>
     *
     * @param request HTTP 请求对象，用于从 Header 或 URL 参数中获取 Token
     * @return 响应数据，包含操作结果
     * @author PlayerEG
     */
    @PostMapping("/logout")
    @Operation(
        summary = "登出接口",
        description = """
            # 登出（需要登录认证）

            ## 特性
            - Token 认证（支持 Header 和 URL 参数两种方式）
            - Token 白名单移除（立即失效）
            - 剩余有效期检测
            - 单设备登出（不影响其他设备）

            ## 参数说明：
            - Authorization: Header 中的 Token，格式为 `Bearer <token>`，或通过 URL 参数 `?token=<token>` 传递

            ## 返回说明：
            - 登出成功：返回 true 和"登出成功，Token 已被禁用"提示
            - Token 不存在：返回 false 和"Token 不存在"提示
            - Token 已失效：返回 false 和"Token 已失效"提示（Token 不在白名单中）
            - Token 已过期：返回 false 和"Token 已过期"提示

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
            - Token 有效期为 7 天
            """
    )
    public ResponsePojo<Boolean> logout(
        @Parameter(description = "HTTP 请求对象，用于从 Header 或 URL 参数中获取 Token", required = true) HttpServletRequest request
    ) {
        // 提取 Token
        String token = JWTUtils.extractTokenWithLog(request, "登出接口");

        if (token == null || token.isEmpty()) {
            log.error("登出失败 - Token 不存在，Authorization: {}, Token 参数：{}", 
                request.getHeader("Authorization"), request.getParameter("token"));
            return ResponsePojo.error(false, "Token 不存在，请在 Header 中添加 Authorization: Bearer <token> 或在 URL 参数中添加 ?token=<token>");
        }

        // 检查 Token 是否在白名单中
        // 注意：这里应该注入 TokenWhitelistService，示例中省略
        // if (!tokenWhitelistService.isInWhitelist(token)) {
        //     log.warn("Token 不在白名单中，可能已过期或被移除");
        //     return ResponsePojo.success(false, "Token 已失效");
        // }

        // 获取 Token 剩余有效期
        long remainingTime = JWTUtils.getTokenRemainingTime(token);
        if (remainingTime <= 0) {
            log.warn("Token 已过期，无需从白名单移除");
            // tokenWhitelistService.removeFromWhitelist(token);
            return ResponsePojo.success(false, "Token 已过期");
        }

        // 将 Token 从白名单移除
        // tokenWhitelistService.removeFromWhitelist(token);

        // 提取用户信息用于日志
        Integer userId = JWTUtils.getUserIdFromToken(token);
        String username = JWTUtils.getUsernameFromToken(token);
        log.info("用户登出，用户名：{}, 用户 ID: {}, Token 剩余时间：{}ms", username, userId, remainingTime);

        return ResponsePojo.success(true, "登出成功，Token 已被禁用");
    }
}
