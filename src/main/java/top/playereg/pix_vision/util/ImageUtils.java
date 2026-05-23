package top.playereg.pix_vision.util;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.StrUtil;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

/**
 * 图像处理工具类
 * <p>
 * 提供图像格式验证、尺寸检测、格式转换、Base64 编解码、图像缩放等功能。
 * 支持 JPG、JPEG、PNG 格式的图像处理，输出格式支持 PNG、JPG。
 * </p>
 *
 * <h3>使用场景</h3>
 * <ol>
 *   <li>用户上传头像时的格式验证和尺寸调整</li>
 *   <li>邮件模板中 Logo 的 Base64 编码</li>
 *   <li>作品图片的压缩和优化</li>
 *   <li>图像格式统一转换（JPG/JPEG -> PNG）</li>
 * </ol>
 *
 * <h3>注意事项</h3>
 * <ul>
 *   <li>仅支持 JPG、JPEG、PNG 三种输入格式，其他格式会抛出异常</li>
 *   <li>输出支持 PNG、JPG 两种目标格式</li>
 *   <li>转换到 JPG 格式时透明背景自动填充白色</li>
 *   <li>图像缩放时保持宽高比，避免变形</li>
 *   <li>Base64ToImage 方法已废弃，推荐使用二进制文件上传</li>
 *   <li>图像缩放使用双三次插值算法，保证高质量</li>
 * </ul>
 *
 * @author PlayerEG
 * @since DEV-2.0.0
 */
public class ImageUtils {
    private static final PixVisionLogger log = PixVisionLogger.create(ImageUtils.class);

    static {
        System.setProperty("java.awt.headless", "true");
    }

    /**
     * 验证文件是否为有效的图片格式
     * <p>
     * 通过检查文件头部的魔数（Magic Number）来判断是否为有效的图片格式。
     * 仅支持 JPG、JPEG、PNG 三种格式。
     * </p>
     *
     * @param imageBytes 文件字节数组
     * @return true-是有效图片，false-不是有效图片或数据为空
     * @author PlayerEG
     */
    public static boolean isValidImage(byte[] imageBytes) {
        if (imageBytes == null || imageBytes.length < 4) {
            return false;
        }

        // 检查常见图片格式的魔数（Magic Number）
        // PNG: 89 50 4E 47
        if (imageBytes[0] == (byte) 0x89 &&
            imageBytes[1] == (byte) 0x50 &&
            imageBytes[2] == (byte) 0x4E &&
            imageBytes[3] == (byte) 0x47
        ) {
            return true;
        }

        // JPEG: FF D8 FF
        if (imageBytes[0] == (byte) 0xFF &&
            imageBytes[1] == (byte) 0xD8 &&
            imageBytes[2] == (byte) 0xFF
        ) {
            return true;
        }

        return false;
    }

    /**
     * 检测图像是否为正方形
     *
     * @param imageBytes 图像字节数组
     * @return true-是正方形，false-不是正方形
     * @throws RuntimeException 如果无法识别图像格式
     * @author PlayerEG
     */
    public static boolean isSquareImage(byte[] imageBytes) {
        if (imageBytes == null || imageBytes.length == 0) {
            throw new IllegalArgumentException("图像数据不能为空");
        }

        try {
            BufferedImage image = readImage(imageBytes);
            int width = image.getWidth();
            int height = image.getHeight();

            boolean isSquare = (width == height);
            log.debug("图像尺寸检测：{}x{}, 是否正方形：{}", width, height, isSquare);

            return isSquare;
        } catch (Exception e) {
            log.error("检测图像是否为正方形失败：{}", e.getMessage(), e);
            throw new RuntimeException("检测图像尺寸失败：" + e.getMessage(), e);
        }
    }

    /**
     * 获取图像宽度（像素）
     *
     * @param imageBytes 图像字节数组
     * @return 图像宽度（像素）
     * @throws IllegalArgumentException 数据为空时
     * @throws RuntimeException 无法识别图像格式时
     * @author PlayerEG
     */
    public static int getImageWidth(byte[] imageBytes) {
        if (imageBytes == null || imageBytes.length == 0) {
            throw new IllegalArgumentException("图像数据不能为空");
        }
        BufferedImage image = readImage(imageBytes);
        return image.getWidth();
    }

    /**
     * 获取图像高度（像素）
     *
     * @param imageBytes 图像字节数组
     * @return 图像高度（像素）
     * @throws IllegalArgumentException 数据为空时
     * @throws RuntimeException 无法识别图像格式时
     * @author PlayerEG
     */
    public static int getImageHeight(byte[] imageBytes) {
        if (imageBytes == null || imageBytes.length == 0) {
            throw new IllegalArgumentException("图像数据不能为空");
        }
        BufferedImage image = readImage(imageBytes);
        return image.getHeight();
    }

    /**
     * 从字节数组读取 BufferedImage
     *
     * @param imageBytes 图像字节数组
     * @return BufferedImage 对象
     * @throws RuntimeException 无法识别图像格式时
     */
    private static BufferedImage readImage(byte[] imageBytes) {
        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(imageBytes);
            BufferedImage image = ImageIO.read(inputStream);
            if (image == null) {
                throw new RuntimeException("无法识别的图像格式");
            }
            return image;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("读取图像失败：" + e.getMessage(), e);
        }
    }

    /**
     * 将图像字节数据编码为指定格式
     *
     * @param imageBytes   图像字节数组
     * @param targetFormat 目标格式（"png" 或 "jpg"）
     * @return 编码后的字节数组
     * @throws IOException 编码失败时
     */
    private static byte[] encodeToFormat(byte[] imageBytes, String targetFormat) throws IOException {
        BufferedImage bufferedImage = readImage(imageBytes);

        // JPG 不支持透明通道，需将 ARGB 转为 RGB
        if ("jpg".equals(targetFormat) && bufferedImage.getType() != BufferedImage.TYPE_INT_RGB) {
            BufferedImage rgbImage = new BufferedImage(
                bufferedImage.getWidth(),
                bufferedImage.getHeight(),
                BufferedImage.TYPE_INT_RGB
            );
            Graphics2D g = rgbImage.createGraphics();
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, rgbImage.getWidth(), rgbImage.getHeight());
            g.drawImage(bufferedImage, 0, 0, null);
            g.dispose();
            bufferedImage = rgbImage;
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        boolean writeSuccess = ImageIO.write(bufferedImage, targetFormat, outputStream);
        byte[] outputBytes = outputStream.toByteArray();

        if (!writeSuccess || outputBytes.length == 0) {
            throw new RuntimeException("图像编码失败，输出为空");
        }

        return outputBytes;
    }

    /**
     * 将任意图像强制转换为指定格式并保存
     * <p>
     * 根据保存路径的扩展名自动识别目标格式，支持 PNG、JPG 两种输出格式。
     * 输入支持 JPG、JPEG、PNG 格式。转换到 JPG 时透明背景自动填充白色。
     * </p>
     *
     * @param image         图像字节数组（支持 jpg、jpeg、png 格式）
     * @param saveImagePath 保存路径，扩展名决定目标格式（.png / .jpg / .jpeg）
     * @throws IllegalArgumentException 参数不合法或扩展名不支持
     * @author PlayerEG
     */
    public static void convertImageFormat(byte[] image, String saveImagePath) {
        if (image == null || image.length == 0) {
            throw new IllegalArgumentException("图像数据不能为空");
        }
        if (StrUtil.isBlank(saveImagePath)) {
            throw new IllegalArgumentException("保存路径不能为空");
        }

        // 从扩展名识别目标格式
        String ext = FileUtil.extName(saveImagePath).toLowerCase();
        String targetFormat = switch (ext) {
            case "png" -> "png";
            case "jpg", "jpeg" -> "jpg";
            default -> throw new IllegalArgumentException(
                "不支持的目标格式: " + ext + "，仅支持 png/jpg/jpeg"
            );
        };

        try {
            // 创建输出目录（如果不存在）
            File outputFile = new File(saveImagePath);
            File parentDir = outputFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }

            byte[] outputBytes = encodeToFormat(image, targetFormat);
            FileUtil.writeBytes(outputBytes, saveImagePath);
            log.info("图像已转换为 {} 格式并保存：{}", targetFormat.toUpperCase(), saveImagePath);
            log.info("原始大小：{} bytes, 输出大小：{} bytes", image.length, outputBytes.length);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("图像格式转换失败：{}, 错误：{}", saveImagePath, e.getMessage(), e);
            throw new RuntimeException("图像格式转换失败：" + e.getMessage(), e);
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
     * @param savePath    图像保存路径
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
     * <p>
     * 支持 JPG、JPEG、PNG 格式的图片缩放，输出统一为 PNG 格式。
     * 使用双三次插值算法保证高质量缩放效果。
     * </p>
     *
     * @param imageBytes          原始图像字节数组
     * @param width               目标宽度（像素），为 0 时保持原始宽度
     * @param height              目标高度（像素），为 0 时保持原始高度
     * @param maintainAspectRatio 是否保持宽高比
     *                            <ul>
     *                              <li>true: 根据非零值等比例缩放</li>
     *                              <li>false: 强制缩放到指定尺寸，可能变形</li>
     *                            </ul>
     * @return 压缩后的图像字节数组（PNG 格式）
     * @throws IllegalArgumentException 如果参数不合法（空数据、负数尺寸、宽高都为0）
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
            BufferedImage originalImage = readImage(imageBytes);

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

            log.info(
                "图像缩放完成：{}x{} -> {}x{}, 原始大小：{} bytes, 压缩后：{} bytes",
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

    /**
     * 智能生成封面缩略图
     * <p>
     * 自动判断图像宽高方向，以较短边为基准缩放至目标尺寸，保持宽高比。
     * 横图（宽大于高）约束高度，竖图（高大于宽）约束宽度，输出为 JPG 格式。
     * 若原图尺寸已小于等于目标尺寸，则直接返回原数据。
     * </p>
     *
     * <h3>使用场景</h3>
     * <ol>
     *   <li>作品封面的懒生成，按需生成 400px 缩略图</li>
     *   <li>列表页、瀑布流等需要统一尺寸缩略图的场景</li>
     * </ol>
     *
     * <h3>使用示例</h3>
     * <pre>{@code
     * byte[] original = FileUtil.readBytes("photo.png");
     * byte[] thumb = ImageUtils.generateThumbnail(original, 400);
     * FileUtil.writeBytes(thumb, "thumb.jpg");
     * }</pre>
     *
     * <h3>注意事项</h3>
     * <ul>
     *   <li>输出固定为 JPG 格式，透明背景自动填充白色</li>
     *   <li>目标尺寸为较短边的像素值，长边等比例缩放</li>
     *   <li>原图小于目标尺寸时不缩放，避免放大失真</li>
     * </ul>
     *
     * @param imageBytes 原始图像字节数组
     * @param targetSize 目标尺寸（较短边的像素值），如 400 表示短边不超过 400px
     * @return JPG 格式的缩略图字节数组
     * @throws IllegalArgumentException 参数不合法时
     * @author PlayerEG
     */
    public static byte[] generateThumbnail(byte[] imageBytes, int targetSize) {
        if (imageBytes == null || imageBytes.length == 0) {
            throw new IllegalArgumentException("图像数据不能为空");
        }
        if (targetSize <= 0) {
            throw new IllegalArgumentException("目标尺寸必须大于 0");
        }

        try {
            int originalWidth = getImageWidth(imageBytes);
            int originalHeight = getImageHeight(imageBytes);

            // 原图已足够小，直接返回 JPG 编码
            if (originalWidth <= targetSize && originalHeight <= targetSize) {
                log.info("原图尺寸 {}x{} 已小于目标 {}，无需缩放", originalWidth, originalHeight, targetSize);
                return encodeToFormat(imageBytes, "jpg");
            }

            // 以较短边为基准缩放
            byte[] resizedBytes;
            if (originalWidth > originalHeight) {
                // 横图：约束高度
                resizedBytes = resizeImage(imageBytes, 0, targetSize, true);
            } else {
                // 竖图或正方形：约束宽度
                resizedBytes = resizeImage(imageBytes, targetSize, 0, true);
            }

            // resizeImage 输出 PNG，转为 JPG（复用编码与透明处理逻辑）
            byte[] jpgBytes = encodeToFormat(resizedBytes, "jpg");

            int resultWidth = getImageWidth(jpgBytes);
            int resultHeight = getImageHeight(jpgBytes);

            log.info("智能压缩完成：{}x{} -> {}x{}, 大小：{} bytes",
                originalWidth, originalHeight, resultWidth, resultHeight, jpgBytes.length);

            return jpgBytes;
        } catch (Exception e) {
            log.error("智能压缩失败：targetSize={}, 错误：{}", targetSize, e.getMessage(), e);
            throw new RuntimeException("封面生成失败：" + e.getMessage(), e);
        }
    }
}
