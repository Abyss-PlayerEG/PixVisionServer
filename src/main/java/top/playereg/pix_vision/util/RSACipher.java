package top.playereg.pix_vision.util;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.asymmetric.KeyType;
import cn.hutool.crypto.asymmetric.RSA;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.playereg.pix_vision.config.FilePathConfig;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * RSA加解密工具类
 * <p>
 * 密钥存储位置：${user.home}/.pix_vision/key/rsa/
 * - public.key: RSA公钥（Base64编码）
 * - private.key: RSA私钥（Base64编码）
 *
 * @author PlayerEG
 */
@SuppressWarnings("all")
@Component
public class RSACipher {
    private static final Logger log = LoggerFactory.getLogger(RSACipher.class);

    // 密钥文件名
    private static final String PUBLIC_KEY_FILE = "public.key";
    private static final String PRIVATE_KEY_FILE = "private.key";

    // 密钥内容（运行时从文件加载）
    private static String publicKeyBase64;
    private static String privateKeyBase64;

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
    private static void loadOrGenerateKeys() {
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
     * 公钥加密
     *
     * @param plainText 明文
     * @return Base64 编码的密文
     * @author blue_sky_ks
     */
    public static String encryptToBase64(String plainText) {
        if (StrUtil.isBlank(plainText)) {
            return "";
        }

        // 只使用公钥
        RSA rsa = new RSA(null, publicKeyBase64);

        try {
            byte[] data = StrUtil.bytes(plainText, CharsetUtil.CHARSET_UTF_8);
            byte[] encrypted = rsa.encrypt(data, KeyType.PublicKey);

            return Base64.encode(encrypted);
        } catch (Exception e) {
            log.error("RSA 公钥加密失败", e);
            throw new RuntimeException("RSA 公钥加密失败", e);
        }
    }

    /**
     * 私钥解密
     *
     * @param encryptedBase64 Base64 编码的密文
     * @return 解密后的明文
     * @author blue_sky_ks
     */
    public static String decryptToString(String encryptedBase64) {
        if (StrUtil.isBlank(encryptedBase64)) {
            return null;
        }

        try {
            // 只传入私钥
            RSA rsa = new RSA(privateKeyBase64, null);

            // decryptStr 默认会先做 Base64.decode，然后用私钥解密
            return rsa.decryptStr(encryptedBase64, KeyType.PrivateKey);

        } catch (Exception e) {
            log.error("RSA 私钥解密失败", e);
            throw new RuntimeException("RSA 私钥解密失败", e);
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
     * 生成新的密钥对并覆盖原有密钥文件
     * <strong>注意：</strong>更换密钥后，使用旧密钥加密的数据将无法解密
     *
     * @return 新的密钥数组，[0] 为公钥，[1] 为私钥
     * @author PlayerEG
     */
    public static String[] regenerateKeys() {
        try {
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
