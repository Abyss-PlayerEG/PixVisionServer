package top.playereg.pix_vision.controller;

import cn.hutool.core.util.StrUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.playereg.pix_vision.pojo.ResponsePojo;
import top.playereg.pix_vision.pojo.entity.user.User;
import top.playereg.pix_vision.service.*;
import top.playereg.pix_vision.util.Annotation.PublicAccess;
import top.playereg.pix_vision.util.PixVisionLogger;
import top.playereg.pix_vision.util.RegexUtils;

/**
 * 邮件服务接口
 *
 * @author PlayerEG
 * @see ResponsePojo 响应结果
 * @see top.playereg.pix_vision.service.Impl.UserServiceImpl 用户服务
 * @see top.playereg.pix_vision.service.Impl.VerificationCodeServicesImpl 验证码服务
 * @see top.playereg.pix_vision.service.Impl.EmailServiceImpl 邮件服务
 * @see top.playereg.pix_vision.service.Impl.EmailTemplateServiceImpl 邮件模板服务
 */
@RestController
@SuppressWarnings("all")
@RequestMapping("/api/mail")
@RequiredArgsConstructor
@Tag(name = "邮件服务接口", description = "提供验证码发送、邮箱验证等邮件相关功能")
public class EmailController {

    private static final PixVisionLogger log = PixVisionLogger.create(EmailController.class);
    private final EmailService emailService;
    private final UserService userService;
    private final VerificationCodeServices verificationCodeServices;
    private final EmailTemplateService emailTemplateService;
    private final TokenWhitelistService tokenWhitelistService;

    /**
     * 发送注册验证码邮件
     *
     * @param email    邮箱地址（标准格式）
     * @param username 用户昵称（6-16位字母/数字/下划线）
     * @return 响应数据，表示邮件是否发送成功
     * @author PlayerEG
     */
    @Operation(
        summary = "发送注册验证码邮件",
        description = """
            # 发送注册验证码邮件（无需登录认证）

            ## 特性
            - 邮箱格式验证
            - Redis 验证码存储
            - HTML 邮件模板渲染
            - 自动生成6位随机验证码
            - 内置邮件主题

            ## 参数说明：
            - email: 邮箱地址，字符串类型，必填，必须是标准邮箱格式
            - username: 用户昵称，字符串类型，必填，用于邮件内容展示

            ## 返回说明：
            - **发送成功**：返回 **{"data": true}** 和“邮件发送成功”提示
            - **发送失败**：返回 **{"data": false}** 和“邮件发送失败”提示
            - **邮箱格式错误**：返回 **{"data": false}** 和“邮箱格式错误”提示
            - **用户名格式错误**：返回 **{"data": false}** 和“用户名格式错误”提示
            - **验证码已存在**：返回 **{"data": {"remainingSeconds": 剩余秒数, "message": "提示信息"}}** 和“验证码已存在，请检查邮箱或稍后重试”提示

            ## 业务逻辑：
            1. 验证邮箱地址格式是否正确
            2. 验证用户名格式是否正确（6-16位字母/数字/下划线）
            3. 检查Redis中是否已有该邮箱的未过期验证码
            4. 生成6位随机验证码并存入Redis
            5. 使用HTML邮件模板渲染邮件内容
            6. 发送邮件并将验证码与邮箱绑定存储

            ## 注意事项：
            - 验证码默认有效期由Redis配置决定（默认5分钟）
            - 用户名格式：6-16位字母、数字、下划线
            - 邮箱格式：标准邮箱格式
            - 建议设置合理的邮件发送频率限制
            - 邮件主题已内置为“PixVision 注册验证码”
            - **如果邮箱已有未过期的验证码，将拒绝发送新的验证码**
            """
    )
    @PostMapping("/send-register-code")
    @PublicAccess("发送注册验证码，无需认证")
    public ResponsePojo<Boolean> sendRegisterCode(
        @Parameter(description = "收件人邮箱地址", required = true, example = "test@example.com") @RequestParam String email,
        @Parameter(description = "用户昵称", required = true, example = "dev_user") @RequestParam String username
    ) {
        // 验证邮箱格式
        if (!RegexUtils.isEmail(email)) {
            return ResponsePojo.error(false, "邮箱格式错误");
        }

        // 验证用户名格式
        if (username == null || username.isEmpty()) {
            return ResponsePojo.error(false, "用户名不能为空");
        }
        if (!RegexUtils.isUsername(username)) {
            return ResponsePojo.error(false, "用户名格式错误");
        }

        log.info("发送注册验证码，用户名：{}，邮箱：{}", username, email);

        // 调用一站式验证码邮件发送方法
        EmailService.VerificationEmailResult result = emailService.sendVerificationEmail(email, username, "注册验证", "PixVision 注册验证码");
        if (!result.isSuccess()) {
            if (result.getExistingCodeRemainingSeconds() != null) {
                return ResponsePojo.error(false, StrUtil.format("验证码已存在，请检查邮箱或{}秒稍后重试", result.getExistingCodeRemainingSeconds()));
            }
            return ResponsePojo.error(false, result.getErrorMessage());
        }

        return ResponsePojo.success(true, "邮件发送成功");
    }

