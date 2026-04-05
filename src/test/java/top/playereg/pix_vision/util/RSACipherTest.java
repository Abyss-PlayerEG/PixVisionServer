package top.playereg.pix_vision.util;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RSA加解密工具测试类
 *
 * @author PlayerEG
 */
@SpringBootTest
class RSACipherTest {
    private static final Logger log = LoggerFactory.getLogger(RSACipherTest.class);

    /**
     * 测试密钥初始化
     */
    @Test
    void testKeyInitialization() {
        // 由于 RSACipher 是 Spring Bean，会在应用启动时自动初始化
        // 这里主要验证密钥是否正确加载
        String publicKey = RSACipher.getPublicKey();
        String privateKey = RSACipher.getPrivateKey();

        assertNotNull(publicKey, "公钥不应为 null");
        assertNotNull(privateKey, "私钥不应为 null");
        assertFalse(publicKey.isEmpty(), "公钥不应为空");
        assertFalse(privateKey.isEmpty(), "私钥不应为空");

        log.info("RSA 密钥初始化成功");
        log.debug("公钥前50字符: {}...", publicKey.substring(0, Math.min(50, publicKey.length())));
    }

    /**
     * 测试小文本加密解密（纯 RSA）
     */
    @Test
    void testSmallTextEncryption() {
        String originalText = "Hello, PixVision!";
        
        // 加密
        String encrypted = RSACipher.encryptToBase64(originalText);
        assertNotNull(encrypted, "加密结果不应为 null");
        assertFalse(encrypted.isEmpty(), "加密结果不应为空");
        assertTrue(encrypted.startsWith("RSA:"), "小数据应使用纯 RSA 加密");
        
        log.info("原文: {}", originalText);
        log.info("密文: {}", encrypted);

        // 解密
        String decrypted = RSACipher.decryptToString(encrypted);
        assertNotNull(decrypted, "解密结果不应为 null");
        assertEquals(originalText, decrypted, "解密后的文本应与原文一致");
        
        log.info("解密后: {}", decrypted);
        log.info("小文本加密测试通过");
    }

    /**
     * 测试中文加密解密
     */
    @Test
    void testChineseTextEncryption() {
        String text = "你好世界！这是一个中文测试。";
        
        String encrypted = RSACipher.encryptToBase64(text);
        assertNotNull(encrypted, "加密结果不应为 null");
        
        String decrypted = RSACipher.decryptToString(encrypted);
        assertNotNull(decrypted, "解密结果不应为 null");
        assertEquals(text, decrypted, "中文文本应能正确加解密");
        
        System.out.println("原文: " + text);
        System.out.println("密文: " + encrypted);
        System.out.println("解密后: " + decrypted);
        
        log.info("中文文本加密测试通过");
    }

    /**
     * 测试特殊字符加密解密
     */
    @Test
    void testSpecialCharactersEncryption() {
        String text = "@#$%^&*()_+-=[]{}|;':\",./<>?`~\\";
        
        String encrypted = RSACipher.encryptToBase64(text);
        String decrypted = RSACipher.decryptToString(encrypted);
        
        assertEquals(text, decrypted, "特殊字符应能正确加解密");
        log.info("特殊字符加密测试通过");
    }

    /**
     * 测试大数据加密（混合加密）
     */
    @Test
    void testLargeDataEncryption() {
        // 生成超过 200 字节的文本
        StringBuilder largeText = new StringBuilder();
        for (int i = 0; i < 50; i++) {
            largeText.append("这是一段很长的文本，用于测试大数据加密功能。第")
                    .append(i).append("行。\n");
        }
        String originalText = largeText.toString();
        
        log.info("原文长度: {} 字节", originalText.length());
        
        // 加密
        String encrypted = RSACipher.encryptToBase64(originalText);
        assertNotNull(encrypted, "加密结果不应为 null");
        assertTrue(encrypted.startsWith("HYBRID:"), "大数据应使用混合加密");
        log.info("密文长度: {} 字节", encrypted.length());
        log.info("密文前100字符: {}...", encrypted.substring(0, Math.min(100, encrypted.length())));

        // 解密
        String decrypted = RSACipher.decryptToString(encrypted);
        assertNotNull(decrypted, "解密结果不应为 null");
        assertEquals(originalText, decrypted, "解密后的文本应与原文一致");
        log.info("解密后长度: {} 字节", decrypted.length());
        
        log.info("大数据加密测试通过");
    }

