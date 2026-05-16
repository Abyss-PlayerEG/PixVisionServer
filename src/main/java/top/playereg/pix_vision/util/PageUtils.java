package top.playereg.pix_vision.util;

import top.playereg.pix_vision.pojo.ResponsePojo;

/**
 * 分页工具类
 *
 * @author PlayerEG
 */
public class PageUtils {

    /**
     * 默认最大每页大小
     */
    private static final long DEFAULT_MAX_SIZE = 500L;

    /**
     * 校验分页参数
     *
     * @param current 当前页码
     * @param size    每页大小
     * @return 如果参数无效，返回错误响应；如果参数有效，返回 null
     */
    public static ResponsePojo<?> validatePageParams(Long current, Long size) {
        return validatePageParams(current, size, DEFAULT_MAX_SIZE);
    }

    /**
     * 校验分页参数（支持自定义最大每页大小）
     *
     * @param current 当前页码
     * @param size    每页大小
     * @param maxSize 最大每页大小
     * @return 如果参数无效，返回错误响应；如果参数有效，返回 null
     */
    public static ResponsePojo<?> validatePageParams(Long current, Long size, long maxSize) {
        if (current == null || current < 1) {
            return ResponsePojo.error(null, "页码必须大于 0");
        }
        if (size == null || size < 1 || size > maxSize) {
            return ResponsePojo.error(null, "每页大小必须在 1-" + maxSize + " 之间");
        }
        return null;
    }

    /**
     * 获取有效的页码（如果无效则使用默认值 1）
     *
     * @param current 当前页码
     * @return 有效的页码
     */
    public static long getValidCurrent(Long current) {
        return (current != null && current >= 1) ? current : 1L;
    }

    /**
     * 获取有效的每页大小（如果无效则使用默认值 10，并限制最大值）
     *
     * @param size 每页大小
     * @return 有效的每页大小
     */
    public static long getValidSize(Long size) {
        return getValidSize(size, DEFAULT_MAX_SIZE, 10L);
    }

    /**
     * 获取有效的每页大小（如果无效则使用默认值，并限制最大值）
     *
     * @param size        每页大小
     * @param maxSize     最大每页大小
     * @param defaultSize 默认每页大小
     * @return 有效的每页大小
     */
    public static long getValidSize(Long size, long maxSize, long defaultSize) {
        if (size == null || size < 1) {
            return defaultSize;
        }
        return Math.min(size, maxSize);
    }
}