    /**
     * 发送登录验证码邮件
     *
     * @param usernameOrEmail 用户名或邮箱地址
     * @return 响应数据，表示邮件是否发送成功
     * @author PlayerEG
     */
    @PostMapping("/send-login-code")
    @PublicAccess("发送登录验证码，无需认证")
    @Operation(
        summary = "发送登录验证码邮件",
        description = """
            # 发送登录验证码邮件（无需登录认证）

            ## 特性
            - 支持用户名或邮箱作为收件人
            - 自动从数据库查询用户信息
            - Redis 验证码存储
            - HTML 邮件模板渲染
            - 内置邮件主题

            ## 参数说明：
            - usernameOrEmail: 用户名或邮箱地址，字符串类型，必填
              - 支持用户名格式：6-16位字母/数字/下划线
              - 支持邮箱格式：标准邮箱格式
              - 系统会自动识别并查询对应的邮箱地址

            ## 返回说明：
            - **发送成功**：返回 **{"data": true}** 和“邮件发送成功”提示
            - **发送失败**：返回 **{"data": false}** 和“邮件发送失败”提示
            - **用户不存在**：返回 **{"data": false}** 和“用户不存在，请先注册”提示
            - **格式错误**：返回 **{"data": false}** 和“用户名或邮箱格式错误”提示

            ## 业务逻辑：
            1. 判断usernameOrEmail参数是用户名还是邮箱
            2. 从数据库查询用户信息，获取用户名和邮箱
            3. 验证用户是否存在
            4. 检查Redis中是否已有该邮箱的未过期验证码
            5. 生成6位随机验证码并存入Redis
            6. 使用HTML邮件模板渲染邮件内容
            7. 发送邮件并将验证码与邮箱绑定存储

            ## 注意事项：
            - 验证码默认有效期由Redis配置决定（默认5分钟）
            - 如果usernameOrEmail是用户名，系统会自动查询对应的邮箱
            - 如果usernameOrEmail是邮箱，直接使用
            - 用户必须已注册才能发送登录验证码
            - 邮件主题已内置为"PixVision 登录验证码"
            - **如果邮箱已有未过期的验证码，将拒绝发送新的验证码**
            """
    )
    public ResponsePojo<Boolean> sendLoginCode(
        @Parameter(description = "用户名或邮箱地址", required = true, example = "dev_user") @RequestParam String usernameOrEmail
    ) {
        // 基础数据校验
        if (!RegexUtils.isUsername(usernameOrEmail) && !RegexUtils.isEmail(usernameOrEmail)) {
            return ResponsePojo.error(null, "用户名或邮箱格式错误");
        }
        // 使用通用方法查询用户
        User user = userService.selectUserByUsernameOrEmail(usernameOrEmail);

        // 检查用户是否存在
        if (user == null) {
            log.warn("登录场景，用户不存在：{}", usernameOrEmail);
            return ResponsePojo.error(false, "用户不存在，请先注册");
        }

        String username = user.getUsername();
        String targetEmail = user.getEmail();
        log.info("发送登录验证码，用户名：{}，邮箱：{}", username, targetEmail);

        // 调用一站式验证码邮件发送方法
        EmailService.VerificationEmailResult result = emailService.sendVerificationEmail(targetEmail, username, "登录验证", "PixVision 登录验证码");
        if (!result.isSuccess()) {
            if (result.getExistingCodeRemainingSeconds() != null) {
                return ResponsePojo.error(false, StrUtil.format("验证码已存在，请检查邮箱或{}秒稍后重试", result.getExistingCodeRemainingSeconds()));
            }
            return ResponsePojo.error(false, result.getErrorMessage());
        }

        return ResponsePojo.success(true, "邮件发送成功");
    }

