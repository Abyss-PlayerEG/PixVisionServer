package top.playereg.pix_vision.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import top.playereg.pix_vision.pojo.ResponsePojo;
import top.playereg.pix_vision.pojo.userPojo.User;
import top.playereg.pix_vision.service.TokenWhitelistService;
import top.playereg.pix_vision.service.UserService;
import top.playereg.pix_vision.service.VerificationCodeServices;
import top.playereg.pix_vision.util.Annotation.PublicAccess;
import top.playereg.pix_vision.util.JWTUtils;
import top.playereg.pix_vision.util.PixVisionLogger;
import top.playereg.pix_vision.util.RegexUtils;
import top.playereg.pix_vision.util.StrSwitchUtils;

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
@Tag(name = "用户资料管理接口")
public class UserProfileController {
    private static final PixVisionLogger log = PixVisionLogger.create(UserProfileController.class);

    private final UserService userService;
    private final TokenWhitelistService tokenWhitelistService;
    private final VerificationCodeServices verificationCodeServices;

    /**
     * 分页查询用户信息
     *
     * @param current 当前页码（从 1 开始）
     * @param size    每页大小
     * @param keyword 关键词（可选，同时模糊查询用户名/邮箱/昵称，精确查询 UUID）
     * @return 响应数据<IPage < User>>
     * @author PlayerEG
     */
    @GetMapping("/page/{current}/{size}")
    @PublicAccess
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
            - size: 每页大小，Long 类型，必填，默认为 10，范围 1-100
            - keyword: **关键词**（可选），字符串类型
              * 如果输入的是标准 UUID 格式，则进行**精确匹配**
              * 否则同时对用户名、邮箱、昵称进行**模糊匹配**

