package top.playereg.pix_vision.util;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.playereg.pix_vision.config.SecureConfig;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * 密码处理工具类
 *
 * @author PlayerEG
 */
@SuppressWarnings("all") // 忽略所有警告
public class PVSUtils {
    private static final Logger log = LoggerFactory.getLogger(PVSUtils.class);

    /**
     * 哈希加密处理
     *
     * @param str 待加密的密码
     * @return 加密后的字符串
     * @author PlayerEG
     */
    public static String PVSSha(String str) {
        String resStr;
        String tempStr;
        String salt = SecureConfig.getSalt(); // 获取盐值
        salt = SecureUtil.sha1(salt);   // 将盐值进行哈希加密
        tempStr = SecureUtil.sha1(str); // 将密码进行第一次哈希加密
        tempStr = str + salt;           // 将密码和盐的哈希值进行拼接
        // 循环进行加密处理，共进行5次
        for (int i = 0; i < 5; i++) {
            tempStr = encryptString(tempStr);
            tempStr = number2Str(tempStr);
        }
        tempStr = encryptString(tempStr);

        // 将字符串进行256哈希加密
        resStr = SecureUtil.sha256(tempStr);
        return resStr;
    }

    /**
     * 对字符串进行加密处理
     *
     * @param str 待加密的字符串
     * @return 加密后的字符串
     * @author PlayerEG
     */
    @NotNull
    public static String encryptString(@NotNull String str) {
        /*
         * 加密原理：获取字符串的每个字符，如果是键盘字符串中的字符，则将字符串向前移动一位，否则不变。
         * 比如输入的字符串是：1e4t.dqm，则得到的字符串是：2r5y.fwq
         * */
        String strEncryptList = "1234567890qwertyuiopasdfghjklzxcvbnm"; // 键盘字符串
        StringBuilder tempStr = new StringBuilder();
        for (char c : str.toCharArray()) {
            int index = strEncryptList.indexOf(Character.toLowerCase(c));
            if (index != -1) {
                // 如果是键盘字符串中的最后一个字符，则循环到开头
                char nextChar = strEncryptList.charAt((index + 1) % strEncryptList.length());
                // 保持原始大小写
                if (Character.isUpperCase(c)) {
                    nextChar = Character.toUpperCase(nextChar);
                }
                tempStr.append(nextChar);
            } else {
                // 如果不在键盘字符串中，则保持原样
                tempStr.append(c);
            }
        }
        return tempStr.toString();
    }

    /**
     * 数字转换特定字符串
     *
     * @param str 待转换的字符串
     * @return 加密后的字符串
     * @author PlayerEG
     */
    @NotNull
    public static String number2Str(@NotNull String str) {
        /**
         * 加密原理：将字符串中的数字转换成对应的字符串，非数字则保持原样
         * */
        // 将字符串的每个字符转换一个数组中的每个元素
        char[] chars = str.toCharArray();
        StringBuilder tempStr = new StringBuilder();
        // 然后遍历数组，将数字转换成对应的字符串，非数字则保持原样
        for (char c : chars) {
            if (Character.isDigit(c)) {
                tempStr.append(SecureConfig.getNumber(Integer.parseInt(String.valueOf(c))));
            } else {
                tempStr.append(c);
            }
        }
        return tempStr.toString();
    }

    /**
     * 字符串转换成字节数组
     *
     * @param str 待转换的字符串
     * @return byte[]
     * @author PlayerEG
     */
    public static byte[] string2Bytes(String str) {
        if (StrUtil.isEmpty(str)) {
            return new byte[0];
        }
        // 关键：必须明确指定编码 (推荐 UTF-8)，确保存取一致
        return str.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * 字节数组转换成字符串
     *
     * @param bytes 待转换的字节数组
     * @return String
     * @author PlayerEG
     */
    public static String bytes2String(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return "";
        }
        // 关键：必须使用与写入时相同的编码 (UTF-8)
        return new String(bytes, StandardCharsets.UTF_8);
    }

    /**
     * UUID生成器
     *
     * @return String
     * @author blue_sky_ks
     */
    public static String generateUUID() {
        String res = UUID.randomUUID().toString();
        return res;
    }

    /**
     * 邮箱正则匹配
     *
     * @param email 待匹配的邮箱
     * @return boolean
     * @author PlayerEG
     */
    public static boolean isEmail(String email) {
        String regex = "^[a-zA-Z0-9_-]+@[a-zA-Z0-9_-]+(\\.[a-zA-Z0-9_-]+)+$";
        if (!email.matches(regex)) {
            log.error("邮箱格式错误: {}", email);
            return false;
        } else {
            log.info("邮箱格式正确: {}", email);
            return true;
        }
    }

    /**
     * 验证码正则匹配
     *
     * @param vCode 待匹配的验证码
     * @return boolean
     * @author PlayerEG
     */
    public static boolean isVCode(String vCode) {
        String regex = "^[0-9A-Z]{6}$";
        if (!vCode.matches(regex)) {
            log.error("验证码格式错误: {}", SecureUtil.sha256(SecureUtil.md5(vCode)));
            return false;
        } else {
            log.info("验证码格式正确: {}", SecureUtil.sha256(SecureUtil.md5(vCode)));
            return true;
        }
    }
}