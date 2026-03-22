package top.playereg.pix_vision.util.createFile;

import cn.hutool.core.io.resource.ResourceUtil;
import top.playereg.pix_vision.config.FilePathConfig;

public class CreateFile {

    /**
     * 创建外部 application.yml 文件
     *
     * @author PlayerEG
     */
    public static void createApplicationYML() {
        // 从classpath下的模板文件读取内容
        String text = ResourceUtil.readUtf8Str("template/application-template.yml");
        
        FilePathConfig.createFile(
                text,
                FilePathConfig.RootPath,
                "application.yml"
        );
    }

    /**
     * 创建外部 email-html 文件夹并创建 email-verification.html 文件
     *
     * @author PlayerEG
     */
    public static void crateEmailHtml() {
        // 邮箱模板目录说明
        String aboutText = ResourceUtil.readUtf8Str("template/email-html/about-path.txt");
        FilePathConfig.createFile(
                aboutText,
                FilePathConfig.EmailHtmlPath,
                "目录说明.txt"
        );
        // 邮箱验证码 HTML 模板
        String emailVerificationText = ResourceUtil.readUtf8Str("template/email-html/email-verification.html");
        FilePathConfig.createFile(
                emailVerificationText,
                FilePathConfig.EmailHtmlPath,
                "email-verification.html"
        );
    }
}
