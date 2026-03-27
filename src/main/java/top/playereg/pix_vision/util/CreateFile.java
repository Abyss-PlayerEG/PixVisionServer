package top.playereg.pix_vision.util;

import cn.hutool.core.io.resource.ResourceUtil;
import top.playereg.pix_vision.config.FilePathConfig;

/**
 * 文件创建类
 *
 * @author PlayerEG
 */
public class CreateFile {
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
    }
}
