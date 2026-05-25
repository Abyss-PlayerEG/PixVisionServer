package top.playereg.pix_vision.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import top.playereg.pix_vision.pojo.NicknameChangeResult;
import top.playereg.pix_vision.pojo.ResponsePojo;
import top.playereg.pix_vision.pojo.userPojo.User;
import top.playereg.pix_vision.service.TokenWhitelistService;
import top.playereg.pix_vision.service.UserService;
import top.playereg.pix_vision.service.VerificationCodeServices;
import top.playereg.pix_vision.util.Annotation.PublicAccess;
import top.playereg.pix_vision.util.*;

import java.util.List;

/**
 * 用户资料管理相关接口（分页查询、修改昵称）
 *
 * @author PlayerEG
 * @see top.playereg.pix_vision.service.Impl.UserServiceImpl
 */
@RestController
@SuppressWarnings("all")
@RequestMapping("/api/user/profile")
@RequiredArgsConstructor
@Tag(name = "用户接口 - 用户基础资料", description = "提供用户基本资料的查询和修改功能")
public class UserProfileController {
    private static final PixVisionLogger log = PixVisionLogger.create(UserProfileController.class);

    private final UserService userService;
    private final TokenWhitelistService tokenWhitelistService;
    private final VerificationCodeServices verificationCodeServices;
    private final top.playereg.pix_vision.service.WorkService workService;

    /**
     * 分页查询用户信息
     *
     * @param current 当前页码（从 1 开始）
     * @param size    每页大小（范围 1-500）
     * @param keyword 关键词（可选，支持 UUID 精确查询或用户名/邮箱/昵称模糊查询）
     * @return 响应数据，包含分页的用户信息列表
     * @author PlayerEG
     */
    @GetMapping("/page/{current}/{size}")
    @PublicAccess("分页查询用户信息，无需认证")
    @Operation(
        summary = "分页查询用户信息",
        description = """
            # 分页查询用户信息（无需登录认证）

            ## 特性
            - Token 认证（通过拦截器自动验证）
            - MyBatis-Plus 分页支持
            - 关键词统一查询（同时搜索用户名/邮箱/昵称/UUID）
            - 模糊匹配与精确匹配
            - UUID 二进制转换

            ## 参数说明：
            - current: 当前页码，**从 1 开始**，Long 类型，必填，默认为 1
            - size: 每页大小，Long 类型，必填，默认为 10，范围 1-500
            - keyword: **关键词**（可选），字符串类型
              * 如果输入的是标准 UUID 格式，则进行**精确匹配**
              * 否则同时对用户名、邮箱、昵称进行**模糊匹配**

            ## 返回说明：
            - **查询成功**：返回 **{"data": {IPage<User>对象}}** ，包含用户列表和分页信息
            - **参数错误**：返回 **{"data": null}** 和"页码或每页大小错误"提示
            - **UUID 格式错误**：返回 **{"data": null}** 和"UUID 格式错误"提示

            ## 业务逻辑：
            1. 校验页码和每页大小参数（current>=1, 1<=size<=100）
            2. 判断关键词类型：
               - 如果是标准 UUID 格式，转换为 byte 数组进行精确查询
               - 否则作为普通关键词，同时对用户名、邮箱、昵称进行模糊查询
            3. 构建 MyBatis-Plus 分页对象
            4. 根据条件查询用户信息
            5. 返回分页结果集

            ## 注意事项：
            - **关键词为可选参数**，可不传
            - 如果关键词是标准 UUID 格式（如：550e8400-e29b-41d4-a716-446655440000），则进行**精确匹配**
            - 如果关键词不是 UUID 格式，则同时对用户名、邮箱、昵称进行**模糊匹配**
            - 默认返回 8 个核心字段（user_id, user_uuid, username, password, nickname, avatar_url, email, status）
            - 已自动过滤逻辑删除的用户（is_delete=0）
            - 每页大小限制：**1-500**
            """
    )
    public ResponsePojo<IPage<User>> getPageUserInfo(
        @Parameter(description = "当前页码，从 1 开始", example = "1") @PathVariable Long current,
        @Parameter(description = "每页大小，范围 1-500", example = "10") @PathVariable Long size,
        @Parameter(description = "关键词（可选），支持 UUID 精确查询或用户名/邮箱/昵称模糊查询") @RequestParam(required = false) String keyword
    ) {
        // 参数校验
        ResponsePojo<?> error = PageUtils.validatePageParams(current, size);
        if (error != null) {
            return (ResponsePojo<IPage<User>>) (ResponsePojo<?>) error;
        }

        // 判断关键词类型：UUID 精确查询 or 普通关键词模糊查询
        byte[] uuidBytes = null;
        String searchKeyword = null;

        if (keyword != null && !keyword.isEmpty()) {
            if (RegexUtils.isUUID(keyword)) {
                // UUID 格式，进行精确查询
                uuidBytes = StrSwitchUtils.uuid2Bytes(keyword);
                log.info("关键词识别为 UUID，进行精确查询: {}", keyword);
            } else {
                // 非 UUID 格式，进行模糊查询
                searchKeyword = keyword;
                log.info("关键词识别为普通文本，进行模糊查询: {}", keyword);
            }
        }

        // 构建分页对象
        Page<User> page = new Page<>(current, size);

        // 调用服务层查询用户信息
        IPage<User> result = userService.selectPageUserInfo(page, searchKeyword, uuidBytes);

        // 将用户的 16 字节二进制UUID转换成字符串UUID，并隐藏敏感字段
        for (User user : result.getRecords()) {
            user.setString_user_uuid(StrSwitchUtils.bytes2Uuid(user.getUser_uuid()));
            // 隐藏敏感字段和隐私信息
            user.setUser_uuid(null);
            user.setPassword(null);
            user.setEmail(null);              // 隐藏邮箱
            user.setString_user_uuid(null);  // 隐藏 UUID 字符串
        }

        log.info("分页查询成功 - 页码：{}, 每页：{}, 总数：{}, 返回：{}",
            current, size, result.getTotal(), result.getRecords().size());

        return ResponsePojo.success(result, "查询成功");
    }

