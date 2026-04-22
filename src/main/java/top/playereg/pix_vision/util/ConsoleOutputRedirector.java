package top.playereg.pix_vision.util;

import cn.hutool.core.date.DateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Date;

import static top.playereg.pix_vision.config.FilePathConfig.LogPath;

/**
 * 控制台输出重定向工具类
 * <p>
 * 将 System.out 和 System.err 的所有输出重定向到日志文件
 * 确保捕获所有控制台内容，包括直接使用 System.out.println() 的输出
 * </p>
 *
 * @author PlayerEG
 */
public class ConsoleOutputRedirector {
    private static final Logger log = LoggerFactory.getLogger(ConsoleOutputRedirector.class);

    private static PrintStream originalOut;
    private static PrintStream originalErr;
    private static FilePrintStream filePrintStream;

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

            log.info("控制台输出重定向已启用，日志文件: {}", logFilePath);
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
     * 初始化控制台输出重定向
     * 捕获所有 System.out 和 System.err 输出到日志文件
     */
    public static void initConsoleOutputRedirector() {
        try {
            // 使用 FilePathConfig 的路径方法确保跨平台兼容
            String logFilePath = LogPath + File.separator + "pix_vision" + DateUtil.format(new Date(), "[yyyy-MM-dd-HH-mm-ss]") + ".log";
            ConsoleOutputRedirector.init(logFilePath);
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
        private final String logFilePath;
        private boolean writing = false; // 防止递归调用

        public FilePrintStream(String logFilePath, PrintStream console) throws Exception {
            super(
                Files.newOutputStream(
                    Paths.get(logFilePath),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND,
                    StandardOpenOption.WRITE
                ),
                true,
                "UTF-8"
            );
            this.console = console;
            this.logFilePath = logFilePath;
        }

        @Override
        public void write(int b) {
            if (writing) return;
            try {
                writing = true;
                super.write(b);
                console.write(b);
            } catch (Exception e) {
                // 忽略异常
            } finally {
                writing = false;
            }
        }

        @Override
        public void write(byte[] buf, int off, int len) {
            if (writing) return;
            try {
                writing = true;

                // 写入文件（过滤 ANSI 转义码）
                String text = new String(buf, off, len, "UTF-8");
                String cleanText = removeAnsiCodes(text);
                byte[] cleanBytes = cleanText.getBytes("UTF-8");
                super.write(cleanBytes, 0, cleanBytes.length);

                // 输出到控制台（保留颜色代码）
                console.write(buf, off, len);
            } catch (Exception e) {
                // 忽略异常
            } finally {
                writing = false;
            }
        }

        @Override
        public void flush() {
            if (!writing) {
                super.flush();
                console.flush();
            }
        }

        /**
         * 移除 ANSI 转义码（颜色控制码）
         * 例如: \u001B[2m, \u001B[39m, \u001B[0m 等
         *
         * @param text 包含 ANSI 码的文本
         * @return 清理后的纯文本
         */
        private String removeAnsiCodes(String text) {
            if (text == null) {
                return null;
            }
            // ANSI 转义码格式: ESC [ ... m
            // ESC 的 ASCII 码是 27 (0x1B)
            return text.replaceAll("\u001B\\[[;\\d]*m", "");
        }

    }
}
