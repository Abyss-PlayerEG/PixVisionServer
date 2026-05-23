package top.playereg.pix_vision.util;

import cn.hutool.core.io.FileUtil;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ImageUtils convertImageFormat 方法单元测试
 * <p>
 * 运行前修改 INPUT_PATH 为测试用图像路径，OUTPUT_DIR 为输出目录。
 * </p>
 *
 * @author PlayerEG
 */
class ImageUtilsTest {

    /**
     * 测试图像路径
     */
    private static final String INPUT_PATH = "/Users/playereg/Downloads/test/get.png";
    /**
     * 输出目录
     */
    private static final String OUTPUT_DIR = "/Users/playereg/Downloads/test/out/";

    @Test
    void convertImageFormat() throws IOException {
        byte[] imageBytes = FileUtil.readBytes(INPUT_PATH);

        ImageUtils.convertImageFormat(imageBytes, OUTPUT_DIR + "output.png");
        ImageUtils.convertImageFormat(imageBytes, OUTPUT_DIR + "output.jpg");

        assertTrue(new File(OUTPUT_DIR + "output.png").exists());
        assertTrue(new File(OUTPUT_DIR + "output.jpg").exists());
        System.out.println("PNG/JPG 全部转换完成");
    }

    @Test
    void generateThumbnail() throws IOException {
        byte[] imageBytes = FileUtil.readBytes(INPUT_PATH);
        // 生成缩略图
        byte[] thumbBytes = ImageUtils.generateThumbnail(imageBytes, 400);
        // 检查缩略图大小
        assertTrue(thumbBytes.length > 0);

        // 保存缩略图
        FileUtil.writeBytes(thumbBytes, OUTPUT_DIR + "thumb.jpg");
        System.out.println("封面大小：" + thumbBytes.length + " bytes");
    }

    @Test
    void getImageWidth() throws IOException {
        byte[] imageBytes = FileUtil.readBytes(INPUT_PATH);
        int width = ImageUtils.getImageWidth(imageBytes);
        assertTrue(width > 0);
        System.out.println("图像宽度：" + width + "px");
    }

    @Test
    void getImageHeight() throws IOException {
        byte[] imageBytes = FileUtil.readBytes(INPUT_PATH);
        int height = ImageUtils.getImageHeight(imageBytes);
        assertTrue(height > 0);
        System.out.println("图像高度：" + height + "px");
    }
}