    /**
     * 按角色分页查询用户信息
     *
     * @param current   当前页码（从 1 开始）
     * @param size      每页大小（范围 1-100）
     * @param userRoles 用户角色列表（可选，支持多个角色）
     * @return 响应数据，包含分页的用户信息列表
     * @author PlayerEG
     */
    @GetMapping("/page-by-role/{current}/{size}")
    @PublicAccess("按角色分页查询用户信息，无需认证")
    @Operation(
        summary = "按角色分页查询用户信息",
        description = """
            # 按角色分页查询用户信息（无需登录认证）

            ## 特性
            - 公开接口（无需 Token 认证）
            - MyBatis-Plus 分页支持
            - 支持单个或多个角色筛选
            - 已自动过滤逻辑删除的用户

            ## 参数说明：
            - current: 当前页码，**从 1 开始**，Long 类型，必填，默认为 1
            - size: 每页大小，Long 类型，必填，默认为 10，范围 1-500
            - userRoles: **用户角色列表**（可选），Integer 数组类型，支持传递多个角色
              * 11-普通用户, 22-创作者, 55-审核员, 66-工单管理员, 77-系统管理员
              * 可以传递单个角色或多个角色，如：?userRoles=11 或 ?userRoles=11&userRoles=22

            ## 返回说明：
            - **查询成功**：返回 **{"data": {IPage<User>对象}}** ，包含用户列表和分页信息
            - **参数错误**：返回 **{"data": null}** 和"页码或每页大小错误"提示
            - **无符合条件的用户**：返回 **{"data": null}** 和"查询失败，返回结果为空"提示

            ## 业务逻辑：
            1. 校验页码和每页大小参数（current>=1, 1<=size<=100）
            2. 如果提供了 userRoles 参数，则按角色筛选用户（支持多角色 OR 查询）
            3. 如果未提供 userRoles 参数，则查询所有用户
            4. 构建 MyBatis-Plus 分页对象
            5. 根据条件查询用户信息
            6. 将二进制 UUID 转换为字符串格式
            7. 返回分页结果集

            ## 注意事项：
            - **userRoles 为可选参数**，可不传，不传则查询所有用户
            - 支持传递多个角色，多个角色之间是 OR 关系（满足任一角色即可）
            - 默认返回 8 个核心字段（user_id, user_uuid, username, password, nickname, avatar_url, email, status, user_role）
            - 已自动过滤逻辑删除的用户（is_delete=0）
            - 每页大小限制：**1-500**
            - 合法的角色代码：11-普通用户, 22-创作者, 55-审核员, 66-工单管理员, 77-系统管理员
            """
    )
    public ResponsePojo<IPage<User>> getPageUserInfoByRole(
        @Parameter(description = "当前页码，从 1 开始", example = "1") @PathVariable Long current,
        @Parameter(description = "每页大小，范围 1-500", example = "10") @PathVariable Long size,
        @Parameter(description = "用户角色列表（可选），支持多个角色", example = "11") @RequestParam(required = false) List<Integer> userRoles
    ) {
        // 参数校验
        ResponsePojo<?> error = PageUtils.validatePageParams(current, size);
        if (error != null) {
            return (ResponsePojo<IPage<User>>) (ResponsePojo<?>) error;
        }

        log.info("按角色分页查询用户信息 - 页码: {}, 每页: {}, 角色列表: {}", current, size, userRoles != null ? userRoles.toString() : "无");

        // 构建分页对象
        Page<User> page = new Page<>(current, size);

        // 调用服务层查询用户信息
        IPage<User> result = userService.selectPageUserInfoByRole(page, userRoles);

        // 将用户的 16 字节二进制UUID转换成字符串UUID，并隐藏敏感字段
        for (User user : result.getRecords()) {
            user.setString_user_uuid(StrSwitchUtils.bytes2Uuid(user.getUser_uuid()));
            // 隐藏敏感字段和隐私信息
            user.setUser_uuid(null);
            user.setPassword(null);
            user.setEmail(null);              // 隐藏邮箱
            user.setString_user_uuid(null);  // 隐藏 UUID 字符串
        }

        log.info("按角色分页查询成功 - 页码：{}, 每页：{}, 总数：{}, 返回：{}",
            current, size, result.getTotal(), result.getRecords().size());

        return ResponsePojo.success(result, "查询成功");
    }