    /**
     * 发送重置密码验证码邮件
     *
     * @param usernameOrEmail 用户名或邮箱地址
     * @return 响应数据，表示邮件是否发送成功
     * @author PlayerEG
     */
    @PostMapping("/send-forget-password-code")
    @PublicAccess("发送重置密码验证码，无需认证")
    @Operation(
        summary = "发送重置密码验证码邮件",
        description = """
            # 发送重置密码验证码邮件（无需登录认证）

            ## 特性
            - 支持用户名或邮箱作为收件人
            - 自动从数据库查询用户信息
            - Redis 验证码存储
            - HTML 邮件模板渲染
            - 内置邮件主题

            ## 参数说明：
            - usernameOrEmail: 用户名或邮箱地址，字符串类型，必填
              - 支持用户名格式：6-16位字母/数字/下划线
              - 支持邮箱格式：标准邮箱格式
              - 系统会自动识别并查询对应的邮箱地址

            ## 返回说明：
            - **发送成功**：返回 **{"data": true}** 和“邮件发送成功”提示
            - **发送失败**：返回 **{"data": false}** 和“邮件发送失败”提示
            - **用户不存在**：返回 **{"data": false}** 和“用户不存在，请先注册”提示
            - **格式错误**：返回 **{"data": false}** 和“用户名或邮箱格式错误”提示
            - **验证码已存在**：返回 **{"data": {"remainingSeconds": 剩余秒数, "message": "提示信息"}}** 和“验证码已存在，请检查邮箱或稍后重试”提示

            ## 业务逻辑：
            1. 判断usernameOrEmail参数是用户名还是邮箱
            2. 从数据库查询用户信息，获取用户名和邮箱
            3. 验证用户是否存在
            4. 检查Redis中是否已有该邮箱的未过期验证码
            5. 生成6位随机验证码并存入Redis
            6. 使用HTML邮件模板渲染邮件内容
            7. 发送邮件并将验证码与邮箱绑定存储

            ## 注意事项：
            - 验证码默认有效期由Redis配置决定（默认5分钟）
            - 如果usernameOrEmail是用户名，系统会自动查询对应的邮箱
            - 如果usernameOrEmail是邮箱，直接使用
            - 用户必须已注册才能发送改密验证码
            - 此接口用于忘记重置密码场景
            - 邮件主题已内置为"PixVision 重置密码验证码"
            - **如果邮箱已有未过期的验证码，将拒绝发送新的验证码**
            """
    )
    public ResponsePojo<Boolean> sendForgetPasswordCode(
        @Parameter(description = "用户名或邮箱地址", required = true, example = "dev_user") @RequestParam String usernameOrEmail
    ) {
        // 基础数据校验
        if (!RegexUtils.isUsername(usernameOrEmail) && !RegexUtils.isEmail(usernameOrEmail)) {
            return ResponsePojo.error(null, "用户名或邮箱格式错误");
        }
        // 使用通用方法查询用户
        User user = userService.selectUserByUsernameOrEmail(usernameOrEmail);

        // 检查用户是否存在
        if (user == null) {
            log.warn("改密场景，用户不存在：{}", usernameOrEmail);
            return ResponsePojo.error(false, "用户不存在，请先注册");
        }

        String username = user.getUsername();
        String targetEmail = user.getEmail();
        log.info("发送改密验证码，用户名：{}，邮箱：{}", username, targetEmail);

        // 调用一站式验证码邮件发送方法
        EmailService.VerificationEmailResult result = emailService.sendVerificationEmail(targetEmail, username, "重置密码", "PixVision 重置密码验证码");
        if (!result.isSuccess()) {
            if (result.getExistingCodeRemainingSeconds() != null) {
                return ResponsePojo.error(false, StrUtil.format("验证码已存在，请检查邮箱或{}秒稍后重试", result.getExistingCodeRemainingSeconds()));
            }
            return ResponsePojo.error(false, result.getErrorMessage());
        }

        return ResponsePojo.success(true, "邮件发送成功");
    }

