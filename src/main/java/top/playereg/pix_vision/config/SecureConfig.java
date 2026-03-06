package top.playereg.pix_vision.config;

import cn.hutool.core.util.StrUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "mu-ying-secure")
@Configuration
@Component
@SuppressWarnings("all")
public class SecureConfig {
    private static SecureConfig instance;

    public SecureConfig() {
        instance = this;
    }

    /**
     * 哈希加密配置
     */
    @Value("${mu-ying-secure.salt}")
    private String salt;

    public static String getSalt() {
        return instance != null ? instance.salt : "mu-ying-salt";
    }

    /**
     * 数字转字符串配置
     */
    @Value("${mu-ying-secure.num2str[0]}")
    private String[] number0;
    @Value("${mu-ying-secure.num2str[1]}")
    private String[] number1;
    @Value("${mu-ying-secure.num2str[2]}")
    private String[] number2;
    @Value("${mu-ying-secure.num2str[3]}")
    private String[] number3;
    @Value("${mu-ying-secure.num2str[4]}")
    private String[] number4;
    @Value("${mu-ying-secure.num2str[5]}")
    private String[] number5;
    @Value("${mu-ying-secure.num2str[6]}")
    private String[] number6;
    @Value("${mu-ying-secure.num2str[7]}")
    private String[] number7;
    @Value("${mu-ying-secure.num2str[8]}")
    private String[] number8;
    @Value("${mu-ying-secure.num2str[9]}")
    private String[] number9;

    public static String getNumber(int number) {
        if (instance == null) {
            // 返回默认值或抛出异常
            switch (number) {
                case 0:
                    return "0";
                case 1:
                    return "1";
                case 2:
                    return "2";
                case 3:
                    return "3";
                case 4:
                    return "4";
                case 5:
                    return "5";
                case 6:
                    return "6";
                case 7:
                    return "7";
                case 8:
                    return "8";
                case 9:
                    return "9";
                default:
                    return "";
            }
        } else {
            switch (number) {
                case 0:
                    return StrUtil.join("", instance.number0);
                case 1:
                    return StrUtil.join("", instance.number1);
                case 2:
                    return StrUtil.join("", instance.number2);
                case 3:
                    return StrUtil.join("", instance.number3);
                case 4:
                    return StrUtil.join("", instance.number4);
                case 5:
                    return StrUtil.join("", instance.number5);
                case 6:
                    return StrUtil.join("", instance.number6);
                case 7:
                    return StrUtil.join("", instance.number7);
                case 8:
                    return StrUtil.join("", instance.number8);
                case 9:
                    return StrUtil.join("", instance.number9);
            }
        }
        return "";
    }
}