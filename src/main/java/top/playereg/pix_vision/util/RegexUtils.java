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
     * 邮箱格式: xxx@xxx.xxx
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
     * 6位, 只允许字母、数字
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

    /**
     * 用户名正则匹配
     * 6-16位, 只允许字母、数字和_
     * @param username 待匹配的昵称
     * @return boolean
     * @author PlayerEG
     */
    public static boolean isUsername(String username) {
        String regex = "^[a-zA-Z0-9_]{6,16}$";
        if (!username.matches(regex)) {
            log.error("用户名格式错误: {}", username);
            log.info("用户名格式要求: 6-16位, 只允许字母、数字和_");
            return false;
        } else {
            log.info("用户名格式正确: {}", username);
            return true;
        }
    }

    /**
     * 数字正则匹配
     * 只允许数字
     * @param pureNumber 待匹配的纯数字
     * @return boolean
     * @author bule_sky_ks
     */
    public static boolean isPureNumber(String pureNumber) {
        String regex = "^[0-9]*$";

        if( pureNumber.matches(regex) ){
            log.info( "数字格式正确: {}", pureNumber );
            return true;
        }else{
            log.error("数字格式错误: {}", pureNumber);
            log.info("只能输入数字！！");
            return false;
        }
    }


}
