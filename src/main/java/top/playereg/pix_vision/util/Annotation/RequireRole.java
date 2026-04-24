package top.playereg.pix_vision.util.Annotation;

import java.lang.annotation.*;

/**
 * 角色权限控制注解
 * <p>
 * 用于标记需要特定角色权限才能访问的控制器类或方法。该注解支持在类级别和方法级别使用，
 * 可以精确控制不同角色的访问权限，并支持权限继承机制。
 * </p>
 *
 * <h3>角色等级说明</h3>
 * <ul>
 *   <li><b>11</b> - 普通用户（基础权限）</li>
 *   <li><b>22</b> - 创作者（内容创作权限）</li>
 *   <li><b>55</b> - 审核员（内容审核权限）</li>
 *   <li><b>66</b> - 工单管理员（工单管理权限）</li>
 *   <li><b>77</b> - 系统管理员（最高权限）</li>
 * </ul>
 *
 * <h3>使用场景</h3>
 * <ol>
 *   <li>限制特定接口仅允许某些角色访问</li>
 *   <li>实现基于角色的访问控制（RBAC）</li>
 *   <li>支持权限继承（高权限角色可访问低权限接口）</li>
 * </ol>
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * // 示例1：方法级别 - 仅允许管理员访问
 * @GetMapping("/admin/dashboard")
 * @RequireRole(value = {77}, allowHigher = false)
 * public ResponsePojo<DashboardVO> getDashboard() {
 *     // 仅系统管理员可访问
 * }
 *
 * // 示例2：方法级别 - 允许管理员和工单管理员，支持更高权限
 * @PostMapping("/ticket/review")
 * @RequireRole(value = {66, 77}, allowHigher = true)
 * public ResponsePojo<Void> reviewTicket(@RequestBody TicketDTO ticket) {
 *     // 工单管理员及以上角色可访问
 * }
 *
 * // 示例3：类级别 - 整个控制器需要创作者权限
 * @RestController
 * @RequestMapping("/api/creator")
 * @RequireRole(value = {22}, allowHigher = true)
 * public class CreatorController {
 *     // 所有接口都需要创作者及以上权限
 * }
 *
 * // 示例4：组合使用 - 类级别基础权限 + 方法级别特殊权限
 * @RestController
 * @RequireRole(value = {22}, allowHigher = true)
 * public class ContentController {
 *
 *     @GetMapping("/list")
 *     public ResponsePojo<List<Content>> list() {
 *         // 继承类级别的创作者权限
 *     }
 *
 *     @DeleteMapping("/{id}")
 *     @RequireRole(value = {77}, allowHigher = false)
 *     public ResponsePojo<Void> delete(@PathVariable Long id) {
 *         // 覆盖为仅系统管理员可删除
 *     }
 * }
 * }</pre>
 *
 * <h3>注意事项</h3>
 * <ul>
 *   <li>方法级别的注解优先级高于类级别</li>
 *   <li>当 {@code allowHigher = true} 时，更高权限的角色也可以访问</li>
 *   <li>权限拦截器会自动验证用户角色，未授权将返回 403 错误</li>
 *   <li>建议配合 JWT 认证使用，确保用户身份已验证</li>
 * </ul>
 *
 * @author PlayerEG
 * @see top.playereg.pix_vision.handler.PermissionInterceptor 权限拦截器实现
 * @since DEV-2.0.0
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireRole {

    /**
     * 允许的角色代码列表
     * <ul>
     *   <li>11 - 普通用户</li>
     *   <li>22 - 创作者</li>
     *   <li>55 - 审核员</li>
     *   <li>66 - 工单管理员</li>
     *   <li>77 - 系统管理员</li>
     * </ul>
     *
     * @return 允许的角色代码数组
     */
    int[] value() default {};

    /**
     * 是否允许更高权限的角色访问
     * <p>
     * true: 允许更高权限角色访问（如普通用户可以访问创作者接口）<br>
     * false: 仅允许指定角色，不允许更高级别
     * </p>
     *
     * @return 是否允许更高权限
     */
    boolean allowHigher() default false;
}
