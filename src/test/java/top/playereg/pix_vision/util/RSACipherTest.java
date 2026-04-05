package top.playereg.pix_vision.util;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RSA加解密工具测试类
 *
 * @author PlayerEG
 */
@SpringBootTest
public class RSACipherTest {
    private static final Logger log = LoggerFactory.getLogger(RSACipherTest.class);

    /**
     * 测试密钥初始化
     */
    @Test
    public void testKeyInitialization() {
        // 由于 RSACipher 是 Spring Bean，会在应用启动时自动初始化
        // 这里主要验证密钥是否正确加载
        String publicKey = RSACipher.getPublicKey();
        String privateKey = RSACipher.getPrivateKey();

        assertNotNull(publicKey, "公钥不应为 null");
        assertNotNull(privateKey, "私钥不应为 null");
        assertFalse(publicKey.isEmpty(), "公钥不应为空");
        assertFalse(privateKey.isEmpty(), "私钥不应为空");

        log.info("RSA 密钥初始化成功");
        log.debug("公钥: {}", publicKey.substring(0, Math.min(50, publicKey.length())) + "...");
    }

    /**
     * 测试加密和解密
     */
    @Test
    public void testEncryptAndDecrypt() {
        String originalText = "Hello, PixVision! 这是一个测试消息。";
        
        // 加密
        String encrypted = RSACipher.encryptToBase64(originalText);
        assertNotNull(encrypted, "加密结果不应为 null");
        assertFalse(encrypted.isEmpty(), "加密结果不应为空");
        log.info("原文: {}", originalText);
        log.info("密文: {}", encrypted);

        // 解密
        String decrypted = RSACipher.decryptToString(encrypted);
        assertNotNull(decrypted, "解密结果不应为 null");
        assertEquals(originalText, decrypted, "解密后的文本应与原文一致");
        log.info("解密后: {}", decrypted);
    }

    /**
     * 测试空字符串处理
     */
    @Test
    public void testEmptyString() {
        String encrypted = RSACipher.encryptToBase64("");
        assertEquals("", encrypted, "空字符串加密应返回空字符串");

        String decrypted = RSACipher.decryptToString("");
        assertNull(decrypted, "空字符串解密应返回 null");
    }

    /**
     * 测试 null 处理
     */
    @Test
    public void testNullHandling() {
        String encrypted = RSACipher.encryptToBase64(null);
        assertEquals("", encrypted, "null 加密应返回空字符串");

        String decrypted = RSACipher.decryptToString(null);
        assertNull(decrypted, "null 解密应返回 null");
    }

    /**
     * 测试密钥生成
     */
    @Test
    public void testKeyGeneration() {
        String[] keys = RSACipher.generateRSABase64();
        assertNotNull(keys, "密钥数组不应为 null");
        assertEquals(2, keys.length, "密钥数组长度应为 2");
        assertNotNull(keys[0], "公钥不应为 null");
        assertNotNull(keys[1], "私钥不应为 null");
        assertFalse(keys[0].isEmpty(), "公钥不应为空");
        assertFalse(keys[1].isEmpty(), "私钥不应为空");

        log.info("密钥生成成功");
        log.debug("新生成的公钥: {}", keys[0].substring(0, Math.min(50, keys[0].length())) + "...");
    }

    /**
     * 测试密钥更换
     */
    @Test
    public void testKeyRegeneration() {
        // 记录旧公钥
        String oldPublicKey = RSACipher.getPublicKey();
        
        // 更换密钥
        String[] newKeys = RSACipher.regenerateKeys();
        assertNotNull(newKeys, "新密钥数组不应为 null");
        
        // 验证新密钥已加载
        String newPublicKey = RSACipher.getPublicKey();
        assertNotEquals(oldPublicKey, newPublicKey, "新公钥应与旧公钥不同");
        
        log.info("密钥更换成功");
        log.warn("注意：使用旧密钥加密的数据将无法用新密钥解密");
    }

    /**
     * 测试中文和特殊字符
     */
    @Test
    public void testChineseAndSpecialCharacters() {
        String text = "你好世界！";
        
        String encrypted = RSACipher.encryptToBase64(text); // 加密
        assertNotNull(encrypted, "加密结果不应为 null");
        String decrypted = RSACipher.decryptToString(encrypted); // 解密
        assertNotNull(decrypted, "解密结果不应为 null");

        System.out.println("原文: " + text);
        System.out.println("密文: " + encrypted);
        System.out.println("解密后: " + decrypted);
        
        assertEquals(text, decrypted, "包含中文和特殊字符的文本应能正确加解密");
        log.info("中文和特殊字符测试通过");
    }
}
