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
                "欢迎来到像素视觉用户目录，\n该目录用于存放后端资源和自定义内容",
                FilePathConfig.RootPath,
                "readme.txt"
        );

        // application.yml 文件
        String text = ResourceUtil.readUtf8Str("template/application-template.yml");
        FilePathConfig.createTextFile(
                text,
                FilePathConfig.RootPath,
                "application.yml"
        );

        // logo-img 目录说明
        String logoImgAbout = ResourceUtil.readUtf8Str("logo/about-path.txt");
        FilePathConfig.createTextFile(
                logoImgAbout,
                FilePathConfig.LogoPath,
                "目录说明.txt"
        );

        // logo 图片-深色
        byte[] darkLogoBytes = ResourceUtil.readBytes("logo/dark.png");
        FilePathConfig.createByteFile(
                darkLogoBytes,
                FilePathConfig.LogoPath,
                "dark.png"
        );

        // logo 图片-浅色
        byte[] lightLogoBytes = ResourceUtil.readBytes("logo/light.png");
        FilePathConfig.createByteFile(
                lightLogoBytes,
                FilePathConfig.LogoPath,
                "light.png"
        );

        // email-html 目录说明
        String emailHtmlAbout = ResourceUtil.readUtf8Str("template/email-html/about-path.txt");
        FilePathConfig.createTextFile(
                emailHtmlAbout,
                FilePathConfig.EmailHtmlPath,
                "目录说明.txt"
        );

        // 邮箱验证码 HTML 模板
        String emailVerificationText = ResourceUtil.readUtf8Str("template/email-html/email-verification.html");
        FilePathConfig.createTextFile(
                emailVerificationText,
                FilePathConfig.EmailHtmlPath,
                "email-verification.html"
        );

        // 默认头像
        copyDefaultAvatars();
    }

    /**
     * 复制默认头像到用户目录
     * 
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
                    byte[] avatarBytes = ResourceUtil.readBytes(resourceAvatarDir + "/" + fileName);
                    byte[] resizeAvatarBytes = ImageUtils.resizeImage(avatarBytes, 600, 0, true);
                    FileUtil.writeBytes(resizeAvatarBytes, targetPath.toFile());
                    log.info("生成默认头像：{}", targetPath);
                } catch (Exception e) {
                    log.error("生成头像失败：{}", fileName, e);
                }
            }
        }
    }
}
