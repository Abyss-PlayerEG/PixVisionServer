package top.playereg.pix_vision.util;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;

@SpringBootTest
public class PVSUtilsTest {
    String str = "123456";

    @Test
    public void testPVSSha() {
        String result = PVSUtils.PVSSha(str);
        System.out.println("\n" + result);
    }

    @Test
    public void testNumber2Str() {
        String result = PVSUtils.number2Str(str);
        System.out.println(result);
    }

    @Test
    public void testEncryptString() {
        String result = PVSUtils.encryptString(str);
        System.out.println(result);
    }

    @org.junit.Test
    public void string2Bytes() {
        String str = "像素视觉";
        byte[] result = PVSUtils.string2Bytes(str);
        System.out.println(Arrays.toString(result));
    }

    @org.junit.Test
    public void bytes2String() {
        byte[] bytes = new byte[]{-27, -125, -113, -25, -76, -96, -24, -89, -122, -24, -89, -119};
        String result = PVSUtils.bytes2String(bytes);
        System.out.println(result);
    }
}