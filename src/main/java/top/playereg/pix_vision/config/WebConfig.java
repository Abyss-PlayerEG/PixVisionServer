package top.playereg.pix_vision.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import top.playereg.pix_vision.handler.JwtAuthenticationInterceptor;
import top.playereg.pix_vision.handler.PermissionInterceptor;

/**
 * Web MVC 配置类 - 注册 JWT 拦截器和权限拦截器
 *
 * @author PlayerEG
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private JwtAuthenticationInterceptor jwtAuthenticationInterceptor;

    @Autowired
    private PermissionInterceptor permissionInterceptor;

    /**
     * 从配置文件读取允许的跨域源地址
     * 如果配置文件中没有定义，使用默认的开发环境地址
     */
    @Value("${cors.allowed-origin}")
    private String[] allowedOrigin;

    /**
     * 配置跨域资源共享（CORS）
     * 允许前端应用跨域访问后端 API
     *
     * @param registry CORS 注册表
     * @author PlayerEG
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
            // 允许的源地址（从配置文件读取）
            .allowedOriginPatterns(allowedOrigin)
            // 允许的 HTTP 方法
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
            // 允许的请求头
            .allowedHeaders("*")
            // 是否允许携带凭证（Cookie、Authorization 等）
            .allowCredentials(true)
            // 预检请求的有效期（秒）
            .maxAge(3600);
    }

    /**
     * 添加拦截器配置
     * <p>
     * 拦截器执行顺序：
     * 1. JWT 认证拦截器 - 验证 Token 有效性
     * 2. 权限验证拦截器 - 验证用户角色权限（基于 @RequireRole 注解）
     * </p>
     *
     * @param registry 拦截器注册表
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 第1层：JWT 认证拦截器（验证 Token）
        registry.addInterceptor(jwtAuthenticationInterceptor)
            // 拦截所有 API 请求
            .addPathPatterns(
                "/api/**",
                "/7e212056/require-auth"                // 测试接口
            )
            // 排除不需要认证的路径
            .excludePathPatterns(
                // 用户认证接口
                "/api/user/auth/login",                     // 登录
                "/api/user/auth/register",                  // 注册
                // 密码管理接口
                "/api/user/password/forgot",                // 忘记密码
                // 用户数据查询接口
                "/api/user/data/list/**",                   // 查询用户拓展数据
                // 邮件服务接口（发送验证码）
                "/api/mail/send-forget-password-code",      // 发送重置密码验证码
                "/api/mail/send-register-code",             // 发送注册验证码
                "/api/mail/send-login-code",                // 发送登录验证码
                // 图片获取接口
                "/api/image/get/**",                        // 获取图像
                // 测试接口
                "/api/test/**",                             // 测试接口
                "/7e212056/no-auth"                        // 测试接口
            );

        // 第2层：权限验证拦截器（验证角色权限）
        // 对所有已认证的 API 请求生效，但只处理带有 @RequireRole 注解的接口
        registry.addInterceptor(permissionInterceptor)
            .addPathPatterns("/api/**");
    }
}