    /**
     * 发送修改密码验证码邮件（用于已登录用户）
     *
     * @param request HTTP 请求对象，用于从 Header 或 URL 参数中获取 Token
     * @return 响应数据，表示邮件是否发送成功
     * @author PlayerEG
     */
    @PostMapping("/send-change-password-code")
    @Operation(
        summary = "发送修改密码验证码邮件",
        description = """
            # 发送修改密码验证码邮件（需要登录认证）

            ## 特性
            - Token 认证（支持 Header 和 URL 参数两种方式）
            - 从 Token 自动获取用户信息
            - Redis 验证码存储
            - HTML 邮件模板渲染
            - 内置邮件主题
            - 需要登录（受保护接口）

            ## 参数说明：
            - Authorization: Header 中的 Token，格式为 `Bearer <token>`，或通过 URL 参数 `?token=<token>` 传递

            ## 返回说明：
            - **发送成功**：返回 **{"data": true}** 和“邮件发送成功”提示
            - **发送失败**：返回 **{"data": false}** 和“邮件发送失败”提示
            - **Token 不存在**：返回 **{"data": false}** 和“Token 不存在”提示
            - **Token 无效**：返回 **{"data": false}** 和“Token 无效”提示
            - **用户不存在**：返回 **{"data": false}** 和“用户不存在”提示
            - **验证码已存在**：返回 **{"data": {"remainingSeconds": 剩余秒数, "message": "提示信息"}}** 和“验证码已存在，请检查邮箱或稍后重试”提示

            ## 业务逻辑：
            1. 从请求头或 URL 参数中提取 Token
            2. 从 Token 中解析用户 ID
            3. 根据用户 ID 查询用户信息，获取用户名和邮箱
            4. 验证用户是否存在
            5. 检查Redis中是否已有该邮箱的未过期验证码
            6. 生成6位随机验证码并存入Redis
            7. 使用HTML邮件模板渲染邮件内容
            8. 发送邮件并将验证码与邮箱绑定存储

            ## 注意事项：
            - 验证码默认有效期由Redis配置决定（默认5分钟）
            - 用户信息从 Token 中自动获取，无需传入
            - 此接口用于**已登录用户修改密码**场景
            - 邮件主题已内置为"PixVision 修改密码验证码"
            - 需要携带有效的 Token 才能调用
            - **如果邮箱已有未过期的验证码，将拒绝发送新的验证码**
            """
    )
    public ResponsePojo<Boolean> sendChangePasswordCode(
        @Parameter(description = "HTTP 请求对象，用于从 Header 或 URL 参数中获取 Token", required = true) jakarta.servlet.http.HttpServletRequest request
    ) {
        // 提取 Token 并验证用户身份
        User user = userService.extractUserFromToken(request, "修改密码验证码接口");
        if (user == null) {
            return ResponsePojo.error(false, "认证失败，请重新登录");
        }

        String username = user.getUsername();
        String targetEmail = user.getEmail();
        log.info("发送修改密码验证码，用户名：{}，邮箱：{}", username, targetEmail);

        // 调用一站式验证码邮件发送方法
        EmailService.VerificationEmailResult result = emailService.sendVerificationEmail(targetEmail, username, "修改密码", "PixVision 修改密码验证码");
        if (!result.isSuccess()) {
            if (result.getExistingCodeRemainingSeconds() != null) {
                return ResponsePojo.error(false, StrUtil.format("验证码已存在，请检查邮箱或{}秒稍后重试", result.getExistingCodeRemainingSeconds()));
            }
            return ResponsePojo.error(false, result.getErrorMessage());
        }

        return ResponsePojo.success(true, "邮件发送成功");
    }

