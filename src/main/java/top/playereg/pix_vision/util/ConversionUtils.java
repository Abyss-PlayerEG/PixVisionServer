package top.playereg.pix_vision.util;

import org.jetbrains.annotations.NotNull;

/**
 * 单位转换工具类
 *
 * @author PlayerEG
 */
@SuppressWarnings("all")
public class ConversionUtils {
    /**
     * 内存单位转换
     *
     * @param bytes 字节数
     * @return 转换后的内存大小
     * @author PlayerEG
     */
    @NotNull
    public static String convertBytes(long bytes) {
        if (bytes < 0) {
            return "Invalid input";
        }

        // 定义单位数组和对应的阈值
        String[] units = {"B", "KB", "MB", "GB", "TB", "PB", "EB"};
        long[] thresholds = {
                1L,
                1024L,
                1024L * 1024L,
                1024L * 1024L * 1024L,
                1024L * 1024L * 1024L * 1024L,
                1024L * 1024L * 1024L * 1024L * 1024L
        };

        int unitIndex = 0;
        // 找到最合适的单位索引
        for (int i = thresholds.length - 1; i >= 0; i--) {
            if (bytes >= thresholds[i]) {
                unitIndex = i;
                break;
            }
        }

        // 计算转换后的值
        double size = bytes / (double) thresholds[unitIndex];

        // 对于字节(B)单位，显示整数；其他单位保留两位小数
        if (unitIndex == 0) {
            return String.format("%d %s", bytes, units[unitIndex]);
        } else {
            return String.format("%.2f %s", size, units[unitIndex]);
        }
    }
}
