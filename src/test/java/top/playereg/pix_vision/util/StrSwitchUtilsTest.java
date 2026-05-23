package top.playereg.pix_vision.util;

import cn.hutool.core.io.FileUtil;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;

@Disabled
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
        String html = StrSwitchUtils.markdownToHtml(markdown, "utf-8", "md2html","");
        System.out.println(html);
        FileUtil.writeUtf8String(html, System.getProperty("user.home") + "/Desktop/md2html.html");
    }

    @Test
    void testGenerateRandomPassword(){
        String res = StrSwitchUtils.generateRandomPassword();

        System.out.println( "密码生成: " + res);

    }
}