            ## 返回说明：
            - **查询成功**：返回 **{"data": {IPage<User>对象}}** ，包含用户列表和分页信息
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
            - 每页大小限制：**1-100**
            """
    )
    public ResponsePojo<IPage<User>> getPageUserInfo(
        @Parameter(description = "当前页码，从 1 开始", example = "1") @PathVariable Long current,
        @Parameter(description = "每页大小，范围 1-100", example = "10") @PathVariable Long size,
        @Parameter(description = "关键词（可选），支持 UUID 精确查询或用户名/邮箱/昵称模糊查询") @RequestParam(required = false) String keyword
    ) {
        // 参数校验
        if (current == null || current < 1) {
            return ResponsePojo.error(null, "页码必须大于 0");
        }
        if (size == null || size < 1 || size > 100) {
            return ResponsePojo.error(null, "每页大小必须在 1-100 之间");
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
     * 按角色分页查询用户信息
     *
     * @param current   当前页码（从 1 开始）
     * @param size      每页大小
     * @param userRoles 用户角色列表（可选，支持多个角色）
     * @return 响应数据<IPage < User>>
     * @author PlayerEG
     */
    @GetMapping("/page-by-role/{current}/{size}")
    @PublicAccess
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
            - size: 每页大小，Long 类型，必填，默认为 10，范围 1-100
            - userRoles: **用户角色列表**（可选），Integer 数组类型，支持传递多个角色
              * 11-普通用户, 22-创作者, 55-审核员, 66-工单管理员, 77-系统管理员
              * 可以传递单个角色或多个角色，如：?userRoles=11 或 ?userRoles=11&userRoles=22

            ## 返回说明：
            - **查询成功**：返回 **{"data": {IPage<User>对象}}** ，包含用户列表和分页信息
            - **参数错误**：返回 **{"data": null}** 和"页码或每页大小错误"提示
            - **无符合条件的用户**：返回 **{"data": null}** 和"查询失败，返回结果为空"提示

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
            - 每页大小限制：**1-100**
            - 合法的角色代码：11-普通用户, 22-创作者, 55-审核员, 66-工单管理员, 77-系统管理员

            ## 使用示例：
            ```
            # 示例1：查询所有用户（无角色筛选）
            GET /api/user/profile/page-by-role/1/10

            # 示例2：查询普通用户（单个角色）
            GET /api/user/profile/page-by-role/1/10?userRoles=11

            # 示例3：查询普通用户和创作者（多个角色）
            GET /api/user/profile/page-by-role/1/10?userRoles=11&userRoles=22

            # 示例4：查询审核员和系统管理员
            GET /api/user/profile/page-by-role/1/10?userRoles=55&userRoles=77
            ```
            """
    )
    public ResponsePojo<IPage<User>> getPageUserInfoByRole(
        @Parameter(description = "当前页码，从 1 开始", example = "1") @PathVariable Long current,
        @Parameter(description = "每页大小，范围 1-100", example = "10") @PathVariable Long size,
        @Parameter(description = "用户角色列表（可选），支持多个角色", example = "11") @RequestParam(required = false) List<Integer> userRoles
    ) {
        // 参数校验
        if (current == null || current < 1) {
            return ResponsePojo.error(null, "页码必须大于 0");
        }
        if (size == null || size < 1 || size > 100) {
            return ResponsePojo.error(null, "每页大小必须在 1-100 之间");
        }

        log.info("按角色分页查询用户信息 - 页码: {}, 每页: {}, 角色列表: {}", current, size, userRoles != null ? userRoles.toString() : "无");

        // 构建分页对象
        Page<User> page = new Page<>(current, size);

        // 调用服务层查询用户信息
        IPage<User> result = userService.selectPageUserInfoByRole(page, userRoles);

        // 返回结果为空，则返回错误信息
        if (result == null || result.getRecords().isEmpty()) {
            log.warn("按角色分页查询返回结果为空 - 页码：{}, 每页：{}, 角色：{}", current, size, userRoles != null ? userRoles.toString() : "无");
            return ResponsePojo.error(null, "查询失败，返回结果为空");
        }

        // 将用户的 16 字节二进制UUID转换成字符串UUID
        for (User user : result.getRecords()) {
            user.setString_user_uuid(StrSwitchUtils.bytes2Uuid(user.getUser_uuid()));
        }

        log.info("按角色分页查询成功 - 页码：{}, 每页：{}, 总数：{}, 返回：{}",
            current, size, result.getTotal(), result.getRecords().size());

        return ResponsePojo.success(result, "查询成功");
    }

    /**
     * 修改用户昵称
     *
     * @param request  HTTP 请求对象，用于从 Header 或 URL 参数中获取 Token
     * @param nickname 新昵称
     * @return 修改结果
     * @author Playereg
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

            ## 参数说明：
            - Authorization: Header 中的 Token，格式为 `Bearer <token>`，或通过 URL 参数 `?token=<token>` 传递
            - nickname: 新昵称，字符串类型，必填，长度 1-20 个字符

            ## 返回说明：
            - **修改成功**：返回 **{"data": true}** 和"昵称修改成功"提示
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
            5. 调用服务层更新用户昵称
            6. 返回修改结果

            ## 注意事项：
            - 需要携带有效的 Token 才能修改昵称
            - Token 必须在白名单中（未过期、未登出）
            - 昵称长度限制：**1-20 个字符**
            - 昵称可以包含中文、英文、数字和特殊字符
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

        // 调用服务层更新昵称
        Boolean result = userService.updateUserNickname(userId, nickname);

        if (result) {
            log.info("用户昵称修改成功，用户 ID: {}, 用户名: {}, 新昵称: {}", userId, username, nickname);
            return ResponsePojo.success(true, "昵称修改成功");
        } else {
            log.error("用户昵称修改失败，用户 ID: {}, 用户名: {}", userId, username);
            return ResponsePojo.error(false, "昵称修改失败");
        }
    }

    /**
     * 更改账号绑定邮箱
     *
     * @param request  HTTP 请求对象，用于从 Header 或 URL 参数中获取 Token
     * @param newEmail 新邮箱
     * @param vCode    邮箱验证码
     * @return 修改结果
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
}