    /**
     * 根据用户 ID、用户名或 UUID 查询用户信息
     *
     * @param userId   用户 ID（可选）
     * @param username 用户名（可选）
     * @param uuid     用户 UUID（可选，字符串格式）
     * @return 响应数据，包含用户详细信息
     * @author PlayerEG
     */
    @GetMapping("/info")
    @PublicAccess("根据用户 ID、用户名或 UUID 查询用户信息，无需认证")
    @Operation(
        summary = "根据用户 ID、用户名或 UUID 查询用户信息",
        description = """
            # 根据用户 ID、用户名或 UUID 查询用户信息（无需登录认证）

            ## 特性
            - 公开接口（无需 Token 认证）
            - 支持通过用户 ID、用户名或 UUID 精确查询单个用户
            - 自动转换二进制 UUID 为字符串格式
            - 查询优先级：userId > username > uuid
            - userId、username、uuid 至少提供一个

            ## 参数说明：
            - userId: **用户 ID**，Integer 类型，可选，直接通过主键查询（优先级最高）
            - username: **用户名**，String 类型，可选，精确匹配
            - uuid: **用户 UUID**，String 类型，可选，支持两种格式：
              * 32位不带连字符：`6ddae8a5837d4721a1b783a7f98c67aa`
              * 36位带连字符：`550e8400-e29b-41d4-a716-446655440000`

            ## 返回说明：
            - **查询成功**：返回 **{"data": {User对象}}** ，包含用户详细信息
            - **参数缺失**：返回 **{"data": null}** 和"请提供用户 ID、用户名或 UUID"提示
            - **UUID 格式错误**：返回 **{"data": null}** 和"UUID 格式错误"提示
            - **用户不存在**：返回 **{"data": null}** 和"用户不存在"提示

            ## 业务逻辑：
            1. 校验参数：userId、username、uuid 至少提供一个
            2. 按优先级查询：userId > username > uuid
            3. 如果使用 uuid，验证格式并转换为二进制
            4. 将二进制 UUID 转换为字符串格式
            5. 隐藏敏感字段后返回用户详细信息

            ## 注意事项：
            - 这是一个**公开接口**，无需 Token 认证
            - **userId、username、uuid 至少提供一个**
            - 查询优先级：userId（最快，主键查询）> username > uuid
            - 只返回**未删除**的用户（is_delete=0）
            - 返回的字段不包含敏感信息（password、user_uuid 均被隐藏）
            - UUID 支持两种格式：32位不带连字符 或 36位带连字符
            - username 为精确匹配，不支持模糊查询
            """
    )
    public ResponsePojo<User> getUserInfo(
        @Parameter(description = "用户 ID（可选），直接通过主键查询，优先级最高", example = "1") @RequestParam(required = false) Integer userId,
        @Parameter(description = "用户名（可选），精确匹配", example = "test_user") @RequestParam(required = false) String username,
        @Parameter(description = "用户 UUID（可选），标准格式", example = "550e8400-e29b-41d4-a716-446655440000") @RequestParam(required = false) String uuid
    ) {
        // 参数校验：userId、username、uuid 至少提供一个
        if (userId == null && (username == null || username.isEmpty()) && (uuid == null || uuid.isEmpty())) {
            log.warn("查询用户信息失败 - 缺少必要参数");
            return ResponsePojo.error(null, "请提供用户 ID、用户名或 UUID");
        }

        User user = null;

        // 优先级：userId > username > uuid
        if (userId != null) {
            // 使用 userId 查询（主键查询，性能最优）
            log.info("通过用户 ID 查询用户信息 - 用户 ID: {}", userId);
            user = userService.selectAllUserById(userId);
        } else if (username != null && !username.isEmpty()) {
            // 使用 username 查询
            log.info("通过用户名查询用户信息 - 用户名: {}", username);
            user = userService.selectUserByUsername(username);
        } else if (uuid != null && !uuid.isEmpty()) {
            // 使用 uuid 查询
            log.info("通过 UUID 查询用户信息 - UUID: {}", uuid);

            // 验证 UUID 格式（支持32位和36位两种格式）
            boolean isValid = RegexUtils.isUUID(uuid) || RegexUtils.isValidUuid(uuid);
            if (!isValid) {
                log.warn("UUID 格式错误: {}", uuid);
                return ResponsePojo.error(null, "UUID 格式错误，支持32位或36位格式");
            }

            // 将字符串 UUID 转换为二进制
            byte[] uuidBytes;
            if (uuid.length() == 32) {
                // 32位格式，直接转换
                uuidBytes = StrSwitchUtils.uuid2Bytes(uuid);
            } else {
                // 36位格式，先去除连字符再转换
                String uuidWithoutHyphens = uuid.replace("-", "");
                uuidBytes = StrSwitchUtils.uuid2Bytes(uuidWithoutHyphens);
            }
            user = userService.selectAllUserByUuid(uuidBytes);
        }

        // 检查用户是否存在（SQL 已过滤 is_delete=0，只需判断 null）
        if (user == null) {
            log.warn("用户不存在或已被删除 - 用户 ID: {}, 用户名: {}, UUID: {}", userId, username, uuid);
            return ResponsePojo.error(null, "用户不存在");
        }

        // 将二进制 UUID 转换为字符串格式
        user.setString_user_uuid(StrSwitchUtils.bytes2Uuid(user.getUser_uuid()));

        // 隐藏敏感字段和隐私信息
        user.setUser_uuid(null);
        user.setPassword(null);
        // user_role 保留返回
        // status 保留返回
        user.setString_user_uuid(null);  // 隐藏 UUID 字符串
        user.setEmail(null);              // 隐藏邮箱
        // username 保留返回

        // 查询用户统计数据
        java.util.Map<String, Object> stats = workService.getUserStats(user.getUser_id());
        if (stats != null) {
            user.setWork_count(((Number) stats.get("work_count")).intValue());
            user.setTotal_likes(((Number) stats.get("total_likes")).longValue());
            user.setTotal_stars(((Number) stats.get("total_stars")).longValue());
            user.setTotal_views(((Number) stats.get("total_views")).longValue());
        }

        log.info("查询用户信息成功 - 用户 ID: {}, 用户名: {}", user.getUser_id(), user.getUsername());

        return ResponsePojo.success(user, "查询成功");
    }

