package top.playereg.pix_vision.util;

import cn.hutool.core.date.DateUtil;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Date;

/**
 * 控制台输出重定向工具类
 * <p>
 * 将 System.out 和 System.err 的所有输出重定向到日志文件
 * 确保捕获所有控制台内容，包括直接使用 System.out.println() 的输出
 * </p>
 *
 * <h3>配置项说明</h3>
 * <ul>
 *   <li>console-output.enabled: 是否启用重定向（默认 true）</li>
 *   <li>console-output.log-dir: 日志目录路径</li>
 *   <li>console-output.file-pattern: 文件名模板，支持 {timestamp} 占位符</li>
 *   <li>console-output.timestamp-format: 时间戳格式</li>
 * </ul>
 *
 * @author PlayerEG
 */
@SuppressWarnings("all")
@Component
public class ConsoleOutputRedirector {
    private static final PixVisionLogger log = PixVisionLogger.create(ConsoleOutputRedirector.class);

    // 配置项（从 application.yml 读取）
    @Value("${console-output.enabled:true}")
    private boolean enabled;

    @Value("${console-output.log-dir:${user.home}/.pix_vision/log}")
    private String logDir;

    @Value("${console-output.file-pattern:pix_vision[{timestamp}].log}")
    private String filePattern;

    @Value("${console-output.timestamp-format:yyyy-MM-dd-HH-mm-ss}")
    private String timestampFormat;

    private static PrintStream originalOut;
    private static PrintStream originalErr;
    private static FilePrintStream filePrintStream;

    /**
     * Spring 初始化时自动启动重定向
     *
     * @author PlayerEG
     */
    @PostConstruct
    public void autoInit() {
        if (enabled) {
            initConsoleOutputRedirector();
        } else {
            log.info("控制台输出重定向已禁用（通过配置 console-output.enabled=false）");
        }
    }

    /**
     * 初始化控制台输出重定向
     *
     * @param logFilePath 日志文件路径
     * @author PlayerEG
     */
    public static void init(String logFilePath) {
        try {
            // 保存原始输出流
            originalOut = System.out;
            originalErr = System.err;

            // 创建文件输出流
            Path logPath = Paths.get(logFilePath);

            // 确保父目录存在
            if (logPath.getParent() != null) {
                Files.createDirectories(logPath.getParent());
            }

            // 创建文件打印流（追加模式）
            filePrintStream = new FilePrintStream(logFilePath, originalOut);

            // 重定向 System.out 和 System.err
            System.setOut(filePrintStream);
            System.setErr(filePrintStream);
        } catch (Exception e) {
            log.error("初始化控制台输出重定向失败", e);
        }
    }

    /**
     * 恢复原始控制台输出
     *
     * @author PlayerEG
     */
    public static void restore() {
        if (originalOut != null) {
            System.setOut(originalOut);
        }
        if (originalErr != null) {
            System.setErr(originalErr);
        }
        if (filePrintStream != null) {
            filePrintStream.close();
        }
        log.info("控制台输出重定向已恢复");
    }

    /**
     * 初始化控制台输出重定向（使用配置文件中的参数）
     * 捕获所有 System.out 和 System.err 输出到日志文件
     */
    public void initConsoleOutputRedirector() {
        try {
            // 生成时间戳
            String timestamp = DateUtil.format(new Date(), timestampFormat);

            // 替换文件名模板中的占位符
            String fileName = filePattern.replace("{timestamp}", timestamp);

            // 构建完整路径（跨平台兼容，统一使用 Paths.get 处理路径分隔符）
            Path logPath = Paths.get(logDir, fileName);
            String logFilePath = logPath.toString();

            // 调用静态初始化方法
            init(logFilePath);
            log.info("控制台输出重定向已启用: {}", logFilePath);
        } catch (Exception e) {
            log.error("控制台输出重定向初始化失败", e);
        }
    }

