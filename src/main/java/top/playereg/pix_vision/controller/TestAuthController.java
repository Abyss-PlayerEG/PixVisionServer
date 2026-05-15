package top.playereg.pix_vision.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.playereg.pix_vision.pojo.ResponsePojo;
import top.playereg.pix_vision.util.Annotation.PublicAccess;
import top.playereg.pix_vision.util.PixVisionLogger;

/**
 * 测试 JWT 鉴权的示例接口
 *
 * @author PlayerEG
 */
@RestController
@RequestMapping("/7e212056")
@Tag(name = "JWT 鉴权测试接口", description = "提供 JWT Token 生成和验证的测试功能")
public class TestAuthController {

    private static final PixVisionLogger log = PixVisionLogger.create(TestAuthController.class);

    /**
     * 测试需要登录的接口
     *
     * @return 响应数据
     */
    @GetMapping("/require-auth")
    @Operation(
            summary = "需要登录的测试接口",
            description = """
                # 测试接口（需要登录认证）

                ## 特性
                - Token 认证（支持 Header 和 URL 参数两种方式）
                - 拦截器自动验证
                - 用户信息提取（userId/username）

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
    @PublicAccess("测试接口 - 无需 JWT 认证")
    @Operation(
            summary = "不需要登录的测试接口",
            description = """
                # 测试接口（无需登录认证）

                ## 特性
                - 公开接口（无需认证）
                - 快速访问测试

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
}
