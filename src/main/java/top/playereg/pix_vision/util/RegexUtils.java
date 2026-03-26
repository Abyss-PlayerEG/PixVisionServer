package top.playereg.pix_vision.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 正则匹配工具类
 *
 * @author PlayerEG
 */
public class RegexUtils {
    private static final Logger log = LoggerFactory.getLogger(RegexUtils.class);
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
            return true;
        } else {
            log.info("邮箱格式正确: {}", email);
            return false;
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
            log.error("验证码格式错误: {}", vCode);
            return false;
        } else {
            log.info("验证码格式正确: {}", vCode);
            return true;
        }
    }
}
