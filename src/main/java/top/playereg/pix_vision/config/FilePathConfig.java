package top.playereg.pix_vision.config;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.CharsetUtil;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import top.playereg.pix_vision.util.ConsoleOutputRedirector;
import top.playereg.pix_vision.util.CreateFile;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;

/**
 * 文件路径配置
 *
 * @author PlayerEG
 */
@SuppressWarnings("all")
@Component
public class FilePathConfig {
    private static final Logger log = LoggerFactory.getLogger(FilePathConfig.class);
    @Value("${workspace-name}")
    private String WorkSpaceName;

    // Root
    public static String RootPath; // 根目录
    // 一级路径
    public static String DataPath; // 数据目录
    public static String ConfigPath; // 配置目录
    public static String LogPath; // 日志目录
    public static String KeyPath; // 密钥目录

    // 二级路径
    public static String LogoPath; // logo 图片目录
    public static String AvatarPath; // 头像目录
    public static String WorksPath; // 作品图片目录
    public static String EmailHtmlPath; // 邮箱 HTML 模板目录

    private String[] paths;

    /**
     * 创建文本文件
     *
     * @param text 文本内容
     * @param path 路径
     * @param file 文件名
     * @author PlayerEG
     */
    public static void createTextFile(
        String text,
        String path,
        String file
    ) {
        Path p = Paths.get(path, file);
        // 检查文件是否已存在
        if (Files.exists(p)) {
            return;
        }
        FileUtil.writeString(text, p.toFile(), CharsetUtil.CHARSET_UTF_8);
        log.info("创建文件: " + p);
    }

    /**
     * 创建二进制文件
     *
     * @param bytes
     * @param path
     * @param file
     * @author PlayerEG
     */
    public static void createByteFile(
        byte[] bytes,
        String path,
        String file
    ) {
        Path p = Paths.get(path, file);
        if (Files.exists(p)) {
            return;
        }
        FileUtil.writeBytes(bytes, p.toFile());
        log.info("创建文件: " + p);
    }

    /**
     * 初始化路径配置
     *
     * @author PlayerEG
     */
    @PostConstruct
    public void initPaths() {
        RootPath = getRootPath(WorkSpaceName);

        DataPath = getPath("data");
        ConfigPath = getPath("config");
        LogPath = getPath("log");
        KeyPath = getPath("key");

        LogoPath = getPath("data", "logo-img");
        AvatarPath = getPath("data", "avatar");
        WorksPath = getPath("data", "works");
        EmailHtmlPath = getPath("config", "email-html");

        paths = new String[]{
            // Root
            RootPath,
            // 一级路径
            DataPath,
            ConfigPath,
            LogoPath,
            EmailHtmlPath,
            // 二级路径
            LogPath,
            AvatarPath,
            WorksPath,
            KeyPath
        };
        initConsoleOutputRedirector();
        createPath();
        CreateFile.create();
    }

    /**
     * 初始化控制台输出重定向
     * 捕获所有 System.out 和 System.err 输出到日志文件
     */
    private void initConsoleOutputRedirector() {
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
     * 获取根目录
     *
     * @param rootPath 根目录
     * @return 根目录
     * @author PlayerEG
     */
    private String getRootPath(String rootPath) {
        return String.valueOf(Paths.get(System.getProperty("user.home"), rootPath));
    }

    /**
     * 获取路径
     *
     * @param subPaths 子路径
     * @return 路径
     * @author PlayerEG
     */
    private String getPath(String... subPaths) {
        return String.valueOf(Paths.get(RootPath, subPaths));
    }

    /**
     * 创建文件路径
     *
     * @throws Exception
     * @author PlayerEG
     */
    public void createPath() {
        // 遍历所有路径并创建不存在的目录
        for (String path : paths) {
            try {
                java.nio.file.Path directoryPath = Paths.get(path);
                if (!Files.exists(directoryPath)) {
                    Files.createDirectories(directoryPath);
                    log.info("创建目录: " + path);
                }
            } catch (Exception e) {
                log.error("创建目录失败: " + path + " " + e.getClass().getName() + ": " + e.getMessage());
            }
        }
    }
}


