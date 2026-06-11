package top.playereg.pix_vision.enums;

import lombok.Getter;

/**
 * 消息类型枚举
 * <p>
 * 定义 tb_messages 表中 message_type 字段的取值。
 * </p>
 *
 * <h3>消息类型说明</h3>
 * <ul>
 *   <li><b>SYSTEM</b> - 系统通知，包括系统公告、审核结果、互动通知等</li>
 *   <li><b>PRIVATE</b> - 私信，用户之间的私密消息</li>
 * </ul>
 *
 * <h3>使用场景</h3>
 * <ol>
 *   <li>消息发送时指定消息类型</li>
 *   <li>消息查询时按类型筛选</li>
 *   <li>未读消息数量按类型统计</li>
 * </ol>
 *
 * @author PlayerEG
 * @since V4.0
 */
@Getter
public enum MessageType {

    /**
     * 系统通知
     * <p>
     * 包括：系统公告、审核结果通知、互动通知（点赞、收藏、评论）
     * </p>
     */
    SYSTEM("system", "系统通知"),

    /**
     * 私信
     * <p>
     * 用户之间的私密消息
     * </p>
     */
    PRIVATE("private", "私信");

    /**
     * 消息类型编码
     */
    private final String code;

    /**
     * 消息类型描述
     */
    private final String desc;

    MessageType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 根据编码获取枚举
     *
     * @param code 消息类型编码
     * @return 对应的枚举，如果不存在则返回 null
     */
    public static MessageType fromCode(String code) {
        for (MessageType type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return null;
    }
}