    /**
     * 通过 Token 查询当前用户个人信息
     *
     * @param request HTTP 请求对象，用于从 Header 或 URL 参数中获取 Token
     * @return 响应数据，包含当前用户的详细信息
     * @author PlayerEG
     */
    @GetMapping("/me")
    @Operation(
        summary = "查询当前用户个人信息",
        description = """
            # 查询当前用户个人信息（需要登录认证）

            ## 特性
            - Token 认证（支持 Header 和 URL 参数两种方式）
            - 自动从 Token 解析当前用户 ID
            - 隐藏敏感字段（密码、角色、状态等）
            - 适用于个人中心页面

            ## 参数说明：
            - Authorization: Header 中的 Token，格式为 `Bearer <token>`，或通过 URL 参数 `?token=<token>` 传递

            ## 返回说明：
            - **查询成功**：返回 **{"data": {User对象}}** ，包含当前用户的详细信息
            - **Token 不存在**：返回 **{"data": null}** 和"Token 不存在"提示
            - **Token 已失效**：返回 **{"data": null}** 和"Token 已失效"提示
            - **用户不存在**：返回 **{"data": null}** 和"用户不存在"提示

            ## 业务逻辑：
            1. 从请求头或 URL 参数中提取 Token
            2. 验证 Token 是否在白名单中
            3. 从 Token 中解析用户 ID
            4. 根据用户 ID 查询用户信息
            5. 将二进制 UUID 转换为字符串格式
            6. 隐藏敏感字段（密码、UUID、角色、状态）
            7. 返回用户详细信息

            ## 注意事项：
            - **必须携带有效的 Token**
            - Token 必须在白名单中（未过期、未登出）
            - 只返回**未删除**的用户（is_delete=0）
            - 返回的字段不包含敏感信息（password、user_uuid、user_role、status 均被隐藏）
            - 适合用于个人中心、用户资料编辑等场景
            """
    )
    public ResponsePojo<User> getCurrentUserInfo(
        @Parameter(description = "HTTP 请求对象，用于从 Header 或 URL 参数中获取 Token", required = true) HttpServletRequest request
    ) {
        // 提取 Token
        String token = JWTUtils.extractTokenWithLog(request, "查询当前用户信息接口");

        if (token == null || token.isEmpty()) {
            log.error("查询当前用户信息失败 - Token 不存在");
            return ResponsePojo.error(null, "Token 不存在，请在 Header 中添加 Authorization: Bearer <token> 或在 URL 参数中添加 ?token=<token>");
        }

        // 检查 Token 是否在白名单中
        if (!tokenWhitelistService.isInWhitelist(token)) {
            log.warn("Token 不在白名单中，可能已过期或被移除");
            return ResponsePojo.error(null, "Token 已失效");
        }

        // 从 Token 中获取用户 ID
        Integer userId = JWTUtils.getUserIdFromToken(token);
        if (userId == null) {
            log.error("从 Token 中解析用户 ID 失败");
            return ResponsePojo.error(null, "Token 无效");
        }

        String username = JWTUtils.getUsernameFromToken(token);
        log.info("开始查询当前用户信息，用户 ID: {}, 用户名: {}", userId, username);

        // 调用服务层查询用户信息
        User user = userService.selectAllUserById(userId);

        // 检查用户是否存在（SQL 已过滤 is_delete=0，只需判断 null）
        if (user == null) {
            log.warn("用户不存在或已被删除 - 用户 ID: {}", userId);
            return ResponsePojo.error(null, "用户不存在");
        }

        // 将二进制 UUID 转换为字符串格式
        user.setString_user_uuid(StrSwitchUtils.bytes2Uuid(user.getUser_uuid()));

        // 隐藏敏感字段
        user.setUser_uuid(null);
        user.setPassword(null);
        // user_role 保留返回
        // status 保留返回

        // 查询用户统计数据
        java.util.Map<String, Object> stats = workService.getUserStats(userId);
        if (stats != null) {
            user.setWork_count(((Number) stats.get("work_count")).intValue());
            user.setTotal_likes(((Number) stats.get("total_likes")).longValue());
            user.setTotal_stars(((Number) stats.get("total_stars")).longValue());
            user.setTotal_views(((Number) stats.get("total_views")).longValue());
        }

        log.info("查询当前用户信息成功 - 用户 ID: {}, 用户名: {}", userId, username);

        return ResponsePojo.success(user, "查询成功");
    }

