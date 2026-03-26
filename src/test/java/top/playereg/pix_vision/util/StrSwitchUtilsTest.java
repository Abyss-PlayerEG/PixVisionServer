package top.playereg.pix_vision.util;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;

@SpringBootTest
public class StrSwitchUtilsTest {
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
}