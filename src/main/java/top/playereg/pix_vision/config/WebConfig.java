package top.playereg.pix_vision.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
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
                        "/7e212056/no-auth",                    // 测试接口
                        "/api/test/**",
                        "/api/user/register",                   // 用户注册
                        "/api/user/login",                      // 用户登录
                        "/api/mail/send-email-code",           // 发送邮箱验证码
                        "/api/mail/verify-email-code-test"          // 验证邮箱验证码
                );
    }
}
