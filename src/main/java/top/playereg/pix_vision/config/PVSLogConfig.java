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

    private static PVSLogConfig instance;
    
    public PVSLogConfig() {
        instance = this;
    }

    public static boolean getOpenHighlightColor() {
        return instance != null ? instance.highlightColor : true;
    }
}
