package top.playereg.pix_vision.util.Annotation;

import java.lang.annotation.*;

/**
 * 公开访问控制注解
 * <p>
 * 用于标记无需 JWT 认证即可访问的控制器类或方法。该注解支持在类级别和方法级别使用，
 * 可以灵活控制接口的公开范围，替代传统的配置文件排除路径方式。
 * </p>
 *
 * <h3>使用场景</h3>
 * <ol>
 *   <li>用户认证接口（登录、注册、忘记密码）</li>
 *   <li>公共服务接口（健康检查、系统信息）</li>
 *   <li>公开资源接口（静态资源、验证码发送）</li>
 *   <li>第三方回调接口（支付回调、Webhook）</li>
 * </ol>
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * // 示例1：方法级别 - 单个接口公开
 * @PostMapping("/login")
 * @PublicAccess("用户登录接口，无需认证")
 * public ResponsePojo<UserLogin> login(@RequestBody LoginDTO login) {
 *     // 无需 Token 即可访问
 * }
 *
 * // 示例2：方法级别 - 多个公开接口
 * @GetMapping("/health")
 * @PublicAccess("健康检查接口")
 * public ResponsePojo<String> health() {
 *     return ResponsePojo.success("OK");
 * }
 *
 * // 示例3：类级别 - 整个控制器公开
 * @RestController
 * @RequestMapping("/api/public")
 * @PublicAccess("公共服务控制器")
 * public class PublicController {
 *
 *     @GetMapping("/system-info")
 *     public ResponsePojo<SystemInfo> getSystemInfo() {
 *         // 所有接口都无需认证
 *     }
 *
 *     @PostMapping("/send-code")
 *     public ResponsePojo<Void> sendVerificationCode(@RequestParam String email) {
 *         // 发送验证码接口
 *     }
 * }
 *
 * // 示例4：混合使用 - 部分接口公开，部分需要认证
 * @RestController
 * @RequestMapping("/api/user")
 * public class UserController {
 *
 *     @PostMapping("/register")
 *     @PublicAccess("用户注册接口")
 *     public ResponsePojo<Void> register(@RequestBody RegisterDTO register) {
 *         // 公开接口，无需认证
 *     }
 *
 *     @GetMapping("/profile")
 *     public ResponsePojo<User> getProfile() {
 *         // 需要 JWT 认证（继承默认行为）
 *     }
 * }
 * }</pre>
 *
 * <h3>注意事项</h3>
 * <ul>
 *   <li>方法级别的注解优先级高于类级别</li>
 *   <li>公开接口仍会经过其他拦截器（如权限拦截器），但跳过 JWT 认证</li>
 *   <li>建议在 value 参数中说明公开原因，便于维护和审计</li>
 *   <li>不要将敏感操作接口标记为公开访问</li>
 *   <li>与 {@code @RequireRole} 互斥，不应同时使用</li>
 * </ul>
 *
 * <h3>安全建议</h3>
 * <ul>
 *   <li>仅在必要时使用，最小化公开接口范围</li>
 *   <li>公开接口应实现其他安全措施（如验证码、限流、IP 白名单）</li>
 *   <li>定期审查公开接口列表，确保安全性</li>
 *   <li>记录公开接口的访问日志，便于监控和审计</li>
 * </ul>
 *
 * @author PlayerEG
 * @see top.playereg.pix_vision.handler.JwtAuthenticationInterceptor JWT 认证拦截器
 * @see top.playereg.pix_vision.util.Annotation.RequireRole 角色权限控制注解
 * @since DEV-2.0.0
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
