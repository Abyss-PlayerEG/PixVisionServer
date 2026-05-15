package top.playereg.pix_vision.util;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.playereg.pix_vision.enums.LogColor;

/**
 * PixVision 自定义日志接口
 * <p>
 * 基于 SLF4J 封装的增强型日志工具，支持自动颜色输出。
 * 不同日志级别对应不同颜色，提升控制台日志的可读性。
 * </p>
 *
 * <h3>颜色映射</h3>
 * <ul>
 *   <li>DEBUG - 蓝色</li>
 *   <li>INFO - 绿色</li>
 *   <li>WARN - 黄色</li>
 *   <li>ERROR - 红色</li>
 *   <li>TRACE - 灰色</li>
 * </ul>
 *
 * <h3>使用场景</h3>
 * <ol>
 *   <li>非 Spring Bean 类（如工具类）的日志记录</li>
 *   <li>需要彩色输出的调试信息</li>
 *   <li>统一项目日志风格</li>
 * </ol>
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * // 示例1：创建日志实例
 * private static final PixVisionLogger log = PixVisionLogger.create(ClassName.class);
 *
 * // 示例2：记录 INFO 日志（绿色）
 * log.info("用户登录成功: {}", username);
 *
 * // 示例3：记录 ERROR 日志（红色）
 * log.error("数据库连接失败", exception);
 *
 * // 示例4：记录 DEBUG 日志（蓝色）
 * log.debug("处理参数: {}, {}", param1, param2);
 * }</pre>
 *
 * <h3>注意事项</h3>
 * <ul>
 *   <li>Spring Bean 类推荐使用 @Slf4j 注解</li>
 *   <li>颜色输出仅在支持 ANSI 的控制台中生效</li>
 *   <li>日志文件会自动过滤 ANSI 转义码</li>
 *   <li>所有方法均为默认实现，无需额外配置</li>
 * </ul>
 *
 * @author PlayerEG
 * @see top.playereg.pix_vision.enums.LogColor 日志颜色枚举
 * @since DEV-2.0.0
 */
public interface PixVisionLogger {

    /**
     * 创建 PixVisionLogger 实例的工厂方法
     *
     * @param clazz 类对象
     * @return PixVisionLogger 实例
     */
    @Contract(value = "_ -> new", pure = true)
    static @NotNull PixVisionLogger create(Class<?> clazz) {
        return new PixVisionLogger() {
            private final Logger logger = LoggerFactory.getLogger(clazz);

            @Override
            public Logger getLogger() {
                return logger;
            }
        };
    }

    /**
     * 获取 SLF4J Logger 实例
     *
     * @return Logger 实例
     */
    Logger getLogger();

    /**
     * DEBUG 级别日志
     *
     * @param message 日志消息
     */
    default void debug(String message) {
        message = LogColor.colorize(message, LogColor.BLUE);
        getLogger().debug(message);
    }

    /**
     * DEBUG 级别日志（带参数）
     *
     * @param format 格式化字符串
     * @param args   参数
     */
    default void debug(String format, Object... args) {
        format = LogColor.colorize(format, LogColor.BLUE);
        getLogger().debug(format, args);
    }

    /**
     * DEBUG 级别日志（带异常）
     *
     * @param message   日志消息
     * @param throwable 异常对象
     */
    default void debug(String message, Throwable throwable) {
        message = LogColor.colorize(message, LogColor.BLUE);
        getLogger().debug(message, throwable);
    }

    /**
     * INFO 级别日志
     *
     * @param message 日志消息
     */
    default void info(String message) {
        message = LogColor.colorize(message, LogColor.GREEN);
        getLogger().info(message);
    }

    /**
     * INFO 级别日志（带参数）
     *
     * @param format 格式化字符串
     * @param args   参数
     */
    default void info(String format, Object... args) {
        format = LogColor.colorize(format, LogColor.GREEN);
        getLogger().info(format, args);
    }

    /**
     * INFO 级别日志（带异常）
     *
     * @param message   日志消息
     * @param throwable 异常对象
     */
    default void info(String message, Throwable throwable) {
        message = LogColor.colorize(message, LogColor.GREEN);
        getLogger().info(message, throwable);
    }

    /**
     * WARN 级别日志
     *
     * @param message 日志消息
     */
    default void warn(String message) {
        message = LogColor.colorize(message, LogColor.YELLOW);
        getLogger().warn(message);
    }

    /**
     * WARN 级别日志（带参数）
     *
     * @param format 格式化字符串
     * @param args   参数
     */
    default void warn(String format, Object... args) {
        format = LogColor.colorize(format, LogColor.YELLOW);
        getLogger().warn(format, args);
    }

    /**
     * WARN 级别日志（带异常）
     *
     * @param message   日志消息
     * @param throwable 异常对象
     */
    default void warn(String message, Throwable throwable) {
        message = LogColor.colorize(message, LogColor.YELLOW);
        getLogger().warn(message, throwable);
    }

    /**
     * ERROR 级别日志
     *
     * @param message 日志消息
     */
    default void error(String message) {
        message = LogColor.colorize(message, LogColor.RED);
        getLogger().error(message);
    }

    /**
     * ERROR 级别日志（带参数）
     *
     * @param format 格式化字符串
     * @param args   参数
     */
    default void error(String format, Object... args) {
        format = LogColor.colorize(format, LogColor.RED);
        getLogger().error(format, args);
    }

    /**
     * ERROR 级别日志（带异常）
     *
     * @param message   日志消息
     * @param throwable 异常对象
     */
    default void error(String message, Throwable throwable) {
        message = LogColor.colorize(message, LogColor.RED);
        getLogger().error(message, throwable);
    }

    /**
     * TRACE 级别日志
     *
     * @param message 日志消息
     */
    default void trace(String message) {
        message = LogColor.colorize(message, LogColor.GRAY);
        getLogger().trace(message);
    }

    /**
     * TRACE 级别日志（带参数）
     *
     * @param format 格式化字符串
     * @param args   参数
     */
    default void trace(String format, Object... args) {
        format = LogColor.colorize(format, LogColor.GRAY);
        getLogger().trace(format, args);
    }

    /**
     * TRACE 级别日志（带异常）
     *
     * @param message   日志消息
     * @param throwable 异常对象
     */
    default void trace(String message, Throwable throwable) {
        message = LogColor.colorize(message, LogColor.GRAY);
        getLogger().trace(message, throwable);
    }
}
