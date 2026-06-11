package top.playereg.pix_vision.enums;

import lombok.Getter;

/**
 * 消息主题枚举
 * <p>
 * 定义 tb_messages 表中 project 字段的取值，用于细分消息类型。
 * </p>
 *
 * <h3>消息主题分类</h3>
 * <ul>
 *   <li><b>系统通知类</b>（from_user_id = 0）：作品审核、合集审核、评论审核、系统公告、账号通知</li>
 *   <li><b>互动通知类</b>（from_user_id = 实际用户）：点赞、收藏、评论</li>
 *   <li><b>私信类</b>（from_user_id = 实际用户）：私信</li>
 * </ul>
 *
 * <h3>使用场景</h3>
 * <ol>
 *   <li>消息发送时指定消息主题</li>
 *   <li>消息查询时按主题筛选</li>
 *   <li>前端消息列表展示时区分消息来源</li>
 * </ol>
 *
 * @author PlayerEG
 * @since V4.0
 */
@Getter
public enum MessageProject {

    // ==================== 系统通知类（from_user_id = 0）====================

    /**
     * 作品审核通知
     */
    WORK_AUDIT("work_audit", "作品审核"),

    /**
     * 合集审核通知
     */
    SERIES_AUDIT("series_audit", "合集审核"),

    /**
     * 评论审核通知
     */
    COMMENT_AUDIT("comment_audit", "评论审核"),

    /**
     * 系统公告
     */
    SYSTEM_NOTICE("system_notice", "系统公告"),

    /**
     * 账号通知（封禁、冻结等）
     */
    ACCOUNT_NOTICE("account_notice", "账号通知"),

    // ==================== 互动通知类（from_user_id = 实际用户）====================

    /**
     * 点赞通知
     */
    LIKE("like", "点赞"),

    /**
     * 收藏通知
     */
    STAR("star", "收藏"),

    /**
     * 评论通知
     */
    COMMENT("comment", "评论"),

    // ==================== 私信类（from_user_id = 实际用户）====================

    /**
     * 私信
     */
    PRIVATE_MESSAGE("private_message", "私信");

    /**
     * 消息主题编码
     */
    private final String code;

    /**
     * 消息主题描述
     */
    private final String desc;

    MessageProject(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 根据编码获取枚举
     *
     * @param code 消息主题编码
     * @return 对应的枚举，如果不存在则返回 null
     */
    public static MessageProject fromCode(String code) {
        for (MessageProject project : values()) {
            if (project.getCode().equals(code)) {
                return project;
            }
        }
        return null;
    }
}
