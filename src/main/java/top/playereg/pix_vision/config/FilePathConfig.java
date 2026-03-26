package top.playereg.pix_vision.config;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.CharsetUtil;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import top.playereg.pix_vision.util.createFile.CreateFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 文件路径配置
 * @author PlayerEG
 */
@SuppressWarnings("all")
@Component
public class FilePathConfig {
    private static final Logger log = LoggerFactory.getLogger(FilePathConfig.class);
    @Value("${workspace-name}")
    private String WorkSpaceName;

    public static String RootPath; // 根目录
    public static String DataPath; // 数据目录
    public static String LogoPath; // logo 图片目录
    public static String EmailHtmlPath; // 邮箱 HTML 模板目录
//    public static String PluginPath; // 插件目录
    public static String ConfigPath; // 配置目录
    public static String LogPath; // 日志目录
    public static String KeyPath; // 密钥目录

    private String[] paths;

    /**
     * 初始化路径配置
     * @author PlayerEG
     */
    @PostConstruct
    public void initPaths() {
        RootPath = getRootPath(WorkSpaceName);
        DataPath = getPath("data");
        ConfigPath = getPath("config");
        LogoPath = getPath("config","logo-img");
        EmailHtmlPath = getPath("config","email-html");
        LogPath = getPath("log");
        KeyPath = getPath("key");
        
        paths = new String[]{
                RootPath,
                DataPath,
                ConfigPath,
                LogoPath,
                EmailHtmlPath,
                LogPath,
                KeyPath
        };
        
        createPath();
        CreateFile.create();
    }

    /**
     * 获取根目录
     * @param rootPath 根目录
     * @return 根目录
     * @author PlayerEG
     */
    private String getRootPath(String rootPath){
        return String.valueOf(Paths.get(System.getProperty("user.home"), rootPath));
    }

    /**
     * 获取路径
     * @param subPaths 子路径
     * @return 路径
     * @author PlayerEG
     */
    private String getPath(String... subPaths) {
        return String.valueOf(Paths.get(RootPath, subPaths));
    }

    /**
     * 创建文件路径
     * @throws Exception
     * @author PlayerEG
     */
    public void createPath(){
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

    /**
     * 创建文本文件
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
     * @param bytes
     * @param path
     * @param file
     * @author PlayerEG
     */
    public static void createByteFile(
            byte[] bytes,
            String path,
            String file
    ){
        Path p = Paths.get(path, file);
        if (Files.exists(p)){
            return;
        }
        FileUtil.writeBytes(bytes, p.toFile());
        log.info("创建文件: " + p);
    }
}
