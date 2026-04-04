package top.playereg.pix_vision.util;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.asymmetric.KeyType;
import cn.hutool.crypto.asymmetric.RSA;

/**
 * todo RSA加解密工具类
 *
 * @author PlayerEG
 */
@SuppressWarnings("all")
public class RSACipher {

    //公钥
    private static final String PUBLIC_KEY_BASE64 = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA3dYeSXjXtbY7oTHxEZeJ6zoWN971+d6kCAwKy5ab4L8ALTFeqmRlexlQrEP/MH3QxTdE3HNuvn/LfAX9/jc2RpulsTzS7e3aLjqkdxndfk8z/A5huzcqdgnL4t97IvC3+z/n9/095jPKQxkCLNQOnQWXo7gRDZo2PMVoeHCN70bsYFJjkXAr2oqOndtmQIKjzMSJN3vJcGJfXiRFhmE2XI2I9WxnsfcKUYXdbiZZkQahxwMoYzbXvZ9RpQDGp9l8o+tdVNVN5dkbCz6UI66wUhLm9IxvppXg0mvvdvgFCe0gbojB5YzLAsdIojXWpPs1841r595zfCMM1PdEdepanQIDAQAB"; //公钥

    //私钥
    private static final String PRIVATE_KEY_BASE64 = "MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQDd1h5JeNe1tjuhMfERl4nrOhY33vX53qQIDArLlpvgvwAtMV6qZGV7GVCsQ/8wfdDFN0Tcc26+f8t8Bf3+NzZGm6WxPNLt7douOqR3Gd1+TzP8DmG7Nyp2Ccvi33si8Lf7P+f3/T3mM8pDGQIs1A6dBZejuBENmjY8xWh4cI3vRuxgUmORcCvaio6d22ZAgqPMxIk3e8lwYl9eJEWGYTZcjYj1bGex9wpRhd1uJlmRBqHHAyhjNte9n1GlAMan2Xyj611U1U3l2RsLPpQjrrBSEub0jG+mleDSa+92+AUJ7SBuiMHljMsCx0iiNdak+zXzjWvn3nN8IwzU90R16lqdAgMBAAECggEAPAsI7pUFObtF34b/e2X7YA48VkoEZOnUlxL/VwSAU3APMaxDtbXv28ZJ1h6fczYSrFadzld2GZzIbFzI1yaFi830JolShDDVVZ7sxgnbsCTq6qXP4Rb46LMVGuanUxk5iwlMlYxXVTgeXlPmCVEqVWjeluMqxnY/iOpbsQGyARm9fts6b147wOL3ojfOJmUY7WN27KcqkdO6IdzjJFyibc5m0DTK2pVXyc61+8dQ3NU8yuw/cCp3ntIElcQniqWm5YElf1DL6a7ncdrPcpd8wBVRhikxxincgl3maEt/LZXcCKsgj01u46ylOJcxSYiOYz62GBBGbeNoGxm6dwSiVwKBgQD9oFoBDoxODn2TFbyrx/pOGoZHMVOTIlHA0IC/AeLQhupr8Q1fKibBsjnnu2r+RyGu4dLdQ0TgTapaDdabrhje6cQOp/66xB+TlCTgblb1EnR97hiYCSMiiyt9TNz6EzPwT2HlT95InjgWIKTNHjvg2Jf0AnvIH8XfPZjT0L/eTwKBgQDf6ZpfDa+zWeYF/CVj/9IZQ1Bmx8YhPAcqw9SJO/+WfDoTjFb3DE9WLchhS5U2Ca2fUzsvL5c40RxAiiEXzkJWJIt27vX5p/kHEXemMb8aIxuMLnvAeb3lEz2Tx96COHA9g5pFKL4hDFY1EHrDa5MMoZmGbqRKkFQOpjM3MDOJUwKBgBekQ9+HWKLKDQCR7SViHb38EPo/6dd0QoSjquyjI15mxSFMtf9h8XzqvSURvlZ3kPc5S6ueYqQ5+SAt3Axk/SKCTelD1aXZNExQaeOVxXtQvhUjBZ3edCz7JhGnAY46DgRfkfOLL2A7h2TgpBwOrmw2JGv1c05jT9GQb1eHROtXAoGAWUIZHJ0rFjbXADnEefmHujRgP6iRbbwtKzoHZnF5cHay/AnsvOy2T6dOgqKLp4/yG1oldKjnAmoxdfTaPlAll4tX4SBfA5eXKN8osCKSiIvINnhtMowVS9UArgF1zLlM3OiyPbGBIVF2qA6asX4Xj2h39+PxMBwatgBA2FYppSsCgYEAoGoIZgplO39RkAOCDswcD4VF41c6lwLcgcIS3m7c8j+b8gYOPtDNpNQZMap7drsXkvgvZwArXu28lihIT3sm24eiW+euyjHOwdXjGXb+Z+gvBkJMjCRiMsy4WNr89HY+zHlFPOsEvhwJPBCONaGneZVfJ9zS4+qCsWSJxYHp88s="; //私钥

    // todo 公钥加密
    public static String encryptToBase64(String plainText) {
        if (StrUtil.isBlank(plainText)) {
            return "";
        }

        // 只使用公钥
        RSA rsa = new RSA(null, PUBLIC_KEY_BASE64);

        try {
            // Hutool 的 RSA.encrypt 默认不分段，这里我们手动实现分段加密（推荐）
            byte[] data = StrUtil.bytes(plainText, CharsetUtil.CHARSET_UTF_8);
            byte[] encrypted = rsa.encrypt(data, KeyType.PublicKey);  // Hutool 内部已处理部分情况

            return Base64.encode(encrypted);   // 推荐返回 Base64，便于传输
        } catch (Exception e) {
            throw new RuntimeException("RSA公钥加密失败", e);
        }
    }

    // todo 私钥解密
    public static String decryptToString(String encryptedBase64) {
        if (StrUtil.isBlank(encryptedBase64)) {
            return null;
        }

        try {
            // 推荐：只传入私钥，减少不必要的公钥加载
            RSA rsa = new RSA(PRIVATE_KEY_BASE64, null);

            // decryptStr 默认会先做 Base64.decode，然后用私钥解密
            return rsa.decryptStr(encryptedBase64, KeyType.PrivateKey);

        } catch (Exception e) {
            // 实际项目中建议使用专门的日志框架，这里用 e.printStackTrace() 仅作示例
            e.printStackTrace();
            throw new RuntimeException("RSA私钥解密失败", e);
        }
    }

    // todo 密钥生成
    public static String[] generateRSABase64(){
        String[] keys = new String[2];

        var keyPair = SecureUtil.generateKeyPair("RSA", 2048);

        //公钥
        keys[0] = Base64.encode(keyPair.getPublic().getEncoded());

        //私钥
        keys[1] = Base64.encode(keyPair.getPrivate().getEncoded());


        return keys;
    }

    // todo 密钥更换

}
