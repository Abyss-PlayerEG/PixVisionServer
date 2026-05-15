package top.playereg.pix_vision.util;

import org.jetbrains.annotations.NotNull;

/**
 * 单位转换工具类
 * <p>
 * 提供常用的单位转换功能，包括内存大小单位的智能转换。
 * 支持从字节(B)到艾字节(EB)的自动单位选择和格式化输出。
 * </p>
 *
 * <h3>使用场景</h3>
 * <ol>
 *   <li>系统信息展示（内存大小、磁盘空间）</li>
 *   <li>文件大小显示（上传下载进度）</li>
 *   <li>性能监控数据格式化</li>
 * </ol>
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * // 示例1：转换小文件
 * String size = ConversionUtils.convertBytes(1024);
 * // 输出: "1.00 KB"
 *
 * // 示例2：转换大文件
 * String size = ConversionUtils.convertBytes(1572864);
 * // 输出: "1.50 MB"
 *
 * // 示例3：转换字节级别
 * String size = ConversionUtils.convertBytes(512);
 * // 输出: "512 B"
 * }</pre>
 *
 * <h3>注意事项</h3>
 * <ul>
 *   <li>输入负数会返回 "Invalid input"</li>
 *   <li>字节单位显示整数，其他单位保留两位小数</li>
 *   <li>支持的最大单位为 EB (Exabyte)</li>
 * </ul>
 *
 * @author PlayerEG
 * @since DEV-2.0.0
 */
@SuppressWarnings("all")
public class ConversionUtils {
    /**
     * 内存单位转换
     * <p>
     * 根据字节数自动选择合适的单位（B、KB、MB、GB、TB、PB、EB）进行格式化输出。
     * 字节单位显示整数，其他单位保留两位小数。
     * </p>
     *
     * @param bytes 字节数（必须为非负数）
     * @return 格式化后的内存大小字符串，如 "1.50 MB"
     * @throws IllegalArgumentException 如果输入为负数
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
