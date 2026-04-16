package top.playereg.pix_vision.util;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.StrUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;

public class ImageUtils {
    private static final Logger log = LoggerFactory.getLogger(ImageUtils.class);
    
    /**
     * 验证文件是否为有效的图片格式（通过魔数检查）
     * 仅支持 JPG、JPEG、PNG 格式
     *
     * @param imageBytes 文件字节数组
     * @return true-是有效图片，false-不是有效图片
     * @author PlayerEG
     */
    public static boolean isValidImage(byte[] imageBytes) {
        if (imageBytes == null || imageBytes.length < 4) {
            return false;
        }
        
        // 检查常见图片格式的魔数（Magic Number）
        // PNG: 89 50 4E 47
        if (imageBytes[0] == (byte) 0x89 && imageBytes[1] == (byte) 0x50 && 
            imageBytes[2] == (byte) 0x4E && imageBytes[3] == (byte) 0x47) {
            return true;
        }
        
        // JPEG: FF D8 FF
        if (imageBytes[0] == (byte) 0xFF && imageBytes[1] == (byte) 0xD8 && 
            imageBytes[2] == (byte) 0xFF) {
            return true;
        }
        
        return false;
    }
    
    /**
     * 任意图像强制格式转换为 png
     *
     * @param image         图像字节数组（支持 jpg、jpeg、gif、bmp 等格式）
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
        // 获取图像原格式
        String imgTypeName = FileUtil.extName(imagePath);
        // 如果获取失败，则默认为 png
        if (imgTypeName == null || imgTypeName.isEmpty()) {
            imgTypeName = "png";
        }
        String base64image = StrUtil.format("data:image/{};base64,{}", imgTypeName, Base64.encode(imageBytes));
        return base64image;
    }

    /**
     * Base64 转换为图像
     *
     * @param base64image Base64 字符串 (格式：data:image/png;base64,/9j/...)
     * @param savePath 图像保存路径
     * @return void
     * @author PlayerEG
     * @deprecated 图像上传已确定为二进制文件上传
     */
    @Deprecated
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

    /**
     * 图像分辨率缩放
     * 
     * @param imageBytes 原始图像字节数组
     * @param width 目标宽度（像素），为 0 时保持原始宽度
     * @param height 目标高度（像素），为 0 时保持原始高度
     * @param maintainAspectRatio 是否保持宽高比，true 时根据 width 和 height 中非零值等比例缩放
     * @return byte[] 压缩后的图像字节数组（PNG 格式）
     * @author PlayerEG
     */
    public static byte[] resizeImage(byte[] imageBytes, int width, int height, boolean maintainAspectRatio) {
        if (imageBytes == null || imageBytes.length == 0) {
            throw new IllegalArgumentException("图像数据不能为空");
        }
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException("宽度和高度必须为非负数");
        }
        if (width == 0 && height == 0) {
            throw new IllegalArgumentException("宽度和高度至少指定一个非零值");
        }

        try {
            // 将字节数组转换为 BufferedImage
            ByteArrayInputStream inputStream = new ByteArrayInputStream(imageBytes);
            BufferedImage originalImage = ImageIO.read(inputStream);

            if (originalImage == null) {
                // 提供更详细的错误信息
                String errorMsg = String.format(
                    "无法识别的图像格式。文件大小: %d bytes, 请确认上传的是有效的 JPG/JPEG/PNG 图片文件",
                    imageBytes.length
                );
                log.error(errorMsg);
                throw new RuntimeException(errorMsg);
            }

            int originalWidth = originalImage.getWidth();
            int originalHeight = originalImage.getHeight();

            // 计算目标尺寸
            int targetWidth = width;
            int targetHeight = height;

            if (maintainAspectRatio) {
                // 保持宽高比缩放
                if (width == 0) {
                    // 只指定了高度，按高度等比例计算宽度
                    targetWidth = (int) Math.round((double) originalWidth * height / originalHeight);
                } else if (height == 0) {
                    // 只指定了宽度，按宽度等比例计算高度
                    targetHeight = (int) Math.round((double) originalHeight * width / originalWidth);
                } else {
                    // 同时指定了宽度和高度，选择较小的缩放比例以保持宽高比
                    double widthRatio = (double) width / originalWidth;
                    double heightRatio = (double) height / originalHeight;
                    double ratio = Math.min(widthRatio, heightRatio);
                    targetWidth = (int) Math.round(originalWidth * ratio);
                    targetHeight = (int) Math.round(originalHeight * ratio);
                }
            }

            // 如果目标尺寸与原图相同，直接返回原数据
            if (targetWidth == originalWidth && targetHeight == originalHeight) {
                log.info("图像尺寸无需调整：{}x{}", originalWidth, originalHeight);
                return imageBytes;
            }

            // 创建缩放后的 BufferedImage
            BufferedImage resizedImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = resizedImage.createGraphics();

            // 设置渲染质量
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // 绘制缩放后的图像
            g2d.drawImage(originalImage, 0, 0, targetWidth, targetHeight, null);
            g2d.dispose();

            // 压缩为 PNG 格式
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(resizedImage, "png", outputStream);
            byte[] resizedBytes = outputStream.toByteArray();

            log.info("图像缩放完成：{}x{} -> {}x{}, 原始大小：{} bytes, 压缩后：{} bytes",
                    originalWidth,
                    originalHeight,
                    targetWidth,
                    targetHeight,
                    imageBytes.length,
                    resizedBytes.length
            );

            return resizedBytes;
        } catch (Exception e) {
            log.error("图像缩放失败：{}x{}, 错误：{}", width, height, e.getMessage(), e);
            throw new RuntimeException("图像缩放失败：" + e.getMessage(), e);
        }
    }
}
