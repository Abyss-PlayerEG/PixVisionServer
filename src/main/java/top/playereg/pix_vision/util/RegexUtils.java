package top.playereg.pix_vision.util;

import cn.hutool.core.util.StrUtil;

/**
 * 正则匹配工具类
 *
 * @author PlayerEG
 */
@SuppressWarnings("all")
public class RegexUtils {
    private static final PixVisionLogger log = PixVisionLogger.create(RegexUtils.class);

    /**
     * 正则表达模板
     *
     * @param regex           正则表达式
     * @param text            待匹配的文本
     * @param strType         文本类型
     * @param returnTextError 错误提示
     * @return boolean 匹配结果
     * @author PlayerEG
     */
    private static boolean isMatch(String regex, String text, String strType, String returnTextError) {
        if (text.matches(regex)) {
            log.debug(StrUtil.format("{}格式匹配成功: {}", strType, text));
            return true;
        } else {
            log.debug(StrUtil.format("{}格式匹配失败: {}", strType, text));
            log.debug(StrUtil.format("{}格式要求: {}"), strType, returnTextError);
            return false;
        }
    }

    /**
     * 邮箱正则匹配
     * 邮箱格式: xxx@xxx.xxx
     *
     * @param email 待匹配的邮箱
     * @return boolean
     * @author PlayerEG
     */
    public static boolean isEmail(String email) {
        return isMatch("^[a-zA-Z0-9_-]+@[a-zA-Z0-9_-]+(\\.[a-zA-Z0-9_-]+)+$", email, "邮箱", "xxx@xxx.xxx");
    }

    /**
     * 验证码正则匹配
     * 6位, 只允许大写字母、数字
     *
     * @param vCode 待匹配的验证码
     * @return boolean
     * @author PlayerEG
     */
    public static boolean isVCode(String vCode, int length) {
        return isMatch("^[0-9A-Z]{" + length + "}$", vCode, "验证码", StrUtil.format("{}位长度, 只允许大写字母、数字", length));
    }

    /**
     * UUID正则匹配
     * 32位, 只允许字母、数字
     *
     * @param uuid 待匹配的UUID
     * @return boolean
     * @author PlayerEG
     */
    public static boolean isUUID(String uuid) {
        return isMatch("^[0-9a-zA-Z]{32}$", uuid, "UUID", "32位, 只允许字母、数字");
    }

    /**
     * 用户名正则匹配
     * 6-16位, 只允许字母、数字和_
     *
     * @param username 待匹配的昵称
     * @return boolean
     * @author PlayerEG
     */
    public static boolean isUsername(String username) {
        return isMatch("^[a-zA-Z0-9_]{5,16}$", username, "用户名", "5-16位, 只允许字母、数字和_");
    }

    /**
     * 数字正则匹配
     * 只允许数字
     *
     * @param pureNumber 待匹配的纯数字
     * @return boolean
     * @author bule_sky_ks
     */
    public static boolean isPureNumber(String pureNumber) {
        return isMatch("^[0-9]*$", pureNumber, "纯数字", "只能输入数字");
    }

    /**
     * URL正则匹配
     * 必须包含 http:// 或 https:// 协议头
     *
     * @param url 待匹配的URL
     * @return boolean
     * @author PlayerEG
     */
    public static boolean isURL(String url) {
        return isMatch("^https?:\\/\\/([\\w-]+\\.)+[\\w-]+(\\/[\\w-./?%&=]*)?$", url, "URL", "http://example.com 或 https://example.com/path");
    }

    /**
     * 哈希值正则匹配
     * 支持常用哈希算法：MD5、SHA1、SHA224、SHA256、SHA384、SHA512、CRC32、BCrypt
     *
     * @param hash     待匹配的哈希值
     * @param hashType 哈希类型：MD5、SHA1、SHA224、SHA256、SHA384、SHA512、CRC32、BCrypt
     * @return boolean 匹配结果
     * @author PlayerEG
     */
    public static boolean isHash(String hash, String hashType) {
        if (hash == null || hash.isEmpty()) {
            return false;
        }

        return switch (hashType.toUpperCase()) {
            // CRC32: 8位十六进制字符串
            case "CRC32" -> isMatch("^[0-9a-fA-F]{8}$", hash, "CRC32哈希值", "8位十六进制字符串");
            // MD5: 32位十六进制字符串
            case "MD5" -> isMatch("^[0-9a-fA-F]{32}$", hash, "MD5哈希值", "32位十六进制字符串");
            // SHA1: 40位十六进制字符串
            case "SHA1" -> isMatch("^[0-9a-fA-F]{40}$", hash, "SHA1哈希值", "40位十六进制字符串");
            // SHA224: 56位十六进制字符串
            case "SHA224" -> isMatch("^[0-9a-fA-F]{56}$", hash, "SHA224哈希值", "56位十六进制字符串");
            // SHA256: 64位十六进制字符串
            case "SHA256" -> isMatch("^[0-9a-fA-F]{64}$", hash, "SHA256哈希值", "64位十六进制字符串");
            // SHA384: 96位十六进制字符串
            case "SHA384" -> isMatch("^[0-9a-fA-F]{96}$", hash, "SHA384哈希值", "96位十六进制字符串");
            // SHA512: 128位十六进制字符串
            case "SHA512" -> isMatch("^[0-9a-fA-F]{128}$", hash, "SHA512哈希值", "128位十六进制字符串");
            // BCrypt: 60位Base64编码字符串（以$2a$、$2b$或$2y$开头）
            case "BCRYPT" ->
                isMatch("^\\$2[aby]\\$[0-9]{2}\\$[./A-Za-z0-9]{53}$", hash, "BCrypt哈希值", "60位，格式: $2a$XX$...");
            // 其他类型: 不支持
            default -> false;
        };
    }

    /**
     * 密码正则匹配
     * <p>
     * 支持明文密码和哈希密码格式验证
     *
     * @param password 待匹配的密码
     * @return boolean
     * @author PlayerEG
     */
    public static boolean isPassword(String password) {
        // 空值检查
        if (password == null || password.isEmpty()) {
            log.debug("密码格式匹配失败: 密码为空");
            return false;
        }

        int length = password.length();
        String strType = "密码";
        String returnTextError = "6-16位, 只允许字母、数字、下划线、英文句号";

        // 根据长度判断密码类型
        if (length >= 6 && length <= 16) {
            // 明文密码: 6-16位, 只允许字母、数字、下划线、英文句号
            log.debug("当前密码长度({})", strType, length);
            return isMatch("^[a-zA-Z0-9_.]{6,16}$", password, strType, returnTextError);
        } else if (length == 32 || length == 40 || length == 64 || length == 128) {
            // 哈希密码
            log.debug("当前密码长度({})", strType, length);
            return switch (length) {
                // MD5: 32位十六进制字符串
                case 32 -> isHash(password, "MD5");
                // SHA1: 40位十六进制字符串
                case 40 -> isHash(password, "SHA1");
                // SHA256: 64位十六进制字符串
                case 64 -> isHash(password, "SHA256");
                // SHA512: 128位十六进制字符串
                case 128 -> isHash(password, "SHA512");
                // 理论上不会到达这里（外层已过滤）
                default -> false;
            };
        } else {
            // 长度不符合要求
            log.debug("{}格式匹配失败: 长度({})不符合要求", strType, length);
            return false;
        }
    }
}
