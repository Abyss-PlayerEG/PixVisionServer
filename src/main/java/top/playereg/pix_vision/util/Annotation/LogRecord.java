package top.playereg.pix_vision.util.Annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 操作日志记录注解
 * <p>
 * 用于标记需要记录用户操作的方法，配合 AOP 切面自动记录操作日志到数据库。
 * 该注解仅支持方法级别使用，可以记录模块名称、事件类型等关键信息。
 * </p>
 *
 * <h3>使用场景</h3>
 * <ol>
 *   <li>用户管理操作（注册、登录、修改资料、修改密码）</li>
 *   <li>内容管理操作（发布作品、删除作品、审核内容）</li>
 *   <li>系统配置操作（修改配置、重置密钥、清理缓存）</li>
 *   <li>敏感数据操作（导出数据、批量删除、权限变更）</li>
 * </ol>
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * // 示例1：基本用法 - 记录简单操作
 * @PostMapping("/change/nickname")
 * @LogRecord(module = "用户管理", event = "修改昵称")
 * public ResponsePojo<Boolean> updateNickname(@RequestParam String nickname) {
 *     // 业务逻辑
 * }
 *
 * // 示例2：重要操作 - 记录详细信息
 * @PostMapping("/change/password")
 * @LogRecord(module = "账户安全", event = "修改密码")
 * public ResponsePojo<Void> changePassword(@RequestBody PasswordDTO password) {
 *     // 修改密码后会使所有 Token 失效
 * }
 *
 * // 示例3：批量操作 - 记录批量行为
 * @PostMapping("/delete")
 * @LogRecord(module = "作品管理", event = "批量删除作品")
 * public ResponsePojo<Void> deleteWorks(@RequestBody List<Long> workIds) {
 *     // 批量删除作品
 * }
 *
 * // 示例4：敏感操作 - 记录管理员行为
 * @PostMapping("/admin/ban-user")
 * @LogRecord(module = "用户管理", event = "封禁用户")
 * public ResponsePojo<Void> banUser(@RequestParam Long userId, @RequestParam String reason) {
 *     // 封禁指定用户
 * }
 * }</pre>
 *
 * <h3>注意事项</h3>
 * <ul>
 *   <li>该注解仅支持方法级别，不支持类级别</li>
 *   <li>module 和 event 参数建议填写，便于日志分类和检索</li>
 *   <li>日志记录在方法执行成功后进行，异常不会记录</li>
 *   <li>会自动记录操作用户 ID、IP 地址、操作时间等信息</li>
 *   <li>敏感信息（如密码）会在日志中自动脱敏</li>
 * </ul>
 *
 * <h3>最佳实践</h3>
 * <ul>
 *   <li>module 使用统一的模块名称，便于统计分析</li>
 *   <li>event 描述要简洁明确，说明具体操作</li>
 *   <li>对敏感操作必须添加日志记录</li>
 *   <li>避免在高频调用的方法上使用，防止日志量过大</li>
 *   <li>定期清理历史日志，保持数据库性能</li>
 * </ul>
 *
 * @author blue_sky_ks, PlayerEG
 * @see top.playereg.pix_vision.util.Annotation.LogRecord 日志记录切面实现
 * @see top.playereg.pix_vision.pojo.OperateLog 操作日志实体类
 * @since DEV-2.0.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@SuppressWarnings("unused")
public @interface LogRecord {
    String module() default "";

    String event() default "";
}
