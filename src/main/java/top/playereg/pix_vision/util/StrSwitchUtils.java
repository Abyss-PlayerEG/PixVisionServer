package top.playereg.pix_vision.util;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.playereg.pix_vision.config.SecureConfig;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
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
     * UUID生成器
     *
     * @return String
     * @author blue_sky_ks
     */
    public static String generateUUID() {
        String res = UUID.randomUUID().toString();
        res = res.replace("-", "");
        res = res.toLowerCase();
        log.info("生成UUID: {}", res);
        return res;
    }

    /**
     * 任意图像格式转换 png
     *
     * @param image 图像字节数组（支持 jpg、jpeg、gif、bmp 等格式）
     * @param saveImagePath 保存路径（必须以 .png 结尾）
     * @return void
     * @author PlayerEG
     */
    public static void imageToPng(byte[] image, String saveImagePath) {
        if (image == null || image.length == 0) {
            throw new IllegalArgumentException("图像数据不能为空");
        }
        if (StrUtil.isBlank(saveImagePath)) {
            throw new IllegalArgumentException("保存路径不能为空");
        }
        if (!saveImagePath.toLowerCase().endsWith(".png")) {
            throw new IllegalArgumentException("保存路径必须以 .png 结尾");
        }
            
        try {
            // 将字节数组转换为 BufferedImage
            ByteArrayInputStream inputStream = new ByteArrayInputStream(image);
            BufferedImage bufferedImage = ImageIO.read(inputStream);
                
            if (bufferedImage == null) {
                throw new RuntimeException("无法识别的图像格式");
            }
                
            // 创建输出目录（如果不存在）
            File outputFile = new File(saveImagePath);
            File parentDir = outputFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }
                
            // 转换为 PNG 格式并保存
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(bufferedImage, "png", outputStream);
            byte[] pngBytes = outputStream.toByteArray();
                
            FileUtil.writeBytes(pngBytes, saveImagePath);
            log.info("图像已转换为 PNG 格式并保存：{}", saveImagePath);
            log.info("原始大小：{} bytes, PNG 大小：{} bytes", image.length, pngBytes.length);
        } catch (Exception e) {
            log.error("图像转 PNG 失败：{}, 错误：{}", saveImagePath, e.getMessage(), e);
            throw new RuntimeException("图像转 PNG 失败：" + e.getMessage(), e);
        }
    }

    /**
     * 图像转换为Base64
     *
     * @param imagePath 图像路径
     * @return String
     * @author PlayerEG
     */
    public static String imageToBase64(String imagePath) {
        byte[] imageBytes = ResourceUtil.readBytes(imagePath);
        String base64image = StrUtil.format("data:image/png;base64,{}",Base64.encode(imageBytes));
        return base64image;
    }

    /**
     * Base64 转换为图像
     *
     * @deprecated
     * @param base64image Base64 字符串 (格式：data:image/png;base64,/9j/...)
     * @param savePath 图像保存路径
     * @return void
     * @author PlayerEG
     */
    public static void base64ToImage(String base64image, String savePath) {
        // 参数验证
        if (StrUtil.isBlank(base64image)) {
            throw new IllegalArgumentException("Base64 字符串不能为空");
        }
        if (StrUtil.isBlank(savePath)) {
            throw new IllegalArgumentException("保存路径不能为空");
        }
            
        try {
            // 移除 Base64 前缀 (如：data:image/png;base64,)
            String base64Data = base64image;
            if (base64image.contains(",")) {
                base64Data = base64image.split(",", 2)[1];
            }
                
            // Base64 解码
            byte[] imageBytes = Base64.decode(base64Data);
                
            // 写入文件
            FileUtil.writeBytes(imageBytes, savePath);
            log.info("保存图像：{}", savePath);
        } catch (Exception e) {
            log.error("Base64 转图像失败：{}, 错误：{}", savePath, e.getMessage(), e);
            throw new RuntimeException("Base64 转图像失败：" + e.getMessage(), e);
        }
    }
}