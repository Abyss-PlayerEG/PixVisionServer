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
import top.playereg.pix_vision.service.TokenWhitelistService;
import top.playereg.pix_vision.service.UserService;
import top.playereg.pix_vision.util.JWTUtils;
import top.playereg.pix_vision.util.RegexUtils;
import top.playereg.pix_vision.util.StrSwitchUtils;
import top.playereg.pix_vision.util.TokenUtils;

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
@Tag(name = "用户资料管理相关接口")
public class UserProfileController {
    private static final Logger log = LoggerFactory.getLogger(UserProfileController.class);

    private final UserService userService;
    private final TokenWhitelistService tokenWhitelistService;

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
            # 分页查询用户信息（需要登录认证）

            ## 特性
            - Token 认证（通过拦截器自动验证）
            - MyBatis-Plus 分页支持
            - 多条件组合查询（用户名/UUID/邮箱）
            - 模糊匹配与精确匹配
            - UUID 二进制转换

            ## 参数说明：
            - current: 当前页码，**从 1 开始**，Long 类型，必填，默认为 1
            - size: 每页大小，Long 类型，必填，默认为 10，范围 1-100
            - username: 用户名（可选），字符串类型，支持模糊查询
            - uuid: 用户 UUID（可选），字符串类型，支持精确查询
            - email: 邮箱（可选），字符串类型，支持模糊查询

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
     * 修改用户昵称
     *
     * @param request  HTTP 请求对象，用于从 Header 或 URL 参数中获取 Token
     * @param nickname 新昵称
     * @return 修改结果
     * @author Playereg
     */
    @PostMapping("/change/nickname")
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
        String token = TokenUtils.extractTokenWithLog(request, "修改昵称接口");

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
}
