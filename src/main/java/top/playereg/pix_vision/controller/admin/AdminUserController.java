package top.playereg.pix_vision.controller.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
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
import top.playereg.pix_vision.pojo.adminPojo.AdminBatchDeleteUserResult;
import top.playereg.pix_vision.pojo.adminPojo.AdminBatchUpdateUserResult;
import top.playereg.pix_vision.pojo.adminPojo.AdminResetPasswordResult;
import top.playereg.pix_vision.pojo.userPojo.User;
import top.playereg.pix_vision.service.EmailService;
import top.playereg.pix_vision.service.EmailTemplateService;
import top.playereg.pix_vision.service.TokenWhitelistService;
import top.playereg.pix_vision.service.UserService;
import top.playereg.pix_vision.util.Annotation.RequireRole;
import top.playereg.pix_vision.util.JWTUtils;
import top.playereg.pix_vision.util.PixVisionLogger;
import top.playereg.pix_vision.util.RegexUtils;
import top.playereg.pix_vision.util.StrSwitchUtils;

import java.util.*;

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
@RequestMapping("/api/admin/user")
@RequiredArgsConstructor
@Tag(name = "系统管理员接口 - 用户管理", description = "提供用户管理的后台接口，包括批量更新、删除、创建用户等操作")
@RequireRole(value = {55, 77})
public class AdminUserController {
    private static final PixVisionLogger log = PixVisionLogger.create(AdminUserController.class);

    private final UserService userService;
    private final TokenWhitelistService tokenWhitelistService;
    private final EmailService emailService;
    private final EmailTemplateService emailTemplateService;

    /**
     * 刷新所有用户权限缓存
     *
     * @return 响应数据，包含清除的缓存数量
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
            - **成功**：返回清除的缓存数量
            - **失败**：返回错误提示

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
    @RequireRole(value = {77})
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
     * 批量更新用户信息（角色、状态、头像和/或重置昵称）
     *
     * @param request       HTTP 请求对象（用于获取当前管理员信息）
     * @param userIds       目标用户 ID 列表
     * @param newRole       新角色代码（可选，11-77）
     * @param newStatus     新状态代码（可选，10-30）
     * @param newAvatar     新用户头像（可选）
     * @param resetNickname 是否重置用户昵称（可选，默认 true）
     * @return 响应数据，包含批量更新的统计信息
     * @author blue_sky_ks
     */
    @Operation(
        summary = "批量更新用户信息",
        description = """
            # 批量更新用户信息（需要登录认证 + 角色权限[77]）

            ## 特性
            - 仅系统管理员可调用
            - 支持批量修改用户的角色、状态、头像和/或重置昵称
            - 重置昵称时自动调用随机昵称生成方法，生成 "user_" 前缀的随机昵称
            - 自动清除被更新用户的角色缓存
            - 修改状态时自动移除该用户所有 Token，强制所有设备下线
            - 记录操作日志（update_user 字段设置为执行操作的管理员 ID）

            ## 参数说明：
            - **userIds**: 目标用户 ID 列表（必填）
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
            - **newAvatar**: 新用户头像路径（可选）
            - **resetNickname**: 是否重置用户昵称（可选，默认 true），设为 false 时不重置

            ## 返回说明：
            - **成功**：返回包含总数、成功数、失败 ID 列表的统计信息
            - **失败**：返回错误提示

            ## 业务逻辑：
            1. 验证当前用户是否为系统管理员（由拦截器自动验证）
            2. 从 Token 中获取当前管理员 ID
            3. 遍历用户 ID 列表，逐个执行更新逻辑
            4. 清除每个被更新用户的角色缓存
            5. 如果修改了状态，强制下线该用户的所有 Token
            6. 返回包含总数、成功数和失败 ID 列表的结果

            ## 安全限制：
            - 管理员不能修改自己的信息
            - 目标用户必须存在且未被删除
            - 至少需要提供 newRole、newStatus、newAvatar 或 resetNickname 中的一个

            ## 注意事项：
            - 修改角色后，目标用户下次请求时会自动获取新的角色信息
            - 修改状态后，会强制该用户所有设备下线
            - 建议谨慎分配高权限角色（如 77-系统管理员）
            - 重置昵称时生成的随机昵称格式为 "user_" + 10位随机字母数字组合
            """
    )
    @RequireRole(value = {77})
    @PostMapping("/update/user-role-status")
    public ResponsePojo<AdminBatchUpdateUserResult> batchUpdateUserInfo(
        HttpServletRequest request,
        @Parameter(description = "目标用户 ID 列表", required = true, example = "123,456") @RequestParam List<Integer> userIds,
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
        @RequestParam(required = false) Integer newStatus,
        @Schema(
            description = "用户头像路径（可选）",
            example = "default/1.png"
        )
        @RequestParam(required = false) String newAvatar,
        @Parameter(description = "用户名称重置")
        @RequestParam(required = false, defaultValue = "true") boolean resetNickname
    ) {
        log.info("管理员开始批量更新用户信息 - 用户数量: {}, 新角色: {}, 新状态: {}, 新头像: {}, 重置昵称: {}",
            userIds != null ? userIds.size() : 0, newRole, newStatus, newAvatar, resetNickname);

        // 参数校验
        if (userIds == null || userIds.isEmpty()) {
            log.warn("用户 ID 列表为空");
            return ResponsePojo.error(null, "用户 ID 列表不能为空");
        }

        if (newRole == null && newStatus == null && newAvatar == null && !resetNickname) {
            log.warn("未提供任何更新字段");
            return ResponsePojo.error(null, "请至少提供 newRole、newStatus、newAvatar 或 resetNickname 中的一个参数");
        }

        try {
            // 从 Token 中获取当前管理员 ID
            String token = JWTUtils.extractTokenWithLog(request, "批量更新用户信息");
            if (token == null || token.isEmpty()) {
                log.error("Token 不存在");
                return ResponsePojo.error(null, "未授权访问");
            }

            Integer adminId = JWTUtils.getUserIdFromToken(token);
            if (adminId == null) {
                log.error("无法从 Token 中获取管理员 ID");
                return ResponsePojo.error(null, "Token 无效");
            }

            int totalCount = userIds.size();
            java.util.Set<Integer> successIds = new java.util.HashSet<>();

            // 循环调用 Service 层更新方法
            for (Integer targetUserId : userIds) {
                // 防止修改自己的信息
                if (targetUserId.equals(adminId)) {
                    log.warn("管理员不能修改自己的信息，跳过 - 用户 ID: {}", adminId);
                    continue;
                }

                boolean success = true;

                // 更新角色
                if (newRole != null) {
                    Boolean roleRes = userService.updateUserRole(targetUserId, newRole, adminId);
                    if (!roleRes) success = false;
                }

                // 更新状态
                if (newStatus != null) {
                    Boolean statusRes = userService.updateUserStatus(targetUserId, newStatus, adminId);
                    if (!statusRes) success = false;

                    // 如果状态更新成功，强制下线
                    if (statusRes) {
                        User targetUser = userService.selectAllUserById(targetUserId);
                        if (targetUser != null) {
                            tokenWhitelistService.removeAllUserTokens(targetUserId, targetUser.getUsername());
                        }
                    }
                }

                // 更新头像
                if (newAvatar != null) {
                    Boolean avatarRes = userService.updateUserAvatar(targetUserId, newAvatar, adminId);
                    if (!avatarRes) success = false;
                }

                // 重置昵称（随机生成）
                if (resetNickname) {
                    String generatedNickname = StrSwitchUtils.generateRandomUserDefaultNickName("user");
                    Boolean nicknameRes = userService.updateUserNickname(targetUserId, generatedNickname, adminId);
                    if (!nicknameRes) success = false;
                }

                if (success) {
                    successIds.add(targetUserId);
                }
            }

            // 计算失败的 ID
            java.util.List<Integer> failedUserIds = new java.util.ArrayList<>();
            for (Integer id : userIds) {
                if (!successIds.contains(id)) {
                    failedUserIds.add(id);
                }
            }

            AdminBatchUpdateUserResult result = new AdminBatchUpdateUserResult(totalCount, successIds.size(), failedUserIds);
            log.info("批量更新用户信息完成 - 总数: {}, 成功: {}, 失败: {}", totalCount, successIds.size(), failedUserIds.size());

            return ResponsePojo.success(result, "批量更新用户信息处理完成");

        } catch (Exception e) {
            log.error("批量更新用户信息异常 - 错误: {}", e.getMessage(), e);
            return ResponsePojo.error(null, "系统错误: " + e.getMessage());
        }
    }

