package top.playereg.pix_vision.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
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
            - **登录成功**：返回 **{"data": {UserLogin 对象}}**，包含用户信息和 JWT Token
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

    /**
     * 分页查询用户信息
     *
     * @param current  当前页码（从 1 开始）
     * @param size     每页大小
     * @param username 用户名（可选，模糊查询）
     * @param uuid     UUID（可选，精确查询）
     * @param email    邮箱（可选，模糊查询）
     * @return 响应数据<IPage < User>>
     * @author PlayerEG
     */
    @GetMapping("/page/{current}/{size}")
    @Operation(
        summary = "分页查询用户信息",
        description = """
            # 分页查询用户信息

            ## 参数说明：
            - current: 当前页码，**从 1 开始**，Long 类型，必填，默认为 1
            - size: 每页大小，Long 类型，必填，默认为 10，范围 1-100
            - username: 用户名（可选），字符串类型，支持模糊查询
            - uuid: 用户 UUID（可选），字符串类型，支持精确查询
            - email: 邮箱（可选），字符串类型，支持模糊查询

            ## 返回说明：
            - **查询成功**：返回 **{"data": {IPage<User>对象}}**，包含用户列表和分页信息
            - **参数错误**：返回 **{"data": null}** 和"页码或每页大小错误"提示
            - **UUID 格式错误**：返回 **{"data": null}** 和"UUID 格式错误"提示

            ## 返回数据结构：
            ```json
            {
              "code": 200,
              "data": {
                "records": [用户对象列表],
                "total": 总记录数，
                "size": 每页大小，
                "current": 当前页，
                "pages": 总页数
              },
              "message": "查询成功"
            }
            ```

            ## 业务逻辑：
            1. 校验页码和每页大小参数（current>=1, 1<=size<=100）
            2. 转换 UUID 字符串为 byte 数组（如提供）
            3. 构建 MyBatis-Plus 分页对象
            4. 根据条件查询用户信息（支持多条件组合）
            5. 返回分页结果集

            ## 注意事项：
            - 所有查询条件均为**可选参数**，可不传
            - 支持多个条件组合查询
            - 用户名和邮箱支持**模糊匹配**
            - UUID 支持**精确匹配**
            - 默认返回 8 个核心字段（user_id, user_uuid, username, password, nickname, avatar_url, email, status）
            - 已自动过滤逻辑删除的用户（is_delete=0）
            - 每页大小限制：**1-100**
            """
    )
    public ResponsePojo<IPage<User>> getPageUserInfo(
        @Parameter(description = "当前页码，从 1 开始", example = "1") @PathVariable Long current,
        @Parameter(description = "每页大小，范围 1-100", example = "10") @PathVariable Long size,
        @Parameter(description = "用户名（可选），支持模糊查询") @RequestParam(required = false) String username,
        @Parameter(description = "用户 UUID（可选），支持精确查询，标准 UUID 格式") @RequestParam(required = false) String uuid,
        @Parameter(description = "邮箱（可选），支持模糊查询") @RequestParam(required = false) String email
    ) {
        // 参数校验
        if (current == null || current < 1) {
            return ResponsePojo.error(null, "页码必须大于 0");
        }
        if (size == null || size < 1 || size > 100) {
            return ResponsePojo.error(null, "每页大小必须在 1-100 之间");
        }
        if (username != null && !username.isEmpty() && !RegexUtils.isUsername(username)) {
            return ResponsePojo.error(null, "用户名格式错误");
        }
        if (email != null && !email.isEmpty() && !RegexUtils.isEmail(email)) {
            return ResponsePojo.error(null, "邮箱格式错误");
        }
        if (uuid != null && !uuid.isEmpty() && !RegexUtils.isUUID(uuid)) {
            return ResponsePojo.error(null, "UUID 格式错误");
        }

        // 转换 UUID 字符串为 byte 数组
        byte[] uuidBytes = null;
        if (uuid != null && !uuid.isEmpty()) {
            uuidBytes = StrSwitchUtils.uuid2Bytes(uuid);
        }

        // 构建分页对象
        Page<User> page = new Page<>(current, size);

        // 将查询到的用户的 16 字节二进制数组转为 16 进制字符串
        IPage<User> result = userService.selectPageUserInfo(page, username, uuidBytes, email);

        // 返回结果为空，则返回错误信息
        if (result == null || result.getRecords().size() == 0) {
            log.error("分页查询返回结果为空 - 页码：{}, 每页：{}", current, size);
            return ResponsePojo.error(null, "查询失败，返回结果为空");
        }

        // 将用户的 16 字节二进制UUID转换成字符串UUID
        for (User user : result.getRecords()) {
            user.setString_user_uuid(StrSwitchUtils.bytes2Uuid(user.getUser_uuid()));
        }

        log.info("分页查询成功 - 页码：{}, 每页：{}, 总数：{}, 返回：{}",
            current, size, result.getTotal(), result.getRecords().size());

        return ResponsePojo.success(result, "查询成功");
    }


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
    @PostMapping("/change-password")
    @Operation(
        summary = "用户密码修改 - JWT验证",
        description = """
            # 用户密码修改 - JWT验证

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
    @PostMapping("/forgot-password")
    @Operation(
        summary = "用户密码修改 - 忘记密码",
        description = """
            # 用户密码修改 - 忘记密码

            ## 参数说明：
            - usernameOrEmail: **用户名**或**邮箱地址**，字符串类型，必填
            - newPassword: 新密码，字符串类型，必填，建议使用强密码组合
            - confirmPassword: 确认新密码，字符串类型，必填，需与新密码一致
            - vCode: 邮箱验证码（6 位大写字母或数字），字符串类型，必填

            ## 返回说明：
            - **重置成功**：返回 **{"data": true}** 和“密码重置成功，请使用新密码重新登录”提示
            - **用户名或邮箱格式错误**：返回 **{"data": false}** 和“用户名或邮箱格式错误”提示
            - **验证码格式错误**：返回 **{"data": false}** 和“验证码格式错误”提示
            - **验证码错误**：返回 **{"data": false}** 和“验证码错误”提示
            - **用户不存在**：返回 **{"data": false}** 和“用户不存在”提示
            - **两次密码不一致**：返回 **{"data": false}** 和“两次输入的密码不一致”提示
            - **重置失败**：返回 **{"data": false}** 和“密码重置失败”提示

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