    /**
     * 测试二进制数据加密
     */
    @Test
    void testBinaryDataEncryption() {
        // 创建测试二进制数据（模拟图片、文件等）
        byte[] binaryData = new byte[1024];
        for (int i = 0; i < binaryData.length; i++) {
            binaryData[i] = (byte) (i % 256);
        }
        
        log.info("二进制数据长度: {} 字节", binaryData.length);
        
        // 加密
        String encrypted = RSACipher.encryptToBase64(binaryData);
        assertNotNull(encrypted, "加密结果不应为 null");
        assertTrue(encrypted.startsWith("HYBRID:"), "二进制数据应使用混合加密");
        log.info("加密后长度: {} 字节", encrypted.length());

        // 解密
        byte[] decrypted = RSACipher.decryptToBytes(encrypted);
        assertNotNull(decrypted, "解密结果不应为 null");
        assertArrayEquals(binaryData, decrypted, "解密后的数据应与原文一致");
        
        log.info("二进制数据加密测试通过");
    }

    /**
     * 测试空字符串处理
     */
    @Test
    void testEmptyString() {
        String encrypted = RSACipher.encryptToBase64("");
        assertEquals("", encrypted, "空字符串加密应返回空字符串");

        String decrypted = RSACipher.decryptToString("");
        assertNull(decrypted, "空字符串解密应返回 null");
        
        log.info("空字符串处理测试通过");
    }

    /**
     * 测试 null 处理
     */
    @Test
    void testNullHandling() {
        String encrypted = RSACipher.encryptToBase64((String) null);
        assertEquals("", encrypted, "null 加密应返回空字符串");

        String decrypted = RSACipher.decryptToString(null);
        assertNull(decrypted, "null 解密应返回 null");
        
        byte[] decryptedBytes = RSACipher.decryptToBytes(null);
        assertNull(decryptedBytes, "null 解密为字节数组应返回 null");
        
        log.info("null 处理测试通过");
    }

    /**
     * 测试字节数组 null 和空处理
     */
    @Test
    void testByteArrayNullAndEmpty() {
        // null 字节数组
        String encrypted1 = RSACipher.encryptToBase64((byte[]) null);
        assertEquals("", encrypted1, "null 字节数组加密应返回空字符串");
        
        // 空字节数组
        String encrypted2 = RSACipher.encryptToBase64(new byte[0]);
        assertEquals("", encrypted2, "空字节数组加密应返回空字符串");
        
        log.info("字节数组空值处理测试通过");
    }

    /**
     * 测试密钥生成
     */
    @Test
    void testKeyGeneration() {
        String[] keys = RSACipher.generateRSABase64();
        
        assertNotNull(keys, "密钥数组不应为 null");
        assertEquals(2, keys.length, "密钥数组长度应为 2");
        assertNotNull(keys[0], "公钥不应为 null");
        assertNotNull(keys[1], "私钥不应为 null");
        assertFalse(keys[0].isEmpty(), "公钥不应为空");
        assertFalse(keys[1].isEmpty(), "私钥不应为空");
        
        // 验证生成的密钥可以用于加解密
        String testData = "测试密钥生成";
        String encrypted = RSACipher.encryptToBase64(testData);
        String decrypted = RSACipher.decryptToString(encrypted);
        assertEquals(testData, decrypted, "新生成的密钥应能正常加解密");

        log.info("密钥生成测试通过");
        log.debug("新生成的公钥前50字符: {}...", keys[0].substring(0, Math.min(50, keys[0].length())));
    }

