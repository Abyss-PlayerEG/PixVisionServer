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
import top.playereg.pix_vision.service.UserService;
import top.playereg.pix_vision.util.Annotation.RequireRole;
import top.playereg.pix_vision.util.JWTUtils;

import java.util.HashMap;
import java.util.Map;

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
    private static final Logger log = LoggerFactory.getLogger(AdminController.class);

    private final UserService userService;

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
            - **成功**：返回 **{"data": {"clearedCount": 清除数量}}** 和"权限缓存刷新成功"提示
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
     * 修改用户角色
     * <p>
     * 系统管理员可以修改指定用户的角色，支持的角色代码：
     * 11-普通用户, 22-创作者, 55-审核员, 66-工单管理员, 77-系统管理员
     * </p>
     *
     * @param request HTTP 请求对象（用于获取当前管理员信息）
     * @param targetUserId 目标用户 ID
     * @param newRole 新角色代码
     * @return 操作结果
     * @author PlayerEG
     */
    @Operation(
        summary = "修改用户角色",
        description = """
            # 修改用户角色（需要登录认证 + 角色权限[77]）

            ## 特性
            - 仅系统管理员可调用
            - 可以修改任意用户的角色（除自己外）
            - 自动清除目标用户的角色缓存
            - 记录操作日志（通过 update_user 字段）

            ## 参数说明：
            - **targetUserId**: 目标用户 ID（必填）
            - **newRole**: 新角色代码（必填），可选值：
              - 11: 普通用户
              - 22: 创作者
              - 55: 审核员
              - 66: 工单管理员
              - 77: 系统管理员

            ## 返回说明：
            - **成功**：返回 **{"data": true}** 和"用户角色修改成功"提示
            - **失败**：返回 **{"data": false}** 和错误提示

            ## 业务逻辑：
            1. 验证当前用户是否为系统管理员（由拦截器自动验证）
            2. 从 Token 中获取当前管理员 ID
            3. 验证目标用户是否存在
            4. 验证新角色代码是否合法
            5. 更新用户角色到数据库
            6. 清除目标用户的角色缓存
            7. 返回操作结果

            ## 安全限制：
            - 管理员不能修改自己的角色
            - 目标用户必须存在且未被删除
            - 角色代码必须在允许范围内

            ## 注意事项：
            - 修改角色后，目标用户下次请求时会自动获取新的角色信息
            - 如果需要立即生效，可以调用 /refresh-permission-cache 接口
            - 建议谨慎分配高权限角色（如 77-系统管理员）
            """
    )
    @PostMapping("/update-user-role")
    public ResponsePojo<Boolean> updateUserRole(
            HttpServletRequest request,
            @Parameter(description = "目标用户 ID", required = true, example = "123")
            @RequestParam Integer targetUserId,
            @Parameter(description = "新角色代码（11-普通用户, 22-创作者, 55-审核员, 66-工单管理员, 77-系统管理员）", required = true, example = "22")
            @RequestParam Integer newRole
    ) {
        log.info("管理员开始修改用户角色 - 目标用户 ID: {}, 新角色: {}", targetUserId, newRole);

        try {
            // 从 Token 中获取当前管理员 ID
            String token = JWTUtils.extractTokenWithLog(request, "修改用户角色");
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

            // 调用 Service 层更新用户角色
            Boolean success = userService.updateUserRole(targetUserId, newRole, adminId);

            if (success) {
                log.info("用户角色修改成功 - 目标用户 ID: {}, 新角色: {}", targetUserId, newRole);

                // 构建返回数据
                Map<String, Object> resultData = new HashMap<>();
                resultData.put("targetUserId", targetUserId);
                resultData.put("newRole", newRole);

                String roleName = getRoleName(newRole);
                return ResponsePojo.success(true, "用户角色修改成功，已更新为：" + roleName);
            } else {
                log.warn("用户角色修改失败 - 目标用户 ID: {}", targetUserId);
                return ResponsePojo.error(false, "用户角色修改失败，请检查用户是否存在或角色代码是否正确");
            }
        } catch (Exception e) {
            log.error("修改用户角色异常 - 错误: {}", e.getMessage(), e);
            return ResponsePojo.error(false, "系统错误: " + e.getMessage());
        }
    }

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
}
