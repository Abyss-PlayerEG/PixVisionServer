package top.playereg.pix_vision.config;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import top.playereg.pix_vision.enums.LogColor;
import top.playereg.pix_vision.handler.JwtAuthenticationInterceptor;
import top.playereg.pix_vision.handler.PermissionInterceptor;

import static cn.hutool.core.lang.Console.log;

/**
 * Web MVC 配置类 - 注册 JWT 拦截器和权限拦截器
 *
 * @author PlayerEG
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {
    private static final Logger log = LoggerFactory.getLogger(WebConfig.class);

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
    public void addCorsMappings(@NotNull CorsRegistry registry) {
        int length = allowedOrigin.length;
        log.info("已有 {} 个源地址已进行跨域处理", length);
        log("\n\t允许的源地址列表：");
        int i = 1;
        for (String origin : allowedOrigin)
            log("\t\tURL[{}]: {}", LogColor.colorize(String.valueOf(i++), LogColor.GREEN), origin);
        log();
        registry.addMapping("/**")
            // 允许的源地址（从配置文件读取）
            .allowedOriginPatterns(allowedOrigin)
            // 允许的 HTTP 方法（* 表示所有方法）
            .allowedMethods("*")
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
     * <p>
     * 1. JWT 认证拦截器 - 验证 Token 有效性（支持 @PublicAccess 注解）
     * <p>
     * 2. 权限验证拦截器 - 验证用户角色权限（基于 @RequireRole 注解）
     * </p>
     *
     * @param registry 拦截器注册表
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 第1层：JWT 认证拦截器（验证 Token）
        // 使用 @PublicAccess 注解标记公开接口，无需在此处配置白名单
        registry.addInterceptor(jwtAuthenticationInterceptor)
            .addPathPatterns("/api/**", "/7e212056/**");  // 拦截所有 API 请求

        // 第2层：权限验证拦截器（验证角色权限）
        // 对所有已认证的 API 请求生效，但只处理带有 @RequireRole 注解的接口
        registry.addInterceptor(permissionInterceptor)
            .addPathPatterns("/api/**", "/7e212056/**");
    }
}
