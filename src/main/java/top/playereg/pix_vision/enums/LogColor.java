package top.playereg.pix_vision.enums;

import lombok.Getter;
import top.playereg.pix_vision.config.PVSLogConfig;

@Getter
public enum LogColor {
    RESET("\u001B[0m"),         // 重置颜色
    BLACK("\u001B[30m"),        // 黑色
    RED("\u001B[31m"),          // 红色
    GREEN("\u001B[32m"),        // 绿色
    YELLOW("\u001B[33m"),       // 黄色
    BLUE("\u001B[34m"),         // 蓝色
    PURPLE("\u001B[35m"),       // 品红
    CYAN("\u001B[36m"),         // 青色
    WHITE("\u001B[37m");        // 白色

    private final String code;

    LogColor(String code) {
        this.code = code;
    }

    public static String colorize(String text, LogColor color) {
        if (PVSLogConfig.getOpenHighlightColor()) {
            return color.getCode() + text + RESET.getCode();
        } else {
            return text;
        }
    }
}