    /**
     * 修改用户昵称
     *
     * @param request  HTTP 请求对象，用于从 Header 或 URL 参数中获取 Token
     * @param nickname 新昵称（长度 1-20 个字符）
     * @return 响应数据，表示昵称修改是否成功
     * @author PlayerEG
     */
    @PostMapping("/nickname/change")
    @Operation(
        summary = "修改用户昵称接口",
        description = """
            # 修改用户昵称（需要登录认证）

            ## 特性
            - Token 认证（支持 Header 和 URL 参数两种方式）
            - 昵称长度限制校验（1-20字符）
            - 支持中文、英文、数字和特殊字符
            - AI 内容安全审核（调用 Python AI 审核服务）

            ## 参数说明：
            - Authorization: Header 中的 Token，格式为 `Bearer <token>`，或通过 URL 参数 `?token=<token>` 传递
            - nickname: 新昵称，字符串类型，必填，长度 1-20 个字符

            ## 返回说明：
            - **修改成功**：返回 **{"data": true}** 和"昵称修改成功"提示
            - **AI 审核不通过（违规）**：返回 **{"data": false}** 和"违规内容：{原因}"提示
            - **AI 审核存疑（待审核）**：返回 **{"data": true}** 和"昵称修改已提交，等待人工审核"提示
            - **Token 不存在**：返回 **{"data": null}** 和"Token 不存在"提示
            - **Token 已失效**：返回 **{"data": null}** 和"Token 已失效"提示
            - **昵称为空**：返回 **{"data": null}** 和"昵称不能为空"提示
            - **昵称长度错误**：返回 **{"data": null}** 和"昵称长度必须在 1-20 个字符之间"提示
            - **修改失败**：返回 **{"data": false}** 和"昵称修改失败"提示

            ## 业务逻辑：
            1. 从请求头或 URL 参数中提取 Token（支持 Bearer 前缀）
            2. 验证 Token 是否在白名单中
            3. 从 Token 中解析用户 ID
            4. 校验昵称参数（非空、长度 1-20）
            5. 调用 AI 审核服务对昵称内容进行安全审核
            6. 根据审核结果：
               - 通过：直接更新昵称并记录 lock 表
               - 存疑：暂不更新，将变更记录写入 lock 表，等待人工审核
               - 违规：暂不更新，将违规记录写入 lock 表
            7. 返回差异化的响应消息

            ## 注意事项：
            - 需要携带有效的 Token 才能修改昵称
            - Token 必须在白名单中（未过期、未登出）
            - 昵称长度限制：**1-20 个字符**
            - 昵称可以包含中文、英文、数字和特殊字符
            - 昵称修改会自动调用 AI 审核服务进行内容安全审核
            - AI 审核不通过（违规）时直接拦截，data 返回 false
            - AI 审核存疑时标记为待审核，管理员审核通过后昵称才会更新
            - AI 审核服务不可用时自动降级为待审核状态
            - 修改成功后，下次登录时会看到新昵称
            """
    )
    public ResponsePojo<Boolean> updateNickname(
        @Parameter(description = "HTTP 请求对象，用于从 Header 或 URL 参数中获取 Token", required = true) HttpServletRequest request,
        @Parameter(description = "新昵称，长度 1-20 个字符", required = true, example = "新昵称") @RequestParam String nickname
    ) {
        // 提取 Token
        String token = JWTUtils.extractTokenWithLog(request, "修改昵称接口");

        if (token == null || token.isEmpty()) {
            log.error("修改昵称失败 - Token 不存在");
            return ResponsePojo.error(null, "Token 不存在，请在 Header 中添加 Authorization: Bearer <token> 或在 URL 参数中添加 ?token=<token>");
        }

        // 检查 Token 是否在白名单中
        if (!tokenWhitelistService.isInWhitelist(token)) {
            log.warn("Token 不在白名单中，可能已过期或被移除");
            return ResponsePojo.error(null, "Token 已失效");
        }

        // 从 Token 中获取用户 ID
        Integer userId = JWTUtils.getUserIdFromToken(token);
        if (userId == null) {
            log.error("从 Token 中解析用户 ID 失败");
            return ResponsePojo.error(null, "Token 无效");
        }

        String username = JWTUtils.getUsernameFromToken(token);
        log.info("开始修改用户昵称，用户 ID: {}, 用户名: {}, 新昵称: {}", userId, username, nickname);

        // 校验昵称参数
        if (nickname == null || nickname.isEmpty()) {
            log.warn("昵称为空，用户 ID: {}", userId);
            return ResponsePojo.error(null, "昵称不能为空");
        }

        if (nickname.length() < 1 || nickname.length() > 20) {
            log.warn("昵称长度不符合要求，用户 ID: {}, 昵称长度: {}", userId, nickname.length());
            return ResponsePojo.error(null, "昵称长度必须在 1-20 个字符之间");
        }

        // 调用带 AI 审核的昵称修改服务
        NicknameChangeResult result = userService.updateNicknameWithAudit(userId, nickname, userId);

        if (result.getSuccess() == null || !result.getSuccess()) {
            log.error("用户昵称修改失败，用户 ID: {}, 用户名: {}", userId, username);
            return ResponsePojo.error(false, "昵称修改失败");
        }

        Integer approvalStatus = result.getApprovalStatus();
        String auditReason = result.getAuditReason();

        // 违规内容
        if (approvalStatus != null && approvalStatus == 30) {
            String reason = auditReason != null ? auditReason : "未知原因";
            log.warn("昵称审核不通过（违规），用户 ID: {}, 原因: {}", userId, reason);
            return ResponsePojo.error(false, "违规内容：" + reason);
        }

        // 待审核
        if (approvalStatus != null && approvalStatus == 20) {
            log.info("昵称修改已提交，等待人工审核，用户 ID: {}", userId);
            return ResponsePojo.success(true, "昵称修改已提交，等待人工审核");
        }

        // 审核通过（10）
        log.info("用户昵称修改成功，用户 ID: {}, 用户名: {}, 新昵称: {}", userId, username, nickname);
        return ResponsePojo.success(true, "昵称修改成功");
    }

