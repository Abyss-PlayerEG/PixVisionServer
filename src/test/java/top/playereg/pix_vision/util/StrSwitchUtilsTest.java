package top.playereg.pix_vision.util;

import cn.hutool.core.io.FileUtil;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.util.Arrays;

@SpringBootTest
class StrSwitchUtilsTest {
    String str = "123456";

    @Test
    public void testPasswdToHash256() {
        String result = StrSwitchUtils.PasswdToHash256(str);
        System.out.println("\n" + result);
    }

    @Test
    public void testNumber2Str() {
        String result = StrSwitchUtils.number2Str(str);
        System.out.println(result);
    }

    @Test
    public void testEncryptString() {
        String result = StrSwitchUtils.encryptString(str);
        System.out.println(result);
    }

    @Test
    public void string2Bytes() {
        String str = "像素视觉";
        byte[] result = StrSwitchUtils.string2Bytes(str);
        System.out.println(Arrays.toString(result));
    }

    @Test
    public void bytes2String() {
        byte[] bytes = new byte[]{-27, -125, -113, -25, -76, -96, -24, -89, -122, -24, -89, -119};
        String result = StrSwitchUtils.bytes2String(bytes);
        System.out.println(result);
    }

    @Test
    public void generateUUID() {
        String result = StrSwitchUtils.generateUUID();
        System.out.println(result);
    }

    @Test
    void imageToPng() {
        String inputPath = System.getProperty("user.home") + "/Pictures/壁纸/macos_12_monterey_official_stock_wallpaper_6k_resolution_light-3840x2160.jpg";
        String savePath = System.getProperty("user.home") + "/Desktop/out.png";
        
        // 检查文件是否存在
        File inputFile = new File(inputPath);
        if (!inputFile.exists()) {
            System.out.println("错误：文件不存在 - " + inputPath);
            return;
        }
        
        // 使用 FileUtil.readBytes() 读取系统文件（不是 classpath 资源）
        byte[] imageBytes = FileUtil.readBytes(inputFile);
        
        // 执行格式转换
        StrSwitchUtils.imageToPng(imageBytes, savePath);
        
        System.out.println("图片已保存到：" + savePath);
    }

    @Test
    void markdownToHtml() {
        String markdown = """
                # 一级标题
                ---
                ## 二级标题
                ---
                ### 三级标题
                - 无序1
                - 无序2
                    1. 有序1
                    2. 有序2
                ---
                > 引用部分
                >> 内嵌引用
                
                **黑体**
                
                *斜体*
                
                `行内代码`
                
                ```python
                print("代码块")
                ```
                """;
        String html = StrSwitchUtils.markdownToHtml(markdown, "utf-8", "md2html");
        System.out.println(html);
        FileUtil.writeUtf8String(html, System.getProperty("user.home") + "/Desktop/md2html.html");
    }
}