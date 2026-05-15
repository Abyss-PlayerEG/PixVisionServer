package top.playereg.pix_vision.util;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.asymmetric.KeyType;
import cn.hutool.crypto.asymmetric.RSA;
import cn.hutool.crypto.symmetric.AES;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import top.playereg.pix_vision.config.FilePathConfig;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * RSA 加解密工具类（支持混合加密）
 * <p>
 * 基于 Hutool 实现的 RSA 加密工具，支持纯 RSA 和 AES+RSA 混合加密两种模式。
 * 密钥自动管理，首次启动时生成并保存到用户目录。
 * </p>
 *
 * <h3>密钥存储位置</h3>
 * <pre>${user.home}/.pix_vision/key/rsa/
 * - public.key:  RSA 公钥（Base64 编码）
 * - private.key: RSA 私钥（Base64 编码）</pre>
 *
 * <h3>加密策略</h3>
 * <ul>
 *   <li><b>小数据（&lt; 200字节）</b>：直接使用 RSA 加密，返回格式："RSA:" + Base64(密文)</li>
 *   <li><b>大数据（≥ 200字节）或二进制数据</b>：使用 AES + RSA 混合加密
 *     <ol>
 *       <li>生成随机 AES-256 密钥</li>
 *       <li>用 AES 加密原始数据</li>
 *       <li>用 RSA 加密 AES 密钥</li>
 *       <li>返回格式："HYBRID:" + Base64(RSA(AES密钥)) + "::" + Base64(AES(数据))</li>
 *     </ol>
 *   </li>
 * </ul>
 *
 * <h3>使用场景</h3>
 * <ol>
 *   <li>敏感数据加密存储（密码、Token）</li>
 *   <li>API 传输数据加密</li>
 *   <li>配置文件中的敏感信息加密</li>
 * </ol>
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * // 示例1：加密字符串
 * String encrypted = RSACipher.encryptToBase64("sensitive data");
 *
 * // 示例2：解密字符串
 * String decrypted = RSACipher.decryptToString(encrypted);
 *
 * // 示例3：更换密钥对
 * String[] newKeys = rsaCipher.regenerateKeys();
 * }</pre>
 *
 * <h3>注意事项</h3>
 * <ul>
 *   <li>密钥在应用启动时自动生成，无需手动创建</li>
 *   <li>更换密钥后，使用旧密钥加密的数据将无法解密</li>
 *   <li>私钥文件包含敏感信息，请妥善保管，不要泄露</li>
 *   <li>建议定期备份密钥文件到安全位置</li>
 *   <li>混合加密适用于大文件或二进制数据，性能更优</li>
 * </ul>
 *
 * @author PlayerEG, blue_sky_ks
 * @see cn.hutool.crypto.asymmetric.RSA Hutool RSA 实现
 * @since DEV-2.0.0
 */
@SuppressWarnings("all")
@Component
public class RSACipher {
    private static final PixVisionLogger log = PixVisionLogger.create(RSACipher.class);

    // 密钥文件名
    private static final String PUBLIC_KEY_FILE = "public.key";
    private static final String PRIVATE_KEY_FILE = "private.key";

    // 混合加密分隔符
    private static final String HYBRID_SEPARATOR = "::";

    // RSA 单次加密最大数据长度（2048位密钥 - 填充开销）
    private static final int RSA_MAX_DATA_SIZE = 200;

    // 密钥内容（运行时从文件加载）
    private static String publicKeyBase64;
    private static String privateKeyBase64;

    @Autowired
    private FilePathConfig filePathConfig;

    /**
     * 初始化：检查并加载密钥，如不存在则生成
     *
     * @author PlayerEG
     */
    @PostConstruct
    public void init() {
        loadOrGenerateKeys();
    }