    /**
     * 发送注销账户验证码邮件（用于已登录用户）
     *
     * @param request HTTP 请求对象，用于从 Header 或 URL 参数中获取 Token
     * @return 响应数据，表示邮件是否发送成功
     * @author PlayerEG
     */
    @PostMapping("/send-delete-account-code")
    @Operation(
        summary = "发送注销账户验证码邮件",
        description = """
            # 发送注销账户验证码邮件（需要登录认证）

            ## 特性
            - Token 认证（支持 Header 和 URL 参数两种方式）
            - 从 Token 自动获取用户信息
            - Redis 验证码存储
            - HTML 邮件模板渲染
            - 内置邮件主题
            - 需要登录（受保护接口）

            ## 参数说明：
            - Authorization: Header 中的 Token，格式为 `Bearer <token>`，或通过 URL 参数 `?token=<token>` 传递

            ## 返回说明：
            - **发送成功**：返回 **{"data": true}** 和“邮件发送成功”提示
            - **发送失败**：返回 **{"data": false}** 和“邮件发送失败”提示
            - **Token 不存在**：返回 **{"data": false}** 和“Token 不存在”提示
            - **Token 无效**：返回 **{"data": false}** 和“Token 无效”提示
            - **用户不存在**：返回 **{"data": false}** 和“用户不存在”提示
            - **验证码已存在**：返回 **{"data": {"remainingSeconds": 剩余秒数, "message": "提示信息"}}** 和“验证码已存在，请检查邮箱或稍后重试”提示

            ## 业务逻辑：
            1. 从请求头或 URL 参数中提取 Token
            2. 从 Token 中解析用户 ID
            3. 根据用户 ID 查询用户信息，获取用户名和邮箱
            4. 验证用户是否存在
            5. 检查Redis中是否已有该邮箱的未过期验证码
            6. 生成6位随机验证码并存入Redis
            7. 使用HTML邮件模板渲染邮件内容
            8. 发送邮件并将验证码与邮箱绑定存储

            ## 注意事项：
            - 验证码默认有效期由Redis配置决定（默认5分钟）
            - 用户信息从 Token 中自动获取，无需传入
            - 此接口用于**已登录用户注销账户**场景
            - 邮件主题已内置为"PixVision 注销账户验证码"
            - 需要携带有效的 Token 才能调用
            - 注销操作不可逆，请谨慎操作
            - **如果邮箱已有未过期的验证码，将拒绝发送新的验证码**
            """
    )
    public ResponsePojo<Boolean> sendDeleteAccountCode(
        @Parameter(description = "HTTP 请求对象，用于从 Header 或 URL 参数中获取 Token", required = true) jakarta.servlet.http.HttpServletRequest request
    ) {
        // 提取 Token 并验证用户身份
        User user = userService.extractUserFromToken(request, "注销账户验证码接口");
        if (user == null) {
            return ResponsePojo.error(false, "认证失败，请重新登录");
        }

        String username = user.getUsername();
        String targetEmail = user.getEmail();
        log.info("发送注销账户验证码，用户名：{}，邮箱：{}", username, targetEmail);

        // 调用一站式验证码邮件发送方法
        EmailService.VerificationEmailResult result = emailService.sendVerificationEmail(targetEmail, username, "注销账户", "PixVision 注销账户验证码");
        if (!result.isSuccess()) {
            if (result.getExistingCodeRemainingSeconds() != null) {
                return ResponsePojo.error(false, StrUtil.format("验证码已存在，请检查邮箱或{}秒稍后重试", result.getExistingCodeRemainingSeconds()));
            }
            return ResponsePojo.error(false, result.getErrorMessage());
        }

        return ResponsePojo.success(true, "邮件发送成功");
    }

    /**
     * 发送权限变更验证码邮件（用于已登录用户）
     *
     * @param request HTTP 请求对象，用于从 Header 或 URL 参数中获取 Token
     * @return 响应数据，表示邮件是否发送成功
     * @author PlayerEG
     */
    @PostMapping("/send-role-change-code")
    @Operation(
        summary = "发送权限变更验证码邮件",
        description = """
            # 发送权限变更验证码邮件（需要登录认证）

            ## 特性
            - Token 认证（支持 Header 和 URL 参数两种方式）
            - 从 Token 自动获取用户信息
            - Redis 验证码存储
            - HTML 邮件模板渲染
            - 内置邮件主题
            - 需要登录（受保护接口）

            ## 参数说明：
            - Authorization: Header 中的 Token，格式为 `Bearer <token>`，或通过 URL 参数 `?token=<token>` 传递

            ## 返回说明：
            - **发送成功**：返回 **{"data": true}** 和"邮件发送成功"提示
            - **发送失败**：返回 **{"data": false}** 和"邮件发送失败"提示
            - **Token 不存在**：返回 **{"data": false}** 和"Token 不存在"提示
            - **Token 无效**：返回 **{"data": false}** 和"Token 无效"提示
            - **用户不存在**：返回 **{"data": false}** 和"用户不存在"提示
            - **验证码已存在**：返回 **{"data": {"remainingSeconds": 剩余秒数, "message": "提示信息"}}** 和"验证码已存在，请检查邮箱或稍后重试"提示

            ## 业务逻辑：
            1. 从请求头或 URL 参数中提取 Token
            2. 从 Token 中解析用户 ID
            3. 根据用户 ID 查询用户信息，获取用户名和邮箱
            4. 验证用户是否存在
            5. 检查Redis中是否已有该邮箱的未过期验证码
            6. 生成6位随机验证码并存入Redis
            7. 使用HTML邮件模板渲染邮件内容
            8. 发送邮件并将验证码与邮箱绑定存储

            ## 注意事项：
            - 验证码默认有效期由Redis配置决定（默认5分钟）
            - 用户信息从 Token 中自动获取，无需传入
            - 此接口用于**已登录用户申请权限变更**场景
            - 邮件主题已内置为"PixVision 权限变更验证码"
            - 需要携带有效的 Token 才能调用
            - 收到验证码后，调用 `/api/user/profile/role/apply` 接口完成权限变更
            - **如果邮箱已有未过期的验证码，将拒绝发送新的验证码**
            """)
    public ResponsePojo<Boolean> sendRoleChangeCode(
        @Parameter(description = "HTTP 请求对象，用于从 Header 或 URL 参数中获取 Token", required = true) jakarta.servlet.http.HttpServletRequest request
    ) {
        // 提取 Token 并验证用户身份
        User user = userService.extractUserFromToken(request, "权限变更验证码接口");
        if (user == null) {
            return ResponsePojo.error(false, "认证失败，请重新登录");
        }

        String username = user.getUsername();
        String targetEmail = user.getEmail();
        log.info("发送权限变更验证码，用户名：{}，邮箱：{}", username, targetEmail);

        // 调用一站式验证码邮件发送方法
        EmailService.VerificationEmailResult result = emailService.sendVerificationEmail(targetEmail, username, "权限变更", "PixVision 权限变更验证码");
        if (!result.isSuccess()) {
            if (result.getExistingCodeRemainingSeconds() != null) {
                return ResponsePojo.error(false, StrUtil.format("验证码已存在，请检查邮箱或{}秒稍后重试", result.getExistingCodeRemainingSeconds()));
            }
            return ResponsePojo.error(false, result.getErrorMessage());
        }

        return ResponsePojo.success(true, "邮件发送成功");
    }

    /**
     * 发送更改邮箱验证码邮件（用于已登录用户）
     *
     * @param request  HTTP 请求对象，用于从 Header 或 URL 参数中获取 Token
     * @param newEmail 新邮箱地址（标准格式）
     * @return 响应数据，表示邮件是否发送成功
     * @author PlayerEG
     */
    @PostMapping("/send-change-email-code")
    @Operation(
        summary = "发送更改邮箱验证码邮件",
        description = """
            # 发送更改邮箱验证码邮件（需要登录认证）

            ## 特性
            - Token 认证（支持 Header 和 URL 参数两种方式）
            - 从 Token 自动获取用户信息
            - 新邮箱格式校验
            - 新邮箱唯一性检查
            - Redis 验证码存储
            - HTML 邮件模板渲染
            - 内置邮件主题
            - 需要登录（受保护接口）

            ## 参数说明：
            - Authorization: Header 中的 Token，格式为 `Bearer <token>`，或通过 URL 参数 `?token=<token>` 传递
            - newEmail: 新邮箱地址，字符串类型，必填，需符合标准邮箱格式

            ## 返回说明：
            - **发送成功**：返回 **{"data": true}** 和“邮件发送成功”提示
            - **发送失败**：返回 **{"data": false}** 和“邮件发送失败”提示
            - **Token 不存在**：返回 **{"data": false}** 和“Token 不存在”提示
            - **Token 无效**：返回 **{"data": false}** 和“Token 无效”提示
            - **用户不存在**：返回 **{"data": false}** 和“用户不存在”提示
            - **新邮箱为空**：返回 **{"data": false}** 和“新邮箱不能为空”提示
            - **邮箱格式错误**：返回 **{"data": false}** 和“邮箱格式错误”提示
            - **邮箱已被使用**：返回 **{"data": false}** 和“该邮箱已被其他账号使用”提示
            - **验证码已存在**：返回 **{"data": {"remainingSeconds": 剩余秒数, "message": "提示信息"}}** 和“验证码已存在，请检查邮箱或稍后重试”提示

            ## 业务逻辑：
            1. 从请求头或 URL 参数中提取 Token
            2. 从 Token 中解析用户 ID
            3. 根据用户 ID 查询用户信息，获取用户名和当前邮箱
            4. 验证用户是否存在
            5. 校验新邮箱格式是否正确
            6. 检查新邮箱是否已被其他用户使用
            7. 检查Redis中是否已有该新邮箱的未过期验证码
            8. 生成6位随机验证码并存入Redis（使用新邮箱作为 key）
            9. 使用HTML邮件模板渲染邮件内容
            10. 发送邮件并将验证码与新邮箱绑定存储

            ## 注意事项：
            - 验证码默认有效期由Redis配置决定（默认5分钟）
            - 用户信息从 Token 中自动获取，无需传入
            - 此接口用于**已登录用户更改绑定邮箱**场景
            - 邮件主题已内置为“PixVision 更改邮箱验证码”
            - 需要携带有效的 Token 才能调用
            - 新邮箱不能被其他账号使用
            - 验证码会发送到**新邮箱**，而不是当前绑定的邮箱
            - 收到验证码后，调用 `/api/user/profile/change/email` 接口完成邮箱修改
            - **如果新邮箱已有未过期的验证码，将拒绝发送新的验证码**
            """
    )
    public ResponsePojo<Boolean> sendChangeEmailCode(
        @Parameter(description = "HTTP 请求对象，用于从 Header 或 URL 参数中获取 Token", required = true) jakarta.servlet.http.HttpServletRequest request,
        @Parameter(description = "新邮箱地址，需符合标准邮箱格式", required = true, example = "newemail@example.com") @RequestParam String newEmail
    ) {
        // 提取 Token 并验证用户身份
        User user = userService.extractUserFromToken(request, "更改邮箱验证码接口");
        if (user == null) {
            return ResponsePojo.error(false, "认证失败，请重新登录");
        }

        // 校验新邮箱参数
        if (newEmail == null || newEmail.isEmpty()) {
            log.warn("新邮箱为空，用户 ID: {}", user.getUser_id());
            return ResponsePojo.error(false, "新邮箱不能为空");
        }

        if (!RegexUtils.isEmail(newEmail)) {
            log.warn("邮箱格式错误，用户 ID: {}, 邮箱: {}", user.getUser_id(), newEmail);
            return ResponsePojo.error(false, "邮箱格式错误");
        }

        String username = user.getUsername();
        String currentEmail = user.getEmail();
        log.info("发送更改邮箱验证码，用户名：{}，当前邮箱：{}，新邮箱：{}", username, currentEmail, newEmail);

        // 检查新邮箱是否已被其他用户使用
        User existingUser = userService.selectAllUserByEmail(newEmail);
        if (existingUser != null && !existingUser.getUser_id().equals(user.getUser_id())) {
            log.warn("新邮箱已被其他用户使用: {}", newEmail);
            return ResponsePojo.error(false, "该邮箱已被其他账号使用");
        }

        // 如果新邮箱与当前邮箱相同，给出提示
        if (newEmail.equals(currentEmail)) {
            log.warn("新邮箱与当前邮箱相同，用户 ID: {}", user.getUser_id());
            return ResponsePojo.error(false, "新邮箱与当前邮箱相同，无需修改");
        }

        // 调用一站式验证码邮件发送方法（发送到新邮箱）
        EmailService.VerificationEmailResult result = emailService.sendVerificationEmail(newEmail, username, "更改邮箱", "PixVision 更改邮箱验证码");
        if (!result.isSuccess()) {
            if (result.getExistingCodeRemainingSeconds() != null) {
                return ResponsePojo.error(false, StrUtil.format("验证码已存在，请检查邮箱或{}秒稍后重试", result.getExistingCodeRemainingSeconds()));
            }
            return ResponsePojo.error(false, result.getErrorMessage());
        }

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
     * @deprecated
     */
    @Deprecated
    @PostMapping("/verify-email-code-test")
    @Operation(
        summary = "验证\"验证码\" - 测试",
        description = """
            # 验证用户输入的邮箱验证码是否正确（无需登录认证｜测试）

            ## 特性
            - 公开接口（无需认证）
            - Redis 验证码比对
            - 验证后自动清除验证码
            - 已废弃（@Deprecated）

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
        if (!RegexUtils.isVCode(inputVCode, 6)) {
            return ResponsePojo.error(false, "验证码格式错误");
        }

        boolean isTrue = verificationCodeServices.verificationCodeVerify(email, inputVCode);

        if (!isTrue) {
            return ResponsePojo.error(false, "验证失败");
        }

        return ResponsePojo.success(true, "验证成功");
    }
}
