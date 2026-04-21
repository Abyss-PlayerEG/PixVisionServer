package top.playereg.pix_vision.util.Aspect;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 日志切面注解 - 用于标记需要记录用户操作的方法
 * <p>
 * 使用示例：
 * <pre>{@code
 * @LogRecord(module = "用户管理", event = "修改昵称")
 * public ResponsePojo<Boolean> updateNickname(...) {
 *     // 业务逻辑
 * }
 * </pre>
 *
 * @author blue_sky_ks, PlayerEG
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@SuppressWarnings("unused")
public @interface LogRecord {
    String module() default "";

    String event() default "";
}