    /**
     * 测试密钥更换
     */
    @Test
    void testKeyRegeneration() {
        // 记录旧公钥
        String oldPublicKey = RSACipher.getPublicKey();
        String oldPrivateKey = RSACipher.getPrivateKey();
        
        assertNotNull(oldPublicKey, "旧公钥不应为 null");
        
        // 更换密钥
        String[] newKeys = RSACipher.regenerateKeys();
        assertNotNull(newKeys, "新密钥数组不应为 null");
        
        // 验证新密钥已加载
        String newPublicKey = RSACipher.getPublicKey();
        String newPrivateKey = RSACipher.getPrivateKey();
        
        assertNotEquals(oldPublicKey, newPublicKey, "新公钥应与旧公钥不同");
        assertNotEquals(oldPrivateKey, newPrivateKey, "新私钥应与旧私钥不同");
        
        // 验证新密钥可以正常使用
        String testData = "测试密钥更换";
        String encrypted = RSACipher.encryptToBase64(testData);
        String decrypted = RSACipher.decryptToString(encrypted);
        assertEquals(testData, decrypted, "新密钥应能正常加解密");
        
        log.info("密钥更换测试通过");
        log.warn("注意：使用旧密钥加密的数据将无法用新密钥解密");
    }

    /**
     * 测试自动选择加密策略
     */
    @Test
    void testAutoEncryptionStrategy() {
        // 小数据（< 200 字节）- 应该使用纯 RSA 加密
        String smallText = "小数据测试";
        String smallEncrypted = RSACipher.encryptToBase64(smallText);
        assertTrue(smallEncrypted.startsWith("RSA:"), "小数据应使用纯 RSA 加密");
        String smallDecrypted = RSACipher.decryptToString(smallEncrypted);
        assertEquals(smallText, smallDecrypted);
        log.info("小数据加密策略: {}...", smallEncrypted.substring(0, Math.min(30, smallEncrypted.length())));

        // 边界测试：接近 200 字节
        StringBuilder boundaryText = new StringBuilder();
        for (int i = 0; i < 25; i++) {
            boundaryText.append("测试");
        }
        String boundaryEncrypted = RSACipher.encryptToBase64(boundaryText.toString());
        log.info("边界数据大小: {} 字节, 加密策略: {}", 
                boundaryText.toString().getBytes(StandardCharsets.UTF_8).length,
                boundaryEncrypted.startsWith("RSA:") ? "RSA" : "HYBRID");

        // 大数据（>= 200 字节）- 应该使用混合加密
        StringBuilder largeText = new StringBuilder();
        for (int i = 0; i < 30; i++) {
            largeText.append("这是第").append(i).append("行数据。");
        }
        String largeEncrypted = RSACipher.encryptToBase64(largeText.toString());
        assertTrue(largeEncrypted.startsWith("HYBRID:"), "大数据应使用混合加密");
        String largeDecrypted = RSACipher.decryptToString(largeEncrypted);
        assertEquals(largeText.toString(), largeDecrypted);
        log.info("大数据加密策略: {}...", largeEncrypted.substring(0, Math.min(30, largeEncrypted.length())));
        
        log.info("自动加密策略选择测试通过");
    }

    /**
     * 测试多次加密同一数据产生不同密文
     */
    @Test
    void testDifferentCiphertextForSamePlaintext() {
        String originalText = "测试数据";
        
        // 多次加密同一数据
        String encrypted1 = RSACipher.encryptToBase64(originalText);
        String encrypted2 = RSACipher.encryptToBase64(originalText);
        String encrypted3 = RSACipher.encryptToBase64(originalText);
        
        // 验证密文不同（因为 AES 密钥随机生成）
        assertNotEquals(encrypted1, encrypted2, "相同明文应产生不同密文");
        assertNotEquals(encrypted2, encrypted3, "相同明文应产生不同密文");
        assertNotEquals(encrypted1, encrypted3, "相同明文应产生不同密文");
        
        // 但解密后应该相同
        String decrypted1 = RSACipher.decryptToString(encrypted1);
        String decrypted2 = RSACipher.decryptToString(encrypted2);
        String decrypted3 = RSACipher.decryptToString(encrypted3);
        
        assertEquals(originalText, decrypted1);
        assertEquals(originalText, decrypted2);
        assertEquals(originalText, decrypted3);
        
        log.info("多次加密测试通过：相同明文产生了不同的密文");
    }

