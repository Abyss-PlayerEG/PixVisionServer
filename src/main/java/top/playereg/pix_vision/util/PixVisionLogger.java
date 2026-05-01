package top.playereg.pix_vision.util;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.playereg.pix_vision.enums.LogColor;

/**
 * PixVision 自定义日志接口
 * 提供更简洁、统一的日志记录方式
 *
 * @author PlayerEG
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
        message = LogColor.colorize(message,LogColor.BLUE);
        getLogger().debug(message);
    }

    /**
     * DEBUG 级别日志（带参数）
     *
     * @param format 格式化字符串
     * @param args   参数
     */
    default void debug(String format, Object... args) {
        format = LogColor.colorize(format,LogColor.BLUE);
        getLogger().debug(format, args);
    }

    /**
     * DEBUG 级别日志（带异常）
     *
     * @param message 日志消息
     * @param throwable 异常对象
     */
    default void debug(String message, Throwable throwable) {
        message = LogColor.colorize(message,LogColor.BLUE);
        getLogger().debug(message, throwable);
    }

    /**
     * INFO 级别日志
     *
     * @param message 日志消息
     */
    default void info(String message) {
        message = LogColor.colorize(message,LogColor.GREEN);
        getLogger().info(message);
    }

    /**
     * INFO 级别日志（带参数）
     *
     * @param format 格式化字符串
     * @param args   参数
     */
    default void info(String format, Object... args) {
        format = LogColor.colorize(format,LogColor.GREEN);
        getLogger().info(format, args);
    }

    /**
     * INFO 级别日志（带异常）
     *
     * @param message 日志消息
     * @param throwable 异常对象
     */
    default void info(String message, Throwable throwable) {
        message = LogColor.colorize(message,LogColor.GREEN);
        getLogger().info(message, throwable);
    }

    /**
     * WARN 级别日志
     *
     * @param message 日志消息
     */
    default void warn(String message) {
        message = LogColor.colorize(message,LogColor.YELLOW);
        getLogger().warn(message);
    }

    /**
     * WARN 级别日志（带参数）
     *
     * @param format 格式化字符串
     * @param args   参数
     */
    default void warn(String format, Object... args) {
        format = LogColor.colorize(format,LogColor.YELLOW);
        getLogger().warn(format, args);
    }

    /**
     * WARN 级别日志（带异常）
     *
     * @param message 日志消息
     * @param throwable 异常对象
     */
    default void warn(String message, Throwable throwable) {
        message = LogColor.colorize(message,LogColor.YELLOW);
        getLogger().warn(message, throwable);
    }

    /**
     * ERROR 级别日志
     *
     * @param message 日志消息
     */
    default void error(String message) {
        message = LogColor.colorize(message,LogColor.RED);
        getLogger().error(message);
    }

    /**
     * ERROR 级别日志（带参数）
     *
     * @param format 格式化字符串
     * @param args   参数
     */
    default void error(String format, Object... args) {
        format = LogColor.colorize(format,LogColor.RED);
        getLogger().error(format, args);
    }

    /**
     * ERROR 级别日志（带异常）
     *
     * @param message 日志消息
     * @param throwable 异常对象
     */
    default void error(String message, Throwable throwable) {
        message = LogColor.colorize(message,LogColor.RED);
        getLogger().error(message, throwable);
    }

    /**
     * TRACE 级别日志
     *
     * @param message 日志消息
     */
    default void trace(String message) {
        message = LogColor.colorize(message,LogColor.GRAY);
        getLogger().trace(message);
    }

    /**
     * TRACE 级别日志（带参数）
     *
     * @param format 格式化字符串
     * @param args   参数
     */
    default void trace(String format, Object... args) {
        format = LogColor.colorize(format,LogColor.GRAY);
        getLogger().trace(format, args);
    }

    /**
     * TRACE 级别日志（带异常）
     *
     * @param message 日志消息
     * @param throwable 异常对象
     */
    default void trace(String message, Throwable throwable) {
        message = LogColor.colorize(message,LogColor.GRAY);
        getLogger().trace(message, throwable);
    }
}