    /**
     * 加载或生成密钥
     * <p>
     * 1. 检查密钥文件是否存在
     * 2. 如果存在，从文件加载密钥
     * 3. 如果不存在，生成新密钥并保存到文件
     *
     * @author PlayerEG
     */
    private void loadOrGenerateKeys() {
        // 确保 FilePathConfig 已初始化
        if (FilePathConfig.KeyPath == null) {
            log.error("FilePathConfig.KeyPath 未初始化，请检查 Bean 初始化顺序");
            throw new IllegalStateException("FilePathConfig 未正确初始化");
        }

        Path rsaDir = Paths.get(FilePathConfig.KeyPath, "rsa");
        Path publicKeyPath = rsaDir.resolve(PUBLIC_KEY_FILE);
        Path privateKeyPath = rsaDir.resolve(PRIVATE_KEY_FILE);

        try {
            // 创建 RSA 密钥目录
            if (!Files.exists(rsaDir)) {
                Files.createDirectories(rsaDir);
                log.info("创建 RSA 密钥目录: {}", rsaDir);
            }

            // 检查密钥文件是否存在
            if (Files.exists(publicKeyPath) && Files.exists(privateKeyPath)) {
                // 从文件加载密钥
                publicKeyBase64 = FileUtil.readUtf8String(publicKeyPath.toFile());
                privateKeyBase64 = FileUtil.readUtf8String(privateKeyPath.toFile());
                log.info("RSA 密钥已从文件加载");
            } else {
                // 生成新密钥
                log.info("未找到 RSA 密钥文件，正在生成新的密钥对...");
                String[] keys = generateRSABase64();
                publicKeyBase64 = keys[0];
                privateKeyBase64 = keys[1];

                // 保存密钥到文件
                FileUtil.writeUtf8String(publicKeyBase64, publicKeyPath.toFile());
                FileUtil.writeUtf8String(privateKeyBase64, privateKeyPath.toFile());
                log.info("RSA 密钥已生成并保存到: {}", rsaDir);
            }
        } catch (Exception e) {
            log.error("RSA 密钥初始化失败", e);
            throw new RuntimeException("RSA 密钥初始化失败", e);
        }
    }