    /**
     * 更改账号绑定邮箱
     *
     * @param request  HTTP 请求对象，用于从 Header 或 URL 参数中获取 Token
     * @param newEmail 新邮箱地址
     * @param vCode    邮箱验证码（6位大写字母或数字）
     * @return 响应数据，表示邮箱修改是否成功
     * @author PlayerEG
     */
    @PostMapping("/email/change")
    @Operation(
        summary = "更改账号绑定邮箱接口",
        description = """
            # 更改账号绑定邮箱（需要登录认证）

            ## 特性
            - Token 认证（支持 Header 和 URL 参数两种方式）
            - 邮箱格式校验
            - 邮箱验证码验证
            - 邮箱唯一性检查
            - 自动清除旧验证码

            ## 参数说明：
            - Authorization: Header 中的 Token，格式为 `Bearer <token>`，或通过 URL 参数 `?token=<token>` 传递
            - newEmail: 新邮箱地址，字符串类型，必填，需符合标准邮箱格式
            - vCode: 邮箱验证码，6 位大写字母或数字，字符串类型，必填

            ## 返回说明：
            - **修改成功**：返回 **{"data": true}** 和“邮箱修改成功”提示
            - **Token 不存在**：返回 **{"data": null}** 和“Token 不存在”提示
            - **Token 已失效**：返回 **{"data": null}** 和“Token 已失效”提示
            - **新邮箱为空**：返回 **{"data": null}** 和“新邮箱不能为空”提示
            - **邮箱格式错误**：返回 **{"data": null}** 和“邮箱格式错误”提示
            - **验证码为空**：返回 **{"data": null}** 和“验证码不能为空”提示
            - **验证码格式错误**：返回 **{"data": null}** 和“验证码格式错误”提示
            - **验证码错误**：返回 **{"data": false}** 和“验证码错误或已过期”提示
            - **邮箱已被使用**：返回 **{"data": false}** 和“该邮箱已被其他账号使用”提示
            - **修改失败**：返回 **{"data": false}** 和“邮箱修改失败”提示

            ## 业务逻辑：
            1. 从请求头或 URL 参数中提取 Token（支持 Bearer 前缀）
            2. 验证 Token 是否在白名单中
            3. 从 Token 中解析用户 ID
            4. 校验新邮箱参数（非空、格式正确）
            5. 校验验证码参数（非空、格式正确）
            6. 检查新邮箱是否已被其他用户使用
            7. 验证邮箱验证码（使用新邮箱作为 key）
            8. 调用服务层更新用户邮箱
            9. 返回修改结果

            ## 注意事项：
            - 需要携带有效的 Token 才能修改邮箱
            - Token 必须在白名单中（未过期、未登出）
            - 新邮箱必须符合标准邮箱格式
            - 新邮箱不能被其他账号使用
            - 需要先通过 `/api/mail/send-change-email-code` 接口发送验证码到新邮箱
            - 验证码有效期为 **5 分钟**
            - 验证码验证成功后会自动失效（一次性使用）
            - 修改成功后，下次登录时需要使用新邮箱
            """
    )
    public ResponsePojo<Boolean> updateEmail(
        @Parameter(description = "HTTP 请求对象，用于从 Header 或 URL 参数中获取 Token", required = true) HttpServletRequest request,
        @Parameter(description = "新邮箱地址，需符合标准邮箱格式", required = true, example = "newemail@example.com") @RequestParam String newEmail,
        @Parameter(description = "邮箱验证码，6 位大写字母或数字", required = true, example = "ABC123") @RequestParam String vCode
    ) {
        // 提取 Token
        String token = JWTUtils.extractTokenWithLog(request, "修改邮箱接口");

        if (token == null || token.isEmpty()) {
            log.error("修改邮箱失败 - Token 不存在");
            return ResponsePojo.error(null, "Token 不存在，请在 Header 中添加 Authorization: Bearer <token> 或在 URL 参数中添加 ?token=<token>");
        }

        // 检查 Token 是否在白名单中
        if (!tokenWhitelistService.isInWhitelist(token)) {
            log.warn("Token 不在白名单中，可能已过期或被移除");
            return ResponsePojo.error(null, "Token 已失效");
        }

        // 从 Token 中获取用户 ID
        Integer userId = JWTUtils.getUserIdFromToken(token);
        if (userId == null) {
            log.error("从 Token 中解析用户 ID 失败");
            return ResponsePojo.error(null, "Token 无效");
        }

        String username = JWTUtils.getUsernameFromToken(token);
        log.info("开始修改用户邮箱，用户 ID: {}, 用户名: {}, 新邮箱: {}", userId, username, newEmail);

        // 校验新邮箱参数
        if (newEmail == null || newEmail.isEmpty()) {
            log.warn("新邮箱为空，用户 ID: {}", userId);
            return ResponsePojo.error(null, "新邮箱不能为空");
        }

        if (!RegexUtils.isEmail(newEmail)) {
            log.warn("邮箱格式错误，用户 ID: {}, 邮箱: {}", userId, newEmail);
            return ResponsePojo.error(null, "邮箱格式错误");
        }

        // 校验验证码参数
        if (vCode == null || vCode.isEmpty()) {
            log.warn("验证码为空，用户 ID: {}", userId);
            return ResponsePojo.error(null, "验证码不能为空");
        }

        if (!RegexUtils.isVCode(vCode, 6)) {
            log.warn("验证码格式错误，用户 ID: {}, 验证码长度: {}", userId, vCode.length());
            return ResponsePojo.error(null, "验证码格式错误，应为 6 位大写字母或数字");
        }

        // 调用服务层更新邮箱
        Boolean result = userService.updateUserEmail(userId, newEmail, vCode);

        if (result) {
            log.info("用户邮箱修改成功，用户 ID: {}, 用户名: {}, 新邮箱: {}", userId, username, newEmail);
            return ResponsePojo.success(true, "邮箱修改成功");
        } else {
            log.error("用户邮箱修改失败，用户 ID: {}, 用户名: {}", userId, username);
            return ResponsePojo.error(false, "邮箱修改失败，请检查验证码是否正确或邮箱是否已被使用");
        }
    }

