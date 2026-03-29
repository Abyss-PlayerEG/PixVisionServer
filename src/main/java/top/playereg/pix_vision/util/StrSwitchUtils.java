package top.playereg.pix_vision.util;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.playereg.pix_vision.config.SecureConfig;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.UUID;

/**
 * 字符串处理工具类
 *
 * @author PlayerEG
 */
@SuppressWarnings("all") // 忽略所有警告
public class StrSwitchUtils {
    private static final Logger log = LoggerFactory.getLogger(StrSwitchUtils.class);

    /**
     * 哈希加密处理
     *
     * @param str 待加密的密码
     * @return 加密后的字符串
     * @author PlayerEG
     */
    public static String PasswdToHash256(String str) {
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
     * UUID 生成器
     *
     * @return String
     * @author blue_sky_ks
     */
    public static String generateUUID() {
        String res = UUID.randomUUID().toString();
        res = res.replace("-", ""); // 去分隔线
        res = res.toLowerCase(); // 转换为小写
        log.info("生成 UUID: {}", res);
        return res;
    }
    
    /**
     * 将 UUID 字符串转换为 16 字节二进制数组
     *
     * @param uuid UUID 字符串（32 位，无分隔符）
     * @return byte[] 16 字节的二进制数组
     * @author PlayerEG
     */
    public static byte[] uuidToBytes(String uuid) {
        if (StrUtil.isEmpty(uuid) || uuid.length() != 32) {
            log.error("UUID 格式错误");
            throw new IllegalArgumentException("UUID 字符串必须为 32 位字符");
        }
            
        byte[] bytes = new byte[16];
        for (int i = 0; i < 16; i++) {
            // 每两个十六进制字符转换为一个字节
            int index = i * 2;
            bytes[i] = (byte) Integer.parseInt(uuid.substring(index, index + 2), 16);
        }
            
        log.info("UUID 转换为 16 字节二进制：{} -> {}", uuid, bytes);
        return bytes;
    }
    
    /**
     * 将 16 字节二进制数组转换为 UUID 字符串
     *
     * @param bytes 16 字节的二进制数组
     * @return String UUID 字符串（32 位，无分隔符，小写）
     * @author PlayerEG
     */
    public static String bytesToUuid(byte[] bytes) {
        if (bytes == null || bytes.length != 16) {
            throw new IllegalArgumentException("二进制数组长度必须为 16");
        }
            
        StringBuilder sb = new StringBuilder(32);
        for (byte b : bytes) {
            // 将每个字节转换为两位十六进制字符串
            sb.append(String.format("%02x", b));
        }
            
        String result = sb.toString().toLowerCase();
        log.info("16 字节二进制 UUID: {}", bytes);
        log.info("转换后 UUID: {}", result);
        return result;
    }
    
    /**
     * 将字节数组转换为十六进制字符串（用于日志输出）
     *
     * @param bytes 字节数组
     * @return String 十六进制字符串
     * @author blue_sky_ks
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /**
     * 随机用户名生成
     *
     * @param prdfix 前缀
     * @return String
     * @author blue_sky_ks
     */
    public static String generateRandomUserDefaultNickName(String prdfix) {
        String AlphaNum = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Integer nameLength = 10;
        String userDefaultName = prdfix + "_";
        final SecureRandom RANDOM = new SecureRandom(); //安全随机
        StringBuilder sb = new StringBuilder(userDefaultName);
        for (int i = 0; i < nameLength; i++) {
            sb.append(AlphaNum.charAt(RANDOM.nextInt(AlphaNum.length())));
        }

        return sb.toString();
    }

    /**
     * 将 Markdown 转换为 HTML
     * @param markdown Markdown 内容
     * @param charset 编码格式
     * @param title 标题
     * @param  cssStyle 样式
     * @return String HTML 内容
     * @author PlayerEG
     */
    public static String markdownToHtml(String markdown,String charset,String title,String cssStyle) {
        // Html模板
        String htmlTemplate = """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                    <meta charset="{}">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>{}</title>
                    <style>{}</style>
                </head>
                <body>
                {}
                </body>
                </html>
                """;
        Parser parser = Parser.builder().build();
        Node document = parser.parse(markdown);
        HtmlRenderer renderer = HtmlRenderer.builder().build();
        String html = renderer.render(document);
        return StrUtil.format(htmlTemplate, charset, title, cssStyle, html);
    }

}