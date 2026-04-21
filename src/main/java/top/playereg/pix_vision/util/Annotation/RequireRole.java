package top.playereg.pix_vision.util.Annotation;

import java.lang.annotation.*;

/**
 * 角色权限注解
 * <p>
 * 用于标记需要特定角色才能访问的接口或控制器类
 * </p>
 *
 * @author PlayerEG
 * @see top.playereg.pix_vision.handler.PermissionInterceptor
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
    boolean allowHigher() default true;
}
