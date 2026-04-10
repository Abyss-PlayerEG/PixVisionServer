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
                "/7e212056/no-auth",                        // 测试接口
                "/api/test/**",                             // 测试接口
                "/api/user/register",                       // 用户注册
                "/api/user/login",                          // 用户登录
                "/api/mail/send-email-code",                // 发送邮箱验证码
                "/api/mail/verify-email-code-test",         // 验证邮箱验证码
                "/api/user/forgot-password",                // 忘记密码
                "/aip/get-image"                            // 图像获取
            );
    }
}