    /**
     * 公钥加密（自动选择加密策略）
     * <p>
     * 根据数据大小自动选择最优加密方式：
     * <ul>
     *   <li><b>小数据（&lt; 200字节）</b>：直接 RSA 加密，返回格式 "RSA:" + Base64(密文)</li>
     *   <li><b>大数据（≥ 200字节）</b>：AES + RSA 混合加密，返回格式 "HYBRID:" + Base64(RSA(AES密钥)) + "::" + Base64(AES(数据))</li>
     * </ul>
     * </p>
     *
     * @param plainText 明文字符串
     * @return Base64 编码的密文，带加密策略前缀
     * @author blue_sky_ks, PlayerEG
     */
    public static String encryptToBase64(String plainText) {
        if (StrUtil.isBlank(plainText)) {
            return "";
        }
        return encryptToBase64(plainText.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 公钥加密字节数组（支持任意二进制数据）
     * <p>
     * 根据数据大小自动选择：
     * - 小数据（&lt; 200字节）：直接 RSA 加密
     * - 大数据（≥ 200字节）：AES + RSA 混合加密
     *
     * @param data 明文数据字节数组
     * @return Base64 编码的密文
     * @author blue_sky_ks
     */
    public static String encryptToBase64(byte[] data) {
        if (data == null || data.length == 0) {
            return "";
        }

        try {
            // 小数据直接使用 RSA 加密
            if (data.length < RSA_MAX_DATA_SIZE) {
                RSA rsa = new RSA(null, publicKeyBase64);
                byte[] encrypted = rsa.encrypt(data, KeyType.PublicKey);
                return "RSA:" + Base64.encode(encrypted);
            }

            // 大数据使用 AES + RSA 混合加密
            return hybridEncrypt(data);
        } catch (Exception e) {
            log.error("RSA 加密失败", e);
            throw new RuntimeException("RSA 加密失败", e);
        }
    }

    /**
     * 私钥解密（自动识别加密策略）
     * <p>
     * 根据密文前缀自动识别加密方式并解密：
     * <ul>
     *   <li><b>RSA:</b> 前缀 - 纯 RSA 解密</li>
     *   <li><b>HYBRID:</b> 前缀 - AES + RSA 混合解密</li>
     *   <li><b>无前缀:</b> 兼容旧格式，尝试纯 RSA 解密</li>
     * </ul>
     * </p>
     *
     * @param encryptedBase64 Base64 编码的密文
     * @return 解密后的明文字符串，如果输入为空则返回 null
     * @author blue_sky_ks, PlayerEG
     */
    public static String decryptToString(String encryptedBase64) {
        if (StrUtil.isBlank(encryptedBase64)) {
            return null;
        }

        byte[] decrypted = decryptToBytes(encryptedBase64);
        if (decrypted == null) {
            return null;
        }

        return new String(decrypted, StandardCharsets.UTF_8);
    }

    /**
     * 私钥解密为字节数组（支持任意二进制数据）
     * <p>
     * 自动识别加密策略：
     * - RSA: 前缀标识，直接 RSA 解密
     * - HYBRID: 包含分隔符，AES + RSA 混合解密
     *
     * @param encryptedBase64 Base64 编码的密文
     * @return 解密后的字节数组
     * @author blue_sky_ks
     */
    public static byte[] decryptToBytes(String encryptedBase64) {
        if (StrUtil.isBlank(encryptedBase64)) {
            return null;
        }

        try {
            // 判断加密策略
            if (encryptedBase64.startsWith("RSA:")) {
                // 纯 RSA 加密
                String actualEncrypted = encryptedBase64.substring(4);
                RSA rsa = new RSA(privateKeyBase64, null);
                return rsa.decrypt(Base64.decode(actualEncrypted), KeyType.PrivateKey);
            } else if (encryptedBase64.contains(HYBRID_SEPARATOR)) {
                // 混合加密
                return hybridDecrypt(encryptedBase64);
            } else {
                // 兼容旧格式（无标识符的纯 RSA 加密）
                RSA rsa = new RSA(privateKeyBase64, null);
                return rsa.decrypt(Base64.decode(encryptedBase64), KeyType.PrivateKey);
            }
        } catch (Exception e) {
            log.error("RSA 解密失败", e);
            throw new RuntimeException("RSA 解密失败", e);
        }
    }

    /**
     * 混合加密：AES + RSA
     * <p>
     * 1. 生成随机 AES 密钥
     * 2. 用 AES 加密原始数据
     * 3. 用 RSA 加密 AES 密钥
     * 4. 返回格式：RSA(AES密钥) + "::" + AES(数据)
     *
     * @param data 原始数据
     * @return 混合加密后的 Base64 字符串
     * @author blue_sky_ks
     */
    private static String hybridEncrypt(byte[] data) {
        try {
            // 1. 生成随机 AES 密钥（256位）
            AES aes = SecureUtil.aes();
            byte[] aesKey = aes.getSecretKey().getEncoded();

            // 2. 用 AES 加密数据
            byte[] encryptedData = aes.encrypt(data);

            // 3. 用 RSA 加密 AES 密钥
            RSA rsa = new RSA(null, publicKeyBase64);
            byte[] encryptedAesKey = rsa.encrypt(aesKey, KeyType.PublicKey);

            // 4. 组合结果：RSA加密的AES密钥 + "::" + AES加密的数据
            String result = Base64.encode(encryptedAesKey) + HYBRID_SEPARATOR + Base64.encode(encryptedData);
            return "HYBRID:" + result;
        } catch (Exception e) {
            log.error("混合加密失败", e);
            throw new RuntimeException("混合加密失败", e);
        }
    }

    /**
     * 混合解密：RSA + AES
     * <p>
     * 1. 分离 RSA 加密的 AES 密钥和 AES 加密的数据
     * 2. 用 RSA 私钥解密得到 AES 密钥
     * 3. 用 AES 密钥解密数据
     *
     * @param encryptedBase64 混合加密的字符串
     * @return 解密后的原始数据
     * @author blue_sky_ks
     */
    private static byte[] hybridDecrypt(String encryptedBase64) {
        try {
            // 移除前缀
            String actualEncrypted = encryptedBase64.startsWith("HYBRID:")
                ? encryptedBase64.substring(7)
                : encryptedBase64;

            // 1. 分离两部分
            String[] parts = actualEncrypted.split(HYBRID_SEPARATOR, 2);
            if (parts.length != 2) {
                throw new IllegalArgumentException("混合加密数据格式错误");
            }

            byte[] encryptedAesKey = Base64.decode(parts[0]);
            byte[] encryptedData = Base64.decode(parts[1]);

            // 2. 用 RSA 私钥解密 AES 密钥
            RSA rsa = new RSA(privateKeyBase64, null);
            byte[] aesKey = rsa.decrypt(encryptedAesKey, KeyType.PrivateKey);

            // 3. 用 AES 密钥解密数据
            AES aes = new AES(aesKey);
            return aes.decrypt(encryptedData);
        } catch (Exception e) {
            log.error("混合解密失败", e);
            throw new RuntimeException("混合解密失败", e);
        }
    }

    /**
     * 生成 RSA 密钥对（Base64 编码）
     *
     * @return 密钥数组，[0] 为公钥，[1] 为私钥
     * @author blue_sky_ks
     */
    public static String[] generateRSABase64() {
        String[] keys = new String[2];

        var keyPair = SecureUtil.generateKeyPair("RSA", 2048);

        // 公钥
        keys[0] = Base64.encode(keyPair.getPublic().getEncoded());

        // 私钥
        keys[1] = Base64.encode(keyPair.getPrivate().getEncoded());

        return keys;
    }

    /**
     * 更换 RSA 密钥对
     * <p>
     * 生成新的密钥对并覆盖原有密钥文件，旧密钥会自动备份为 .bak 文件。
     * <strong>警告：</strong>更换密钥后，使用旧密钥加密的数据将无法解密！
     * </p>
     *
     * @return 新的密钥数组，[0] 为公钥，[1] 为私钥
     * @throws IllegalStateException 如果 FilePathConfig 未正确初始化
     * @author PlayerEG
     */
    public String[] regenerateKeys() {
        try {
            // 确保 FilePathConfig 已初始化
            if (FilePathConfig.KeyPath == null) {
                log.error("FilePathConfig.KeyPath 未初始化");
                throw new IllegalStateException("FilePathConfig 未正确初始化");
            }

            Path rsaDir = Paths.get(FilePathConfig.KeyPath, "rsa");
            Path publicKeyPath = rsaDir.resolve(PUBLIC_KEY_FILE);
            Path privateKeyPath = rsaDir.resolve(PRIVATE_KEY_FILE);

            // 生成新密钥
            log.warn("正在重新生成 RSA 密钥对...");
            String[] newKeys = generateRSABase64();

            // 备份旧密钥（添加 .bak 后缀）
            Path oldPublicKeyPath = rsaDir.resolve(PUBLIC_KEY_FILE + ".bak");
            Path oldPrivateKeyPath = rsaDir.resolve(PRIVATE_KEY_FILE + ".bak");
            if (Files.exists(publicKeyPath)) {
                FileUtil.copy(publicKeyPath.toFile(), oldPublicKeyPath.toFile(), true);
            }
            if (Files.exists(privateKeyPath)) {
                FileUtil.copy(privateKeyPath.toFile(), oldPrivateKeyPath.toFile(), true);
            }
            log.info("旧密钥已备份到: {}.bak", rsaDir);

            // 保存新密钥
            publicKeyBase64 = newKeys[0];
            privateKeyBase64 = newKeys[1];
            FileUtil.writeUtf8String(publicKeyBase64, publicKeyPath.toFile());
            FileUtil.writeUtf8String(privateKeyBase64, privateKeyPath.toFile());

            log.info("RSA 密钥对已更新");
            return newKeys;
        } catch (Exception e) {
            log.error("RSA 密钥更换失败", e);
            throw new RuntimeException("RSA 密钥更换失败", e);
        }
    }

    /**
     * 获取当前公钥（Base64 编码）
     *
     * @return 公钥字符串
     * @author PlayerEG
     */
    public static String getPublicKey() {
        return publicKeyBase64;
    }

    /**
     * 获取当前私钥（Base64 编码）
     * <strong>注意：</strong>私钥应妥善保管，不要泄露
     *
     * @return 私钥字符串
     * @author PlayerEG
     */
    public static String getPrivateKey() {
        return privateKeyBase64;
    }
}
