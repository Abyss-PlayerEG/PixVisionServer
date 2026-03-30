package top.playereg.pix_vision.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import top.playereg.pix_vision.pojo.ResponsePojo;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 测试 JWT 鉴权的示例接口
 *
 * @author PlayerEG
 */
@RestController
@RequestMapping("/7e212056")
@Tag(name = "JWT 鉴权测试接口")
public class TestAuthController {

    private static final Logger log = LoggerFactory.getLogger(TestAuthController.class);

    /**
     * 测试需要登录的接口
     *
     * @return 响应数据
     */
    @GetMapping("/require-auth")
    @Operation(
            summary = "需要登录的测试接口",
            description = """
                # 测试接口 - 需要 JWT 认证
                
                ## 接口说明：
                - 此接口**需要登录**才能访问
                - 用于测试 JWT 拦截器是否正常工作
                
                ## Token 使用方式：
                - Header: `Authorization: Bearer <token>`
                - 或 URL 参数：`?token=<token>`
                
                ## 返回说明：
                - **验证通过**：返回当前登录用户的信息
                - **未授权**：返回 401 错误和提示信息
                """
    )
    public ResponsePojo<String> requireAuth(
             HttpServletRequest request
    ) {
        // 从 request 中获取拦截器设置的用户信息
        String username = (String) request.getAttribute("username");
        Integer userId = (Integer) request.getAttribute("userId");

        log.info("访问受保护的接口，用户 ID: {}, 用户名：{}", userId, username);

        return ResponsePojo.success(
            String.format("欢迎访问！您已通过JWT认证 - 用户ID:%d 用户名:%s", userId, username),
            "认证成功"
        );
    }

    /**
     * 测试不需要登录的接口
     *
     * @return 响应数据
     */
    @GetMapping("/no-auth")
    @Operation(
            summary = "不需要登录的测试接口",
            description = """
                # 测试接口 - 无需 JWT 认证
                
                ## 接口说明：
                - 此接口**不需要登录**即可访问
                - 用于对比测试 JWT 拦截器的效果
                
                ## 返回说明：
                - 直接返回公开信息
                """
    )
    public ResponsePojo<String> noAuth() {
        log.info("访问公开接口");
        return ResponsePojo.success("这是一个公开接口，无需登录即可访问", "公开访问");
    }

    /**
     * 清除/注销 Token（模拟登出）
     *
     * @return 响应数据
     */
    @PostMapping("/logout")
    @Operation(
            summary = "用户登出接口（清除 Token）",
            description = """
                # 用户登出 - 清除 Token
                
                ## 接口说明：
                - 此接口**需要登录**才能访问
                - 用于模拟用户登出操作
                - JWT 是无状态的，服务端无法真正"清除"Token
                - 客户端需要主动删除本地存储的 Token
                
                ## Token 使用方式：
                - Header: `Authorization: Bearer <token>`
                - 或 URL 参数：`?token=<token>`
                
                ## 返回说明：
                - **成功**：返回登出提示，客户端需删除本地 Token
                - **未授权**：返回 401 错误和提示信息
                
                ## 注意事项：
                - JWT Token 在有效期内仍然有效
                - 如需完全禁用 Token，需实现 Token 黑名单机制（使用 Redis）
                - 建议客户端同时清除所有相关缓存
                """
    )
    public ResponsePojo<String> logout(HttpServletRequest request) {
        // 从 request 中获取拦截器设置的用户信息
        String username = (String) request.getAttribute("username");
        Integer userId = (Integer) request.getAttribute("userId");

        // 获取请求头中的 Token（用于日志记录）
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
            log.info("用户登出，用户名：{}, 用户 ID: {}, Token 前缀：{}", username, userId,
                     token != null && token.length() > 10 ? token.substring(0, 10) + "..." : "unknown");
        } else {
            log.info("用户登出，用户名：{}, 用户 ID: {}", username, userId);
        }

        // 注意：JWT 是无状态的，这里只是返回提示信息
        // 真正的 Token 清除需要客户端配合删除
        return ResponsePojo.success(
            String.format("登出成功！请在客户端删除 Token。\n用户：%s (ID: %d)", username, userId),
            "登出成功，请删除本地存储的 Token"
        );
    }
}