    /**
     * 批量删除用户账户
     *
     * @param request HTTP 请求对象（用于获取当前管理员信息）
     * @param userIds 目标用户 ID 列表
     * @return 响应数据，包含批量删除的统计信息
     * @author PlayerEG
     */
    @Operation(
        summary = "批量删除用户账户",
        description = """
            # 批量删除用户账户（需要登录认证 + 角色权限[77]）

            ## 特性
            - 仅系统管理员可调用
            - 支持批量逻辑删除用户账户（is_delete = 1）
            - 自动清除被删用户的所有 Token，强制所有设备下线
            - 清除被删用户的角色缓存
            - 记录操作日志（update_user 字段设置为执行操作的管理员 ID）

            ## 参数说明：
            - **userIds**: 目标用户 ID 列表（必填）

            ## 返回说明：
            - **成功**：返回包含总数、成功数、失败 ID 列表的统计信息
            - **失败**：返回错误提示

            ## 业务逻辑：
            1. 验证当前用户是否为系统管理员（由拦截器自动验证）
            2. 从 Token 中获取当前管理员 ID
            3. 遍历用户 ID 列表，逐个执行逻辑删除
            4. 清除每个被删用户的所有 Token
            5. 清除每个被删用户的角色缓存
            6. 返回包含总数、成功数和失败 ID 列表的结果

            ## 安全限制：
            - 管理员不能删除自己的账户
            - 目标用户必须存在且未被删除
            - 删除后用户无法登录，但数据保留在数据库中

            ## 注意事项：
            - 这是逻辑删除，不是物理删除
            - 删除后用户的所有 Token 立即失效
            - 建议谨慎使用，确保有充分的理由
            """
    )
    @RequireRole(value = {77})
    @PostMapping("/delete")
    public ResponsePojo<AdminBatchDeleteUserResult> batchDeleteUsers(
        HttpServletRequest request,
        @Parameter(description = "目标用户 ID 列表", required = true, example = "123,456") @RequestParam List<Integer> userIds
    ) {
        log.info("管理员开始批量删除用户账户 - 用户 ID 数量: {}", userIds != null ? userIds.size() : 0);

        // 参数校验
        if (userIds == null || userIds.isEmpty()) {
            log.warn("用户 ID 列表为空");
            return ResponsePojo.error(null, "用户 ID 列表不能为空");
        }

        try {
            // 从 Token 中获取当前管理员 ID
            String token = JWTUtils.extractTokenWithLog(request, "批量删除用户账户");
            if (token == null || token.isEmpty()) {
                log.error("Token 不存在");
                return ResponsePojo.error(null, "未授权访问");
            }

            Integer adminId = JWTUtils.getUserIdFromToken(token);
            if (adminId == null) {
                log.error("无法从 Token 中获取管理员 ID");
                return ResponsePojo.error(null, "Token 无效");
            }

            int totalCount = userIds.size();
            java.util.Set<Integer> successIds = new java.util.HashSet<>();

            // 循环调用 Service 层删除方法
            for (Integer userId : userIds) {
                Boolean res = userService.deleteUserAccountByAdmin(userId, adminId);
                if (res) {
                    successIds.add(userId);
                }
            }

            // 计算失败的 ID
            java.util.List<Integer> failedUserIds = new java.util.ArrayList<>();
            for (Integer id : userIds) {
                if (!successIds.contains(id)) {
                    failedUserIds.add(id);
                }
            }

            AdminBatchDeleteUserResult result = new AdminBatchDeleteUserResult(totalCount, successIds.size(), failedUserIds);
            log.info("批量删除用户账户完成 - 总数: {}, 成功: {}, 失败: {}", totalCount, successIds.size(), failedUserIds.size());

            return ResponsePojo.success(result, "批量删除用户账户处理完成");

        } catch (Exception e) {
            log.error("批量删除用户账户异常 - 错误: {}", e.getMessage(), e);
            return ResponsePojo.error(null, "系统错误: " + e.getMessage());
        }
    }

