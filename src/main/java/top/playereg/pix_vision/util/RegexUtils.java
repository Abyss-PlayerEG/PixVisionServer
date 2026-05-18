package top.playereg.pix_vision.util;

import cn.hutool.core.util.StrUtil;

/**
 * 正则表达式验证工具类
 * <p>
 * 提供常用数据格式的验证功能，包括邮箱、验证码、UUID、用户名、URL、哈希值等。
 * 所有验证方法均返回 boolean 类型，失败时会自动记录调试日志。
 * </p>
 *
 * <h3>使用场景</h3>
 * <ol>
 *   <li>用户注册时的表单数据验证</li>
 *   <li>API 接口参数格式校验</li>
 *   <li>密码强度检查（支持明文和哈希格式）</li>
 *   <li>文件上传前的 URL 格式验证</li>
 * </ol>
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * // 示例1：验证邮箱
 * if (RegexUtils.isEmail(user.getEmail())) {
 *     // 邮箱格式正确
 * }
 *
 * // 示例2：验证6位验证码
 * if (RegexUtils.isVCode(code, 6)) {
 *     // 验证码格式正确
 * }
 *
 * // 示例3：验证密码（支持明文和哈希）
 * if (RegexUtils.isPassword(password)) {
 *     // 密码格式符合要求
 * }
 * }</pre>
 *
 * <h3>注意事项</h3>
 * <ul>
 *   <li>所有验证方法在失败时会记录 DEBUG 级别日志</li>
 *   <li>密码验证支持明文（6-16位）和哈希格式（MD5/SHA1/SHA256/SHA512）</li>
 *   <li>URL 验证要求必须包含 http:// 或 https:// 协议头</li>
 *   <li>用户名允许字母、数字和下划线，长度5-16位</li>
 * </ul>
 *
 * @author PlayerEG
 * @since DEV-2.0.0
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
     * 邮箱格式验证
     * <p>
     * 验证邮箱地址是否符合标准格式：xxx@xxx.xxx
     * 支持字母、数字、下划线、连字符作为用户名部分。
     * </p>
     *
     * @param email 待验证的邮箱地址
     * @return true-格式正确，false-格式错误
     * @author PlayerEG
     */
    public static boolean isEmail(String email) {
        return isMatch("^[a-zA-Z0-9_-]+@[a-zA-Z0-9_-]+(\\.[a-zA-Z0-9_-]+)+$", email, "邮箱", "xxx@xxx.xxx");
    }

    /**
     * 验证码格式验证
     * <p>
     * 验证指定长度的验证码，仅允许大写字母和数字。
     * 常用于邮箱或手机验证码的格式校验。
     * </p>
     *
     * @param vCode  待验证的验证码字符串
     * @param length 验证码长度（通常为 4 或 6）
     * @return true-格式正确，false-格式错误
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
     * 标准 UUID 格式验证（36位，包含连字符）
     * <p>
     * 验证 UUID 是否符合标准格式：xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
     * 例如：550e8400-e29b-41d4-a716-446655440000
     * </p>
     *
     * @param uuid 待验证的 UUID 字符串
     * @return true-格式正确，false-格式错误
     * @author PlayerEG
     */
    public static boolean isValidUuid(String uuid) {
        return isMatch("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$", uuid, "标准UUID", "36位，格式: xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx");
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
     * 密码格式验证
     * <p>
     * 支持两种密码格式验证：
     * <ul>
     *   <li><b>明文密码</b>：6-16位，允许字母、数字、下划线、英文句号</li>
     *   <li><b>哈希密码</b>：32位(MD5)、40位(SHA1)、64位(SHA256)、128位(SHA512)</li>
     * </ul>
     * 根据密码长度自动判断类型并进行相应验证。
     * </p>
     *
     * @param password 待验证的密码（明文或哈希值）
     * @return true-格式正确，false-格式错误或为空
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

    /**
     * 电话号码格式验证
     * <p>
     * 验证中国大陆手机号码格式：
     * <ul>
     *   <li>11位数字</li>
     *   <li>以1开头</li>
     *   <li>第二位为3-9</li>
     *   <li>示例：13800138000</li>
     * </ul>
     * </p>
     *
     * @param phone 待验证的电话号码
     * @return true-格式正确，false-格式错误
     * @author PlayerEG
     */
    public static boolean isPhone(String phone) {
        return isMatch("^1[3-9]\\d{9}$", phone, "电话号码", "11位数字，以1开头，第二位为3-9");
    }

    /**
     * QQ号码格式验证
     * <p>
     * 验证QQ号码格式：
     * <ul>
     *   <li>5-11位数字</li>
     *   <li>不能以0开头</li>
     *   <li>示例：123456789</li>
     * </ul>
     * </p>
     *
     * @param qq 待验证的QQ号码
     * @return true-格式正确，false-格式错误
     * @author PlayerEG
     */
    public static boolean isQQ(String qq) {
        return isMatch("^[1-9]\\d{4,10}$", qq, "QQ号码", "5-11位数字，不能以0开头");
    }

    /**
     * 微信号格式验证
     * <p>
     * 验证微信号格式：
     * <ul>
     *   <li>6-20位</li>
     *   <li>允许字母、数字、下划线、减号</li>
     *   <li>必须以字母开头</li>
     *   <li>示例：wxid_123456</li>
     * </ul>
     * </p>
     *
     * @param wechat 待验证的微信号
     * @return true-格式正确，false-格式错误
     * @author PlayerEG
     */
    public static boolean isWechat(String wechat) {
        return isMatch("^[a-zA-Z][a-zA-Z0-9_-]{5,19}$", wechat, "微信号", "6-20位，以字母开头，允许字母、数字、下划线、减号");
    }

    /**
     * Bilibili UID格式验证
     * <p>
     * 验证Bilibili用户ID格式：
     * <ul>
     *   <li>纯数字</li>
     *   <li>1-10位</li>
     *   <li>示例：123456789</li>
     * </ul>
     * </p>
     *
     * @param bilibiliUid 待验证的Bilibili UID
     * @return true-格式正确，false-格式错误
     * @author PlayerEG
     */
    public static boolean isBilibiliUid(String bilibiliUid) {
        return isMatch("^\\d{1,16}$", bilibiliUid, "Bilibili UID", "1-16位纯数字");
    }
}
