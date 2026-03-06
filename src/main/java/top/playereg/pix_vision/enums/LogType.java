package top.playereg.pix_vision.enums;

import lombok.Getter;

@Getter
public enum LogType {
    INFO("INFO"),
    WARN("WARN"),
    ERROR("ERROR"),
    DEBUG("DEBUG");

    private final String type;
    LogType(String type) {
        this.type = type;
    }
}
