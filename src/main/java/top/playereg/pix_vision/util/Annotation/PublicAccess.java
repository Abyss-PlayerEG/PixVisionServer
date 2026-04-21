package top.playereg.pix_vision.util.Annotation;

import java.lang.annotation.*;

/**
 * 公开访问注解
 * <p>
 * 标记该接口或控制器无需 JWT 认证即可访问
 * 用于替代 WebConfig 中的 excludePathPatterns 配置
 * </p>
 *
 * <p><b>使用示例：</b></p>
 * <pre>{@code
 * // 方法级别
 * @GetMapping("/login")
 * @PublicAccess
 * public ResponsePojo<User> login(...) { ... }
 *
 * // 类级别（整个控制器公开）
 * @RestController
 * @PublicAccess
 * public class PublicController { ... }
 * }</pre>
 *
 * @author PlayerEG
 * @see top.playereg.pix_vision.handler.JwtAuthenticationInterceptor
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface PublicAccess {
    
    /**
     * 描述信息（可选）
     * 用于说明为什么该接口需要公开访问
     *
     * @return 描述文本
     */
    String value() default "";
}
