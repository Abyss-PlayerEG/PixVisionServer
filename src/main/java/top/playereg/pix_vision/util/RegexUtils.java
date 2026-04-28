package top.playereg.pix_vision.util;

import cn.hutool.core.util.StrUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 正则匹配工具类
 *
 * @author PlayerEG
 */
@SuppressWarnings("all")
public class RegexUtils {
    private static final Logger log = LoggerFactory.getLogger(RegexUtils.class);

    /**
     * 正则表达模板
     *
     * @param regex           正则表达式
     * @param text            待匹配的文本
     * @param strType         文本类型
     * @param returnTextError 错误提示
     * @return boolean 匹配结果
     * @author PlayerEG
     */
    private static boolean isMatch(
        String regex,
        String text,
        String strType,
        String returnTextError
    ) {
        if (text.matches(regex)) {
            log.debug(StrUtil.format("{}格式匹配成功: {}", strType, text));
            return true;
        } else {
            log.debug(StrUtil.format("{}格式匹配失败: {}", strType, text));
            log.debug(StrUtil.format("{}格式要求: {}"), strType, returnTextError);
            return false;
        }
    }

    /**
     * 邮箱正则匹配
     * 邮箱格式: xxx@xxx.xxx
     *
     * @param email 待匹配的邮箱
     * @return boolean
     * @author PlayerEG
     */
    public static boolean isEmail(String email) {
        return isMatch(
                "^[a-zA-Z0-9_-]+@[a-zA-Z0-9_-]+(\\.[a-zA-Z0-9_-]+)+$",
                email,
                "邮箱",
                "xxx@xxx.xxx"
        );
    }

    /**
     * 验证码正则匹配
     * 6位, 只允许大写字母、数字
     *
     * @param vCode 待匹配的验证码
     * @return boolean
     * @author PlayerEG
     */
    public static boolean isVCode(String vCode, int length) {
        return isMatch(
                "^[0-9A-Z]{" + length + "}$",
                vCode,
                "验证码",
                StrUtil.format("{}位长度, 只允许大写字母、数字", length)
        );
    }

    /**
     * UUID正则匹配
     * 32位, 只允许字母、数字
     *
     * @param uuid 待匹配的UUID
     * @return boolean
     * @author PlayerEG
     */
    public static boolean isUUID(String uuid) {
        return isMatch(
                "^[0-9a-zA-Z]{32}$",
                uuid,
                "UUID",
                "32位, 只允许字母、数字"
        );
    }

    /**
     * 用户名正则匹配
     * 6-16位, 只允许字母、数字和_
     *
     * @param username 待匹配的昵称
     * @return boolean
     * @author PlayerEG
     */
    public static boolean isUsername(String username) {
        return isMatch(
                "^[a-zA-Z0-9_]{5,16}$",
                username,
                "用户名",
                "5-16位, 只允许字母、数字和_"
        );
    }

    /**
     * 数字正则匹配
     * 只允许数字
     *
     * @param pureNumber 待匹配的纯数字
     * @return boolean
     * @author bule_sky_ks
     */
    public static boolean isPureNumber(String pureNumber) {
        return isMatch(
                "^[0-9]*$",
                pureNumber,
                "纯数字",
                "只能输入数字"
        );
    }

    /**
     * URL正则匹配
     * 必须包含 http:// 或 https:// 协议头
     *
     * @param url 待匹配的URL
     * @return boolean
     * @author PlayerEG
     */
    public static boolean isURL(String url) {
        return isMatch(
            "^https?:\\/\\/([\\w-]+\\.)+[\\w-]+(\\/[\\w-./?%&=]*)?$",
            url,
            "URL",
            "http://example.com 或 https://example.com/path"
        );
    }

    /**
     * 密码正则匹配
     * 6-16位, 只允许字母、数字和_
     *
     * @param password 待匹配的密码
     * @return boolean
     * @author PlayerEG
     */
    public static boolean isPassword(String password) {
        return isMatch(
            "^[a-zA-Z0-9_.]{6,16}$",
                password,
                "密码",
                "6-16位, 只允许字母、数字、下划线、英文句号"
        );
    }
}