    /**
     * 创建新用户
     *
     * @param request         HTTP 请求对象（用于获取当前管理员信息）
     * @param username        用户名（必填，唯一）
     * @param password        密码（明文，将自动加密）
     * @param confirmPassword 确认密码（必须与 password 一致）
     * @param nickname        昵称（必填）
     * @param email           邮箱（必填，唯一）
     * @param role            角色代码（可选，默认为 11-普通用户）
     * @param status          状态代码（可选，默认为 10-正常）
     * @return 响应数据，表示用户是否创建成功
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
            - **成功**：返回 true 表示创建成功
            - **两次密码不一致**：返回 false 和错误提示
            - **失败**：返回 false 和错误提示

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
    @RequireRole(value = {77})
    @PostMapping("/create")
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

    /**
     * 管理员批量重置用户密码
     *
     * @param userIds 目标用户 ID 列表
     * @return 响应数据，包含批量重置密码的统计信息
     * @author blue_sky_ks
     */
    @Operation(
        summary = "批量重置用户密码",
        description = """
            # 批量重置用户密码（需要登录认证 + 角色权限[77]）

            ## 特性
            - **管理员专属接口**：仅系统管理员可调用
            - **批量处理**：支持一次性重置多个用户的密码
            - **自动生成随机密码**：使用安全随机算法生成 12 位临时密码
            - **强制所有设备下线**：移除用户所有 Token，确保账户安全
            - **邮件通知**：所有用户密码重置完成后，批量发送新密码到对应邮箱
            - **密码加密存储**：使用 SHA-256 加密后存储

            ## 参数说明：
            - **userIds**: **用户 ID 列表**，Integer 数组类型，必填
              * 单个用户：传入 [1]
              * 批量用户：传入 [1, 2, 3]

            ## 返回说明：
            - **重置成功**：返回包含总数、成功重置数、成功发送邮件数、失败 ID 列表的统计信息
            - **用户 ID 列表为空**：返回错误提示
            - **部分失败**：返回统计信息并提示实际成功的数量

            ## 业务逻辑：
            1. 校验用户 ID 列表参数有效性
            2. 遍历用户 ID 列表，逐个处理：
               a. 查询用户信息
               b. 生成 12 位随机密码
               c. 对密码进行 SHA-256 加密
               d. 更新数据库中的用户密码
               e. 强制移除用户所有 Token（使所有设备下线）
            3. 所有用户密码重置完成后，批量渲染邮件模板并发送
            4. 记录操作日志

            ## 注意事项：
            - 这是一个**管理员接口**，需要管理员身份认证
            - 调用后用户的**所有登录会话将被强制终止**
            - 新生成的密码为**临时密码**，建议用户登录后立即修改
            - 密码以**明文形式**通过邮件发送，请确保邮件传输安全
            - 原密码会被覆盖，**无法恢复**
            - 如果某个用户不存在或处理失败，会跳过该用户并继续处理下一个
            """
    )
    @RequireRole(value = {77})
    @PostMapping("/update/password")
    public ResponsePojo<AdminResetPasswordResult> batchUpdateUserPassword(
        @Parameter(description = "目标用户 ID 列表", required = true, example = "1,2,3") @RequestParam List<Integer> userIds
    ) {
        log.info("管理员开始批量重置用户密码 - 用户 ID 数量: {}", userIds != null ? userIds.size() : 0);

        // 参数校验
        if (userIds == null || userIds.isEmpty()) {
            log.warn("用户 ID 列表为空");
            return ResponsePojo.error(null, "用户 ID 列表不能为空");
        }

        int totalCount = userIds.size();
        List<Integer> failedUserIds = new ArrayList<>();
        try {
            // 1. 调用 Service 层批量重置密码（返回包含明文密码的结果列表）
            List<Map<String, Object>> resetResults = userService.batchResetUserPasswords(userIds);

            int successCount = resetResults.size();

            // 计算失败的用户 ID (总数 - 成功数，但这里需要更精确的逻辑)
            // 由于 Service 层目前只返回成功的，我们需要在 Controller 层对比或者修改 Service 层
            // 为了简单且符合“一次性重置多个用户”的逻辑，我们假设 Service 层返回的都是成功的。
            // 失败的通常是那些不存在的 ID。我们可以通过遍历原始 ID 和结果来找出失败的 ID。

            Set<Integer> successIds = new HashSet<>();
            for (java.util.Map<String, Object> result : resetResults) {
                successIds.add((Integer) result.get("user_id"));
            }

            for (Integer id : userIds) {
                if (!successIds.contains(id)) {
                    failedUserIds.add(id);
                }
            }

            if (successCount == 0 && totalCount > 0) {
                log.warn("没有用户成功重置密码");
                AdminResetPasswordResult result = new AdminResetPasswordResult(totalCount, 0, 0, failedUserIds);
                return ResponsePojo.error(result, "没有用户成功重置密码，请检查用户 ID 是否正确");
            }

            // 2. 批量发送邮件
            int emailSentCount = 0;
            for (java.util.Map<String, Object> result : resetResults) {
                String username = (String) result.get("username");
                String email = (String) result.get("email");
                String plainPassword = (String) result.get("plainPassword");

                // 渲染邮件 HTML
                String html = emailTemplateService.renderResetPasswordEmail(username, plainPassword);

                try {
                    emailService.sendEMail(email, "PixVision 重置用户密码", html);
                    emailSentCount++;
                    log.info("密码重置邮件发送成功 - 用户名: {}, 邮箱: {}", username, email);
                } catch (Exception e) {
                    log.error("密码重置邮件发送失败 - 用户名: {}, 邮箱: {}, 错误: {}", username, email, e.getMessage());
                }
            }

            log.info("批量重置密码完成 - 总数: {}, 成功重置: {} 人, 成功发送邮件: {} 人, 失败: {} 人", totalCount, successCount, emailSentCount, failedUserIds.size());
            AdminResetPasswordResult responseResult = new AdminResetPasswordResult(totalCount, successCount, emailSentCount, failedUserIds);
            return ResponsePojo.success(responseResult, "批量重置密码处理完成");

        } catch (Exception e) {
            log.error("批量重置密码异常 - 错误: {}", e.getMessage(), e);
            AdminResetPasswordResult result = new AdminResetPasswordResult(totalCount, 0, 0, userIds); // 异常时所有都视为失败
            return ResponsePojo.error(result, "系统错误: " + e.getMessage());
        }
    }

