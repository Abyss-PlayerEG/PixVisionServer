package top.playereg.pix_vision.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.playereg.pix_vision.pojo.ResponsePojo;
import top.playereg.pix_vision.service.UserService;
import top.playereg.pix_vision.util.Annotation.RequireRole;

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
            - **成功**：返回 **{"data": {"clearedCount": 清除数量}}** 和“权限缓存刷新成功”提示
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
}
