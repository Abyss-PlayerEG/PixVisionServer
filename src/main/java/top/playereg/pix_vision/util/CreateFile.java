package top.playereg.pix_vision.util;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.playereg.pix_vision.config.FilePathConfig;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 文件创建类
 *
 * @author PlayerEG
 */
public class CreateFile {
    private static final Logger log = LoggerFactory.getLogger(CreateFile.class);

    public static void create() {
        // 根目录说明文件
        FilePathConfig.createTextFile(
            """
                欢迎来到像素视觉用户目录
                该目录用于存放后端资源和自定义内容
                """,
            FilePathConfig.RootPath,
            "readme.txt"
        );

        // application.yml 文件
        FilePathConfig.createTextFile(
            ResourceUtil.readUtf8Str("template/application-template.yml"),
            FilePathConfig.RootPath,
            "application.yml"
        );

        // logo-img 目录说明
        FilePathConfig.createTextFile(
            """
                当前目录为后端自定义logo，可以进行更换

                ---

                dark.png: 用于暗色主题的图标
                light.png: 用于亮色主题的图标
                """,
            FilePathConfig.LogoPath,
            "目录说明.txt"
        );

        FilePathConfig.createByteFile(
            ResourceUtil.readBytes("logo/dark.png"),
            FilePathConfig.LogoPath,
            "dark.png"
        );

        // logo 图片-浅色
        FilePathConfig.createByteFile(
            ResourceUtil.readBytes("logo/light.png"),
            FilePathConfig.LogoPath,
            "light.png"
        );

        // email-html 目录说明
        FilePathConfig.createTextFile(
            """
                当前目录为邮箱HTML模板，可以进行模板自定义

                ---

                验证码邮箱模板 email-verification.html:

                占位符说明：
                    {{logoUriLight}}: 浅色模式 logo Base64
                    {{logoUriDark}}: 深色模式 logo Base64
                    {{username}}：用户名
                    {{email_text}}：邮箱操作内容
                    {{code}}：验证码
                    {{year}}：当前年份
                    {{systemName}}：系统名称
                """,
            FilePathConfig.EmailHtmlPath,
            "目录说明.txt"
        );

        // 邮箱验证码 HTML 模板
        FilePathConfig.createTextFile(
            ResourceUtil.readUtf8Str("template/email-html/email-verification.html"),
            FilePathConfig.EmailHtmlPath,
            "email-verification.html"
        );

        // 默认头像
        copyDefaultAvatars();

        // log 目录说明（如果目录存在）
        if (Files.exists(Paths.get(FilePathConfig.LogPath))) {
            FilePathConfig.createTextFile(
                """
                    日志文件存储目录

                    该目录用于存放应用运行时的日志文件

                    日志配置说明：
                    1. 日志同时输出到控制台和文件
                    2. 捕获所有控制台输出
                    3. 单个日志文件最大 10MB，超过后自动滚动
                    4. 保留最近 30 天的日志文件
                    5. 日志总大小上限为 1GB
                    6. 日志格式包含时间戳、级别、线程、类名等信息

                    注意事项：
                    1. 定期清理过期日志以释放磁盘空间
                    2. 生产环境建议将日志级别设置为 INFO 或 WARN
                    3. 开发环境可设置为 DEBUG 以便调试
                    4. 所有控制台内容都会被完整记录到日志文件中
                    """,
                FilePathConfig.LogPath,
                "目录说明.txt"
            );
        }

        // RSA 密钥目录说明（如果目录存在）
        String rsaKeyDir = FilePathConfig.KeyPath + "/rsa";
        if (Files.exists(Paths.get(rsaKeyDir))) {
            FilePathConfig.createTextFile(
                """
                    RSA 密钥存储目录

                    该目录用于存放 RSA 密钥对文件：
                    - public.key:  RSA 公钥（Base64 编码）
                    - private.key: RSA 私钥（Base64 编码）

                    注意事项：
                    1. 私钥文件包含敏感信息，请妥善保管，不要泄露
                    2. 更换密钥后，使用旧密钥加密的数据将无法解密
                    3. 建议定期备份密钥文件
                    4. .bak 文件为密钥更换前的备份文件
                    """,
                rsaKeyDir,
                "目录说明.txt"
            );
        }
    }

    /**
     * 复制默认头像到用户目录
     * <p>
     * 将 src/main/resources/static/default-avatar 下的所有头像文件
     * 复制到用户目录下的 AvatarPath/default 目录
     *
     * @author PlayerEG
     */
    private static void copyDefaultAvatars() {
        String defaultAvatarDir = FilePathConfig.AvatarPath + "/default";

        // 创建目标目录
        Path targetDir = Paths.get(defaultAvatarDir);
        try {
            Files.createDirectories(targetDir);
        } catch (Exception e) {
            throw new RuntimeException("创建默认头像目录失败：" + defaultAvatarDir, e);
        }


        // 资源目录下的默认头像路径
        String resourceAvatarDir = "static/default-avatar";

        // 头像文件名列表（1.png 到 21.png）
        for (int i = 1; i <= 21; i++) {
            String fileName = i + ".png";
            Path targetPath = targetDir.resolve(fileName);

            // 如果目标文件不存在，则复制
            if (!Files.exists(targetPath)) {
                try {
                    byte[] avatarBytes = ImageUtils.resizeImage(
                        ResourceUtil.readBytes(resourceAvatarDir + "/" + fileName),
                        600, 0,
                        true
                    );
                    FileUtil.writeBytes(avatarBytes, targetPath.toFile());
                    log.info("生成默认头像：{}", targetPath);
                } catch (Exception e) {
                    log.error("生成头像失败：{}", fileName, e);
                }
            }
        }
    }
}
