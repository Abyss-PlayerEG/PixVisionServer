package top.playereg.pix_vision.pojo;

import lombok.Data;

import java.util.List;

/**
 * AI 文案审核结果
 * <p>
 * 用于接收 Python AI 审核 API 返回的审核数据
 * </p>
 *
 * @author PlayerEG
 */
@Data
public class ContentAuditResult {

    /**
     * 审核结果状态
     * <ul>
     *   <li>{@code normal} - 正常内容，可直接发布</li>
     *   <li>{@code neutral} - 中立/存疑，待人工审核</li>
     *   <li>{@code violation} - 违规内容，直接封禁</li>
     * </ul>
     */
    private String status;

    /**
     * 判断依据简述
     */
    private String reason;

    /**
     * 命中的敏感词数组
     */
    private List<String> insult_words;
}
