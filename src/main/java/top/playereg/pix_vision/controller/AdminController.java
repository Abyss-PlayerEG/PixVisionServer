package top.playereg.pix_vision.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.playereg.pix_vision.pojo.ResponsePojo;
import top.playereg.pix_vision.pojo.userPojo.User;
import top.playereg.pix_vision.service.TokenWhitelistService;
import top.playereg.pix_vision.service.UserService;
import top.playereg.pix_vision.util.Annotation.RequireRole;
import top.playereg.pix_vision.util.JWTUtils;
import top.playereg.pix_vision.util.PixVisionLogger;
import top.playereg.pix_vision.util.RegexUtils;

/**
 * 系统管理员控制器 - 提供系统管理相关的接口
 * <p>
 * 该控制器下的所有接口仅允许系统管理员（角色代码 77）访问
 * </p>
 *
 * @author PlayerEG
 * @see top.playereg.pix_vision.util.Annotation.RequireRole
 * @since DEV-2.0.0
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "系统管理员相关接口")
@RequireRole(value = {77})
public class AdminController {
    private static final PixVisionLogger log = PixVisionLogger.create(AdminController.class);

    private final UserService userService;
    private final TokenWhitelistService tokenWhitelistService;

    /**
     * 刷新所有用户权限缓存
     * <p>
     * 清除 Redis 中所有 role: 前缀的用户角色缓存，确保所有用户的权限验证获取最新的角色信息
     * </p>
     *
     * @return 操作结果
     * @author PlayerEG
     */
    @Operation(
        summary = "刷新所有用户权限缓存",
        description = """
            # 刷新所有用户权限缓存（需要登录认证 + 角色权限[77]）

            ## 特性
            - 仅系统管理员可调用
            - 清除所有用户的角色缓存（role:* 键）
            - 确保所有用户下次请求时获取最新的角色信息
            - 适用于批量修改用户角色后的缓存清理

            ## 参数说明：
            - 无需参数

            ## 返回说明：
            - **成功**：返回 **{"data": 清除数量}** 和"权限缓存刷新成功"提示
            - **失败**：返回 **{"data": null}** 和错误提示

            ## 业务逻辑：
            1. 验证当前用户是否为系统管理员
            2. 使用 SCAN 命令查找所有 role: 前缀的键
            3. 批量删除所有匹配的角色缓存键
            4. 返回清除的缓存数量

            ## 注意事项：
            - 此操作会影响所有在线用户的权限验证
            - 清除后，所有用户下次请求时会重新从数据库加载角色信息
            - 建议在批量修改用户角色后调用
            - 使用 SCAN 而非 KEYS，避免阻塞 Redis
            - 缓存会在后续请求中自动重建

            ## 使用场景：
            - 批量修改多个用户角色后
            - 系统权限配置变更后
            - 需要强制所有用户重新验证权限时
            """
    )
    @PostMapping("/refresh-permission-cache")
    public ResponsePojo<Integer> refreshPermissionCache() {
        log.info("管理员开始刷新所有用户权限缓存");

        try {
            // 调用 Service 层清除所有角色缓存
            int clearedCount = userService.clearAllUserRoleCache();

            log.info("所有用户权限缓存刷新成功 - 清除缓存数量: {}", clearedCount);
            return ResponsePojo.success(clearedCount, "权限缓存刷新成功，共清除 " + clearedCount + " 个缓存");
        } catch (Exception e) {
            log.error("刷新权限缓存失败 - 错误: {}", e.getMessage(), e);
            return ResponsePojo.error(null, "权限缓存刷新失败: " + e.getMessage());
        }
    }

    /**
     * 更新用户信息（角色和/或状态）
     * <p>
     * 系统管理员可以修改指定用户的角色和/或状态，支持的操作：
     * - 仅修改角色
     * - 仅修改状态
     * - 同时修改角色和状态
     * </p>
     *
     * @param request      HTTP 请求对象（用于获取当前管理员信息）
     * @param targetUserId 目标用户 ID
     * @param newRole      新角色代码（可选）
     * @param newStatus    新状态代码（可选）
     * @return 操作结果
     * @author PlayerEG
     */
    @Operation(
        summary = "更新用户信息（角色或状态）",
        description = """
            # 更新用户信息（需要登录认证 + 角色权限[77]）

            ## 特性
            - 仅系统管理员可调用
            - 可以修改任意用户的角色和/或状态（除自己外）
            - 支持单独修改角色、单独修改状态或同时修改
            - 自动清除目标用户的角色缓存
            - 修改状态时自动移除该用户所有 Token，强制所有设备下线
            - 记录操作日志（通过 update_user 字段）

            ## 参数说明：
            - **targetUserId**: 目标用户 ID（必填）
            - **newRole**: 新角色代码（可选），可选值：
              - 11: 普通用户
              - 22: 创作者
              - 55: 审核员
              - 66: 工单管理员
              - 77: 系统管理员
            - **newStatus**: 新状态代码（可选），可选值：
              - 10: 正常
              - 20: 冻结
              - 30: 封禁

            ## 返回说明：
            - **成功**：返回 **{"data": true}** 和操作详情提示
            - **失败**：返回 **{"data": false}** 和错误提示

            ## 业务逻辑：
            1. 验证当前用户是否为系统管理员（由拦截器自动验证）
            2. 从 Token 中获取当前管理员 ID
            3. 验证目标用户是否存在
            4. 如果提供了 newRole，验证并更新用户角色
            5. 如果提供了 newStatus，验证并更新用户状态
            6. 清除目标用户的角色缓存（如果修改了角色）
            7. 将该用户所有 Token 从白名单移除（如果修改了状态）
            8. 返回操作结果

            ## 安全限制：
            - 管理员不能修改自己的角色或状态
            - 目标用户必须存在且未被删除
            - 至少需要提供 newRole 或 newStatus 中的一个
            - 角色代码和状态代码必须在允许范围内

            ## 注意事项：
            - 修改角色后，目标用户下次请求时会自动获取新的角色信息
            - 修改状态后，会强制该用户所有设备下线
            - 如果需要立即生效，可以调用 /refresh-permission-cache 接口
            - 建议谨慎分配高权限角色（如 77-系统管理员）
            - 冻结状态（20）：用户无法登录，但数据保留
            - 封禁状态（30）：用户无法登录，严重违规使用
            """
    )
    @PostMapping("/update-user-info")
    public ResponsePojo<Boolean> updateUserInfo(
        HttpServletRequest request,
        @Parameter(description = "目标用户 ID", required = true, example = "123")
        @RequestParam Integer targetUserId,
        @Schema(
            description = "新角色代码（可选，11-普通用户, 22-创作者, 55-审核员, 66-工单管理员, 77-系统管理员）",
            allowableValues = {"11", "22", "55", "66", "77"},
            example = "22"
        )
        @RequestParam(required = false) Integer newRole,
        @Schema(
            description = "新状态代码（可选，10-正常, 20-冻结, 30-封禁）",
            allowableValues = {"10", "20", "30"},
            example = "20"
        )
        @RequestParam(required = false) Integer newStatus
    ) {
        log.info("管理员开始更新用户信息 - 目标用户 ID: {}, 新角色: {}, 新状态: {}", targetUserId, newRole, newStatus);

        // 参数校验：至少提供一个更新字段
        if (newRole == null && newStatus == null) {
            log.warn("未提供任何更新字段 - 目标用户 ID: {}", targetUserId);
            return ResponsePojo.error(false, "请至少提供 newRole 或 newStatus 中的一个参数");
        }

        try {
            // 从 Token 中获取当前管理员 ID
            String token = JWTUtils.extractTokenWithLog(request, "更新用户信息");
            if (token == null || token.isEmpty()) {
                log.error("Token 不存在");
                return ResponsePojo.error(false, "未授权访问");
            }

            Integer adminId = JWTUtils.getUserIdFromToken(token);
            if (adminId == null) {
                log.error("无法从 Token 中获取管理员 ID");
                return ResponsePojo.error(false, "Token 无效");
            }

            log.info("当前管理员 ID: {}", adminId);

            // 检查目标用户是否存在
            User targetUser = userService.selectAllUserById(targetUserId);
            if (targetUser == null) {
                log.warn("目标用户不存在 - 用户 ID: {}", targetUserId);
                return ResponsePojo.error(false, "目标用户不存在");
            }

            // 防止修改自己的信息
            if (targetUserId.equals(adminId)) {
                log.warn("管理员不能修改自己的信息 - 用户 ID: {}", adminId);
                return ResponsePojo.error(false, "不能修改自己的信息");
            }

            boolean roleUpdated = false;
            boolean statusUpdated = false;
            StringBuilder messageBuilder = new StringBuilder();

            // 更新角色
            if (newRole != null) {
                log.info("开始更新用户角色 - 目标用户 ID: {}, 新角色: {}", targetUserId, newRole);
                Boolean roleSuccess = userService.updateUserRole(targetUserId, newRole, adminId);
                if (!roleSuccess) {
                    log.warn("用户角色更新失败 - 目标用户 ID: {}", targetUserId);
                    return ResponsePojo.error(false, "用户角色更新失败，请检查角色代码是否正确");
                }
                roleUpdated = true;
                String roleName = getRoleName(newRole);
                messageBuilder.append("角色已更新为：").append(roleName);
                log.info("用户角色更新成功 - 目标用户 ID: {}, 新角色: {}", targetUserId, newRole);
            }

            // 更新状态
            if (newStatus != null) {
                log.info("开始更新用户状态 - 目标用户 ID: {}, 新状态: {}", targetUserId, newStatus);
                Boolean statusSuccess = userService.updateUserStatus(targetUserId, newStatus, adminId);
                if (!statusSuccess) {
                    log.warn("用户状态更新失败 - 目标用户 ID: {}", targetUserId);
                    return ResponsePojo.error(false, "用户状态更新失败，请检查状态代码是否正确");
                }
                statusUpdated = true;

                // 将该用户所有 Token 从白名单移除，强制下线
                int removedCount = tokenWhitelistService.removeAllUserTokens(targetUserId, targetUser.getUsername());
                log.info("已移除用户所有 Token，用户 ID: {}, 用户名: {}, 移除数量: {}",
                    targetUserId, targetUser.getUsername(), removedCount);

                if (messageBuilder.length() > 0) {
                    messageBuilder.append("，");
                }
                String statusName = getStatusName(newStatus);
                messageBuilder.append("状态已更新为：").append(statusName).append("，已强制该用户所有设备下线");
                log.info("用户状态更新成功 - 目标用户 ID: {}, 新状态: {}", targetUserId, newStatus);
            }

            // 构建返回消息
            String message = messageBuilder.toString();
            if (message.isEmpty()) {
                message = "用户信息更新成功";
            } else {
                message = "用户信息更新成功，" + message;
            }

            log.info("用户信息更新完成 - 目标用户 ID: {}, 角色更新: {}, 状态更新: {}", targetUserId, roleUpdated, statusUpdated);
            return ResponsePojo.success(true, message);

        } catch (Exception e) {
            log.error("更新用户信息异常 - 错误: {}", e.getMessage(), e);
            return ResponsePojo.error(false, "系统错误: " + e.getMessage());
        }
    }

    /**
     * 删除用户账户
     * <p>
     * 系统管理员可以删除指定用户的账户（逻辑删除），并清除该用户的所有 Token
     * </p>
     *
     * @param request      HTTP 请求对象（用于获取当前管理员信息）
     * @param targetUserId 目标用户 ID
     * @return 操作结果
     * @author PlayerEG
     */
    @Operation(
        summary = "删除用户账户",
        description = """
            # 删除用户账户（需要登录认证 + 角色权限[77]）

            ## 特性
            - 仅系统管理员可调用
            - 逻辑删除用户账户（is_delete = 1）
            - 自动清除该用户的所有 Token，强制所有设备下线
            - 清除用户角色缓存
            - 记录操作日志（通过 update_user 字段）

            ## 参数说明：
            - **targetUserId**: 目标用户 ID（必填）

            ## 返回说明：
            - **成功**：返回 **{"data": true}** 和“用户账户删除成功”提示
            - **失败**：返回 **{"data": false}** 和错误提示

            ## 业务逻辑：
            1. 验证当前用户是否为系统管理员（由拦截器自动验证）
            2. 从 Token 中获取当前管理员 ID
            3. 验证目标用户是否存在
            4. 执行逻辑删除（is_delete = 1）
            5. 清除该用户的所有 Token
            6. 清除用户角色缓存
            7. 返回操作结果

            ## 安全限制：
            - 管理员不能删除自己的账户
            - 目标用户必须存在且未被删除
            - 删除后用户无法登录，但数据保留在数据库中

            ## 注意事项：
            - 这是逻辑删除，不是物理删除
            - 删除后用户的所有 Token 立即失效
            - 建议谨慎使用，确保有充分的理由
            - 删除操作不可逆（除非手动修改数据库）
            """
    )
    @PostMapping("/delete-user")
    public ResponsePojo<Boolean> deleteUser(
        HttpServletRequest request,
        @Parameter(description = "目标用户 ID", required = true, example = "123")
        @RequestParam Integer targetUserId
    ) {
        log.info("管理员开始删除用户账户 - 目标用户 ID: {}", targetUserId);

        try {
            // 从 Token 中获取当前管理员 ID
            String token = JWTUtils.extractTokenWithLog(request, "删除用户账户");
            if (token == null || token.isEmpty()) {
                log.error("Token 不存在");
                return ResponsePojo.error(false, "未授权访问");
            }

            Integer adminId = JWTUtils.getUserIdFromToken(token);
            if (adminId == null) {
                log.error("无法从 Token 中获取管理员 ID");
                return ResponsePojo.error(false, "Token 无效");
            }

            log.info("当前管理员 ID: {}", adminId);

            // 调用 Service 层删除用户账户
            Boolean success = userService.deleteUserAccountByAdmin(targetUserId, adminId);

            if (success) {
                log.info("用户账户删除成功 - 目标用户 ID: {}", targetUserId);
                return ResponsePojo.success(true, "用户账户删除成功，已强制该用户所有设备下线");
            } else {
                log.warn("用户账户删除失败 - 目标用户 ID: {}", targetUserId);
                return ResponsePojo.error(false, "用户账户删除失败，请检查用户是否存在或是否已被删除");
            }
        } catch (Exception e) {
            log.error("删除用户账户异常 - 错误: {}", e.getMessage(), e);
            return ResponsePojo.error(false, "系统错误: " + e.getMessage());
        }
    }

    /**
     * 创建新用户
     * <p>
     * 系统管理员可以直接创建新用户，无需验证码验证
     * </p>
     *
     * @param request         HTTP 请求对象（用于获取当前管理员信息）
     * @param username        用户名
     * @param password        密码（明文）
     * @param confirmPassword 确认密码
     * @param nickname        昵称
     * @param email           邮箱
     * @param role            角色代码（可选，默认为 11-普通用户）
     * @param status          状态代码（可选，默认为 10-正常）
     * @return 操作结果
     * @author PlayerEG
     */
    @Operation(
        summary = "创建新用户",
        description = """
            # 创建新用户（需要登录认证 + 角色权限[77]）

            ## 特性
            - 仅系统管理员可调用
            - 无需验证码验证
            - 自动加密密码（SHA-256）
            - 自动生成 UUID 和随机头像
            - 可指定角色和状态
            - 记录创建者（adminId）
            - 密码二次确认

            ## 参数说明：
            - **username**: 用户名（必填，唯一）
            - **password**: 密码（必填，明文，将自动加密）
            - **confirmPassword**: 确认密码（必填，必须与 password 一致）
            - **nickname**: 昵称（必填）
            - **email**: 邮箱（必填，唯一）
            - **role**: 角色代码（可选，默认 11），可选值：
              - 11: 普通用户
              - 22: 创作者
              - 55: 审核员
              - 66: 工单管理员
              - 77: 系统管理员
            - **status**: 状态代码（可选，默认 10），可选值：
              - 10: 正常
              - 20: 冻结
              - 30: 封禁

            ## 返回说明：
            - **成功**：返回 **{"data": {User 对象}}** 和"用户创建成功"提示
            - **两次密码不一致**：返回 **{"data": null}** 和"两次输入的密码不一致"提示
            - **失败**：返回 **{"data": null}** 和错误提示

            ## 业务逻辑：
            1. 验证当前用户是否为系统管理员（由拦截器自动验证）
            2. 从 Token 中获取当前管理员 ID
            3. 验证参数合法性
            4. 验证两次输入的密码是否一致
            5. 检查用户名和邮箱是否已存在
            6. 加密密码（SHA-256）
            7. 生成 UUID 和随机头像
            8. 创建用户记录
            9. 返回创建的用户信息

            ## 安全限制：
            - 用户名必须唯一
            - 邮箱必须唯一
            - 密码会自动加密，不会存储明文
            - 角色和状态代码必须在允许范围内
            - 两次输入的密码必须完全一致

            ## 注意事项：
            - 管理员可以直接设置用户角色，包括高权限角色
            - 建议谨慎分配高权限角色（如 77-系统管理员）
            - 创建的用户初始状态为正常，可以立即登录
            - 密码在传输过程中建议使用 HTTPS 加密
            - 两次输入的密码必须完全一致
            """
    )
    @PostMapping("/create-user")
    public ResponsePojo<Boolean> createUser(
        HttpServletRequest request,
        @Parameter(description = "用户名（必填，唯一）", required = true, example = "new_user") @RequestParam String username,
        @Parameter(description = "密码（必填，明文，将自动加密）", required = true, example = "123456") @RequestParam String password,
        @Parameter(description = "确认密码（必填，必须与密码一致）", required = true, example = "123456") @RequestParam String confirmPassword,
        @Parameter(description = "昵称（必填）", required = true, example = "新用户") @RequestParam String nickname,
        @Parameter(description = "邮箱（必填，唯一）", required = true, example = "newuser@example.com") @RequestParam String email,
        @Schema(
            description = "角色代码（可选，默认 11-普通用户）",
            allowableValues = {"11", "22", "55", "66", "77"},
            example = "11"
        ) @RequestParam(required = false, defaultValue = "11") Integer role,
        @Schema(
            description = "状态代码（可选，默认 10-正常）",
            allowableValues = {"10", "20", "30"},
            example = "10"
        ) @RequestParam(required = false, defaultValue = "10") Integer status
    ) {
        log.info("管理员开始创建新用户 - 用户名: {}, 邮箱: {}", username, email);

        try {
            // 参数验证
            if (!RegexUtils.isUsername(username)) {
                return ResponsePojo.error(false, "用户名格式不正确");
            }
            if (!RegexUtils.isEmail(email)) {
                return ResponsePojo.error(false, "邮箱格式不正确");
            }
            if (!RegexUtils.isPassword(password) || !RegexUtils.isPassword(confirmPassword)) {
                return ResponsePojo.error(false, "密码格式不正确");
            }

            // 验证两次密码是否一致
            if (!password.equals(confirmPassword)) {
                log.warn("两次输入的密码不一致 - 用户名: {}", username);
                return ResponsePojo.error(false, "两次输入的密码不一致");
            }

            // 从 Token 中获取当前管理员 ID
            String token = JWTUtils.extractTokenWithLog(request, "创建新用户");
            if (token == null || token.isEmpty()) {
                log.error("Token 不存在");
                return ResponsePojo.error(false, "未授权访问");
            }

            Integer adminId = JWTUtils.getUserIdFromToken(token);
            if (adminId == null) {
                log.error("无法从 Token 中获取管理员 ID");
                return ResponsePojo.error(false, "Token 无效");
            }

            log.info("当前管理员 ID: {}", adminId);

            // 调用 Service 层创建用户（Service 层负责所有数据处理）
            User newUser = userService.createUserByAdmin(username, password, nickname, email, role, status, adminId);

            if (newUser != null) {
                log.info("用户创建成功 - 用户名: {}, 用户 ID: {}", username, newUser.getUser_id());
                return ResponsePojo.success(true, "用户创建成功");
            } else {
                log.warn("用户创建失败 - 用户名: {}", username);
                return ResponsePojo.error(false, "用户创建失败，请检查用户名或邮箱是否已存在，以及参数是否正确");
            }
        } catch (Exception e) {
            log.error("创建用户异常 - 错误: {}", e.getMessage(), e);
            return ResponsePojo.error(false, "系统错误: " + e.getMessage());
        }
    }

    // ================================================================================

    /**
     * 根据角色代码获取角色名称
     *
     * @param roleCode 角色代码
     * @return 角色名称
     */
    private String getRoleName(Integer roleCode) {
        if (roleCode == null) {
            return "未知";
        }
        return switch (roleCode) {
            case 11 -> "普通用户";
            case 22 -> "创作者";
            case 55 -> "审核员";
            case 66 -> "工单管理员";
            case 77 -> "系统管理员";
            default -> "未知角色(" + roleCode + ")";
        };
    }

    /**
     * 根据状态代码获取状态名称
     *
     * @param statusCode 状态代码
     * @return 状态名称
     */
    private String getStatusName(Integer statusCode) {
        if (statusCode == null) {
            return "未知";
        }
        return switch (statusCode) {
            case 10 -> "正常";
            case 20 -> "冻结";
            case 30 -> "封禁";
            default -> "未知状态(" + statusCode + ")";
        };
    }
}
