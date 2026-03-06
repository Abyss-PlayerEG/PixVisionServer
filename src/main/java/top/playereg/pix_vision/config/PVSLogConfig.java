package top.playereg.pix_vision.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Configuration
@Component
@SuppressWarnings("all")
public class PVSLogConfig {
    @Value("${mu-ying-logging.open-highlight-color}")
    private boolean highlightColor;
    @Value("${mu-ying-logging.enable}")
    private boolean enable;
    @Value("${mu-ying-logging.level}")
    private String level;

    private static PVSLogConfig instance;
    
    public PVSLogConfig() {
        instance = this;
    }

    public static boolean getOpenHighlightColor() {
        return instance != null ? instance.highlightColor : true;
    }
    
    public static boolean getIsOpen() {
        return instance != null ? instance.enable : true; // 默认启用
    }
    
    public static String getLogLevel() {
        return instance != null ? instance.level : "info"; // 默认级别
    }
}