    /**
     * 分页查询用户信息
     *
     * @param page      页码（默认 1）
     * @param size      每页条目数（默认 10）
     * @param user_role 用户角色（可选）
     * @param status    用户状态（可选）
     * @param is_delete 是否已删除（可选）
     * @param nickname  昵称关键字（可选，模糊查询）
     * @param orderBy   排序方式（可选）
     * @return 响应数据，包含分页用户列表
     * @author PlayerEG
     */
    @Operation(
        summary = "分页查询用户信息",
        description = """
            # 分页查询用户信息（需要登录认证 + 角色权限[55, 77]）

            ## 特性
            - 支持分页查询用户完整信息
            - 支持按用户角色筛选（可选）
            - 支持按用户状态筛选（可选）
            - 支持按删除标记筛选（可选）
            - 支持按昵称关键字模糊查询（可选）
            - 支持排序方式（可选）
            - 审核员和系统管理员均可调用

            ## 参数说明：
            - **page**: 页码（可选，默认 1）
            - **size**: 每页条目数（可选，默认 10）
            - **user_role**: 用户角色（可选），可选值：
              - 11: 普通用户
              - 22: 创作者
              - 55: 审核员
              - 66: 工单管理员
              - 77: 系统管理员
            - **status**: 用户状态（可选），可选值：
              - 10: 正常
              - 20: 冻结
              - 30: 封禁
            - **is_delete**: 是否已删除（可选）
              - true: 仅查已删除
              - false: 仅查未删除
            - **nickname**: 昵称关键字（可选，模糊匹配）
            - **orderBy**: 排序方式（可选）
              - 'oldest': 按最早注册排列
              - 其他值或不传: 按最新注册排列（默认）

            ## 返回说明：
            - **成功**：返回分页用户列表（含总条数、总页数等分页信息）
            - **结果为空**：返回空分页对象和成功状态
            - **失败**：返回错误提示

            ## 业务逻辑：
            1. 构建分页对象（Page）
            2. 将筛选条件传入 Service 层
            3. 数据库执行动态 SQL 分页查询
            4. 返回分页结果

            ## 注意事项：
            - 返回的用户信息包含密码字段，请谨慎处理
            - 查询结果为空时返回空分页对象和成功状态
            - 所有筛选条件均为可选，不传则不过滤
            """
    )
    @PostMapping("/page-select")
    public ResponsePojo<IPage<User>> selectUserPage(
        @Parameter(description = "页码", example = "1", required = true ) @RequestParam( defaultValue = "1") Integer page,
        @Parameter(description = "每页条目数", example = "10", required = true) @RequestParam( defaultValue = "10") Integer size,
        @Schema(
            description = "用户角色（可选，11-普通用户, 22-创作者, 55-审核员, 66-工单管理员, 77-系统管理员）",
            allowableValues = {"11", "22", "55", "66", "77"},
            example = "11"
        ) @RequestParam(required = false) Integer user_role,
        @Schema(
            description = "用户状态（可选，10-正常, 20-冻结, 30-封禁）",
            allowableValues = {"10", "20", "30"},
            example = "10"
        ) @RequestParam(required = false) Integer status,
        @Parameter(description = "是否已删除（可选）", example = "false")
        @RequestParam(required = false) Boolean is_delete,
        @Parameter(description = "昵称关键字（可选，模糊查询）", example = "李华")
        @RequestParam(required = false) String nickname,
        @Schema(
            description = "排序方式：'oldest' - 最早注册，其他值或不传 - 最新注册（默认）",
            allowableValues = {"newest", "oldest"},
            example = "newest"
        ) @RequestParam(required = false) String orderBy
    ) {
        log.info("分页查询用户信息 - 页码: {}, 每页: {}, 角色: {}, 状态: {}, 是否删除: {}, 昵称: {}, 排序: {}",
            page, size, user_role, status, is_delete, nickname, orderBy);

        try {
            IPage<User> result = userService.getAdminUserPage(page, size, user_role, status, is_delete, nickname, orderBy);

            log.info("分页查询用户信息完成 - 总条数: {}, 当前页条数: {}", result.getTotal(), result.getRecords().size());
            return ResponsePojo.success(result, "查询成功，共 " + result.getTotal() + " 条记录");
        } catch (Exception e) {
            log.error("分页查询用户信息异常 - 错误: {}", e.getMessage(), e);
            return ResponsePojo.error(null, "系统错误: " + e.getMessage());
        }
    }
}