    /**
     * 文件打印流 - 同时输出到文件和控制台
     */
    private static class FilePrintStream extends PrintStream {
        private final PrintStream console;
        private final ThreadLocal<Boolean> writing = ThreadLocal.withInitial(() -> false); // 线程安全，防止递归调用

        public FilePrintStream(String logFilePath, PrintStream console) throws Exception {
            super(
                new BufferedOutputStream(
                    Files.newOutputStream(
                        Paths.get(logFilePath),
                        StandardOpenOption.CREATE,
                        StandardOpenOption.APPEND,
                        StandardOpenOption.WRITE
                    ),
                    8192  // 8KB 缓冲区，提升写入性能
                ),
                true,  // autoFlush = true
                "UTF-8"
            );
            this.console = console;
        }

        @Override
        public void write(int b) {
            if (Boolean.TRUE.equals(writing.get())) return;
            try {
                writing.set(true);
                super.write(b);
                console.write(b);
            } catch (Exception e) {
                handleWriteError(e, "写入单个字节失败");
            } finally {
                writing.set(false);
            }
        }

        @Override
        public void write(byte[] buf, int off, int len) {
            if (Boolean.TRUE.equals(writing.get())) return;
            try {
                writing.set(true);

                // 写入文件（过滤 ANSI 转义码）
                String text = new String(buf, off, len, "UTF-8");
                String cleanText = removeAnsiCodes(text);
                byte[] cleanBytes = cleanText.getBytes("UTF-8");
                super.write(cleanBytes, 0, cleanBytes.length);

                // 输出到控制台（保留颜色代码）
                console.write(buf, off, len);
            } catch (Exception e) {
                handleWriteError(e, "写入字节数组失败");
            } finally {
                writing.set(false);
            }
        }

        @Override
        public void flush() {
            if (!Boolean.TRUE.equals(writing.get())) {
                super.flush();
                console.flush();
            }
        }

        /**
         * 处理写入错误（降级策略）
         * 当文件写入失败时，仅在控制台输出错误信息，避免递归调用
         *
         * @param e       异常对象
         * @param message 错误描述
         */
        private void handleWriteError(Exception e, String message) {
            // 降级策略：文件写入失败时仅输出到控制台
            try {
                console.println("[日志系统错误] " + message + ": " + e.getMessage());
            } catch (Exception ex) {
                // 极端情况：控制台也失败，静默忽略避免无限递归
            }
        }

        /**
         * 移除 ANSI 转义码（颜色控制码）
         * 支持多种 ANSI 控制序列格式：
         * - CSI 序列: ESC [ ... m (如 \u001B[2m, \u001B[39m, \u001B[0m)
         * - 其他 CSI 序列: ESC [ ... [@-~]
         * - OSC 序列: ESC ] ... BEL
         * - APC/PM 序列: ESC [_^] ... BEL
         *
         * @param text 包含 ANSI 码的文本
         * @return 清理后的纯文本
         * @see <a href="https://en.wikipedia.org/wiki/ANSI_escape_code">ANSI Escape Code</a>
         */
        private String removeAnsiCodes(String text) {
            if (text == null) {
                return null;
            }
            // 第一层：移除所有 CSI 序列 (Control Sequence Introducer)
            // 匹配: ESC [ 0x1B [ 后跟任意参数和最终字节 [@-~]
            String result = text.replaceAll("\u001B\\[[0-?]*[ -/]*[@-~]", "");
            // 第二层：移除 OSC 序列 (Operating System Command)
            // 匹配: ESC ] ... BEL (0x07) 或 ESC ] ... ST (ESC \)
            result = result.replaceAll("\u001B\\][^\u0007]*(?:\u0007|\u001B\\\\)", "");
            // 第三层：移除 APC/PM 序列 (Application Program Command / Privacy Message)
            // 匹配: ESC [_^] ... BEL
            result = result.replaceAll("\u001B[_^][^\u0007]*\u0007", "");
            return result;
        }

    }
}
