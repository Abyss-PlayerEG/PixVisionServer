package top.playereg.pix_vision.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import top.playereg.pix_vision.handler.JwtAuthenticationInterceptor;

/**
 * Web MVC 配置类 - 注册 JWT 拦截器
 *
 * @author PlayerEG
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private JwtAuthenticationInterceptor jwtAuthenticationInterceptor;

    /**
     * 从配置文件读取允许的跨域源地址
     * 如果配置文件中没有定义，使用默认的开发环境地址
     */
    @Value("${cors.allowed-origin}")
    private String[] allowedOrigin;

    /**
     * 添加静态资源映射
     * 将用户目录下的图片文件夹映射为可访问的URL路径
     * @param registry 资源处理器注册表
     * @author PlayerEG
     */
//    @Override
//    public void addResourceHandlers(ResourceHandlerRegistry registry) {
//        // 映射头像目录: /avatar/** -> ~/.pix_vision/data/avatar/**
//        registry.addResourceHandler("/avatar/**")
//                .addResourceLocations("file:" + FilePathConfig.AvatarPath + "/");
//
//        // 映射作品图片目录(如果需要): /works/** -> ~/.pix_vision/data/works/**
//         registry.addResourceHandler("/works/**")
//                 .addResourceLocations("file:" + FilePathConfig.DataPath + "/works/");
//
//        // 映射Logo目录: /logo/** -> ~/.pix_vision/data/logo-img/**
//        registry.addResourceHandler("/logo/**")
//                .addResourceLocations("file:" + FilePathConfig.LogoPath + "/");
//    }

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
     *
     * @param registry 拦截器注册表
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtAuthenticationInterceptor)
            // 拦截所有请求
            .addPathPatterns(
                "/api/**",
                "/7e212056/require-auth"                // 测试接口
            )
            // 排除不需要验证的路径
            .excludePathPatterns(
                // 用户接口
                "/api/user/auth/login",                     // 登录
                "/api/user/auth/logout",                    // 登出
                "/api/user/register",                       // 注册
                "/api/user/password/forgot",                // 忘记密码
                "/api/user/data/list/**",                   // 查询用户拓展数据
                // 邮箱接口
                "/api/mail/send-reset-password-code",       // 发送密码修改验证码邮件
                "/api/mail/send-register-code",             // 发送注册验证码邮件
                "/api/mail/send-login-code",                // 发送登录验证码邮件
                // 其他接口
                "/api/image/get/**",                        // 获取图像
                "/api/test/**",                             // 测试接口
                "/7e212056/no-auth"                        // 测试接口
            );
    }
}
