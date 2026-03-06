package top.playereg.muyingserver.util;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import top.playereg.pix_vision.util.PVSUtils;

@SpringBootTest
public class PVSUtilsTest {
    String str = "123456";

    @Test
    public void testMuYingSha() {
        String result = PVSUtils.MuYingSha(str);
        System.out.println("\n"+result);
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
}