package top.playereg.pix_vision.util.createFile;

import cn.hutool.core.io.resource.ResourceUtil;
import top.playereg.pix_vision.config.FilePathConfig;

public class CreateApplicationYML {

    public static void create() {
        // 从classpath下的模板文件读取内容
        String text = ResourceUtil.readUtf8Str("template/application-template.yml");
        
        FilePathConfig.createFile(
                text,
                FilePathConfig.RootPath,
                "application.yml"
        );
    }
}