    /**
     * 用户权限变更申请（升级/降权）
     *
     * @param request    HTTP 请求对象
     * @param targetRole 目标角色代码
     * @param vCode      邮箱验证码
     * @return 响应数据
     * @author PlayerEG
     */
    @PostMapping("/role/apply")
    @Operation(
        summary = "用户权限变更申请接口",
        description = """
            # 用户权限变更申请（需要登录认证）

            ## 特性
            - Token 认证（支持 Header 和 URL 参数两种方式）
            - 邮箱验证码验证
            - 智能识别升级/降权操作
            - 升级操作需要管理员审核
            - 降权操作立即生效

            ## 参数说明：
            - Authorization: Header 中的 Token，格式为 `Bearer <token>`，或通过 URL 参数 `?token=<token>` 传递
            - targetRole: **目标角色代码**，整数类型，必填
              * 11-普通用户
              * 22-创作者
              * 55-审核员
              * 66-工单管理员
              * 77-系统管理员
            - vCode: **邮箱验证码**，6 位大写字母或数字，字符串类型，必填

            ## 返回说明：
            - **升级申请成功**：返回 **{"data": "权限升级申请已提交，等待管理员审核"}**
            - **降权成功**：返回 **{"data": "降权成功"}**
            - **Token 不存在**：返回 **{"data": null}** 和"Token 不存在"提示
            - **Token 已失效**：返回 **{"data": null}** 和"Token 已失效"提示
            - **验证码错误**：返回 **{"data": null}** 和"验证码错误或已过期"提示
            - **非法的目标角色**：返回 **{"data": null}** 和"非法的角色代码"提示
            - **同角色操作**：返回 **{"data": null}** 和"当前已是该角色"提示

            ## 业务逻辑：
            1. 从请求头或 URL 参数中提取 Token
            2. 验证 Token 是否在白名单中
            3. 从 Token 中解析用户 ID
            4. 校验 targetRole 参数合法性
            5. 校验 vCode 验证码格式
            6. 查询用户当前角色和邮箱
            7. 验证邮箱验证码
            8. 比较当前角色与目标角色：
               - 目标角色 > 当前角色：升级操作，插入审核锁定记录（type=200）
               - 目标角色 < 当前角色：降权操作，直接更新角色并清除缓存
               - 目标角色 = 当前角色：拒绝操作
            9. 返回对应的结果消息

            ## 注意事项：
            - 需要携带有效的 Token 才能申请
            - Token 必须在白名单中（未过期、未登出）
            - 需要先通过 `/api/mail/send-role-change-code` 接口发送验证码
            - 验证码有效期为 **5 分钟**
            - 升级需要管理员审核通过后才生效
            - 降权立即生效，不可撤销
            - 角色层级：11 < 22 < 55 < 66 < 77
            """)
    public ResponsePojo<String> applyRoleChange(
        @Parameter(description = "HTTP 请求对象，用于从 Header 或 URL 参数中获取 Token", required = true) HttpServletRequest request,
        @Schema(
            description = "目标角色代码",
            allowableValues = {"11", "22", "55", "66", "77"},
            example = "22"
        ) @RequestParam Integer targetRole,
        @Parameter(description = "邮箱验证码，6 位大写字母或数字", required = true, example = "ABC123") @RequestParam String vCode
    ) {
        // 提取 Token
        String token = JWTUtils.extractTokenWithLog(request, "权限变更申请接口");

        if (token == null || token.isEmpty()) {
            log.error("权限变更申请失败 - Token 不存在");
            return ResponsePojo.error(null, "Token 不存在，请在 Header 中添加 Authorization: Bearer <token> 或在 URL 参数中添加 ?token=<token>");
        }

        // 检查 Token 是否在白名单中
        if (!tokenWhitelistService.isInWhitelist(token)) {
            log.warn("Token 不在白名单中，可能已过期或被移除");
            return ResponsePojo.error(null, "Token 已失效");
        }

        // 从 Token 中获取用户 ID
        Integer userId = JWTUtils.getUserIdFromToken(token);
        if (userId == null) {
            log.error("从 Token 中解析用户 ID 失败");
            return ResponsePojo.error(null, "Token 无效");
        }

        String username = JWTUtils.getUsernameFromToken(token);
        log.info("开始权限变更申请 - 用户ID: {}, 用户名: {}, 目标角色: {}", userId, username, targetRole);

        // 校验 targetRole 参数
        if (targetRole == null) {
            log.warn("目标角色为空，用户 ID: {}", userId);
            return ResponsePojo.error(null, "目标角色不能为空");
        }

        // 校验验证码参数
        if (vCode == null || vCode.isEmpty()) {
            log.warn("验证码为空，用户 ID: {}", userId);
            return ResponsePojo.error(null, "验证码不能为空");
        }

        if (!RegexUtils.isVCode(vCode, 6)) {
            log.warn("验证码格式错误，用户 ID: {}", userId);
            return ResponsePojo.error(null, "验证码格式错误，应为 6 位大写字母或数字");
        }

        // 查询用户信息以获取当前角色和邮箱
        User currentUser = userService.selectAllUserById(userId);
        if (currentUser == null) {
            log.error("用户不存在 - 用户ID: {}", userId);
            return ResponsePojo.error(null, "用户不存在");
        }

        Integer currentRole = currentUser.getUser_role();
        String userEmail = currentUser.getEmail();

        // 验证邮箱验证码
        boolean isCodeValid = verificationCodeServices.verificationCodeVerify(userEmail, vCode);
        if (!isCodeValid) {
            log.warn("验证码错误或已过期 - 用户ID: {}, 邮箱: {}", userId, userEmail);
            return ResponsePojo.error(null, "验证码错误或已过期");
        }

        // 判断是否为同角色操作
        if (targetRole.equals(currentRole)) {
            log.info("用户当前已是该角色 - 用户ID: {}, 角色: {}", userId, currentRole);
            return ResponsePojo.error(null, "当前已是该角色，无需变更");
        }

        // 判断升级或降权
        String resultMsg;
        if (targetRole > currentRole) {
            // 升级操作（需审核）
            resultMsg = userService.applyRoleUpgrade(userId, targetRole);
        } else {
            // 降权操作（无需审核，直接生效）
            resultMsg = userService.applyRoleDowngrade(userId, targetRole);
        }

        // 判断操作结果
        if (resultMsg.contains("成功") || resultMsg.contains("已提交")) {
            log.info("权限变更操作完成 - 用户ID: {}, 当前角色: {}, 目标角色: {}, 结果: {}",
                userId, currentRole, targetRole, resultMsg);
            return ResponsePojo.success(resultMsg, resultMsg);
        } else {
            log.warn("权限变更操作被拒绝 - 用户ID: {}, 当前角色: {}, 目标角色: {}, 原因: {}",
                userId, currentRole, targetRole, resultMsg);
            return ResponsePojo.error(null, resultMsg);
        }
    }
}