    /**
     * 测试长文本性能
     */
    @Test
    void testLongTextPerformance() {
        // 生成 10KB 的文本
        StringBuilder longText = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            longText.append("这是一段测试文本，用于性能测试。第").append(i).append("行。\n");
        }
        String originalText = longText.toString();
        
        log.info("长文本大小: {} 字节 ({} KB)", 
                originalText.getBytes(StandardCharsets.UTF_8).length,
                originalText.getBytes(StandardCharsets.UTF_8).length / 1024);
        
        // 测试加密性能
        long encryptStart = System.currentTimeMillis();
        String encrypted = RSACipher.encryptToBase64(originalText);
        long encryptEnd = System.currentTimeMillis();
        long encryptTime = encryptEnd - encryptStart;
        
        log.info("加密耗时: {} ms", encryptTime);
        assertTrue(encryptTime < 5000, "加密耗时应小于 5 秒");
        
        // 测试解密性能
        long decryptStart = System.currentTimeMillis();
        String decrypted = RSACipher.decryptToString(encrypted);
        long decryptEnd = System.currentTimeMillis();
        long decryptTime = decryptEnd - decryptStart;
        
        log.info("解密耗时: {} ms", decryptTime);
        assertTrue(decryptTime < 5000, "解密耗时应小于 5 秒");
        
        assertEquals(originalText, decrypted, "长文本应能正确加解密");
        
        log.info("长文本性能测试通过");
    }

    /**
     * 测试各种数据类型
     */
    @Test
    void testVariousDataTypes() {
        // 数字字符串
        String numbers = "1234567890";
        assertEquals(numbers, RSACipher.decryptToString(RSACipher.encryptToBase64(numbers)));
        
        // JSON 格式
        String json = "{\"name\":\"张三\",\"age\":25,\"email\":\"zhangsan@example.com\"}";
        assertEquals(json, RSACipher.decryptToString(RSACipher.encryptToBase64(json)));
        
        // XML 格式
        String xml = "<user><name>李四</name><age>30</age></user>";
        assertEquals(xml, RSACipher.decryptToString(RSACipher.encryptToBase64(xml)));
        
        // Base64 字符串
        String base64Str = "SGVsbG8sIFdvcmxkIQ==";
        assertEquals(base64Str, RSACipher.decryptToString(RSACipher.encryptToBase64(base64Str)));
        
        // URL
        String url = "https://example.com/api/user?id=123&name=测试";
        assertEquals(url, RSACipher.decryptToString(RSACipher.encryptToBase64(url)));
        
        log.info("各种数据类型测试通过");
    }

    /**
     * 测试异常输入
     */
    @Test
    void testInvalidInput() {
        // 测试无效 Base64 字符串
        assertThrows(RuntimeException.class, () -> {
            RSACipher.decryptToString("这不是有效的Base64字符串!!!");
        }, "无效 Base64 应抛出异常");
        
        // 测试损坏的密文
        assertThrows(RuntimeException.class, () -> {
            RSACipher.decryptToString("RSA:损坏的密文数据");
        }, "损坏的密文应抛出异常");
        
        log.info("异常输入测试通过");
    }

    /**
     * 测试获取密钥
     */
    @Test
    void testGetKeys() {
        String publicKey = RSACipher.getPublicKey();
        String privateKey = RSACipher.getPrivateKey();
        
        assertNotNull(publicKey, "公钥不应为 null");
        assertNotNull(privateKey, "私钥不应为 null");
        
        // 验证密钥格式（Base64 编码的 RSA 密钥通常以特定字符开头）
        assertTrue(publicKey.length() > 100, "公钥长度应合理");
        assertTrue(privateKey.length() > 100, "私钥长度应合理");
        
        log.info("获取密钥测试通过");
        log.debug("公钥长度: {} 字符", publicKey.length());
        log.debug("私钥长度: {} 字符", privateKey.length());
    }
}