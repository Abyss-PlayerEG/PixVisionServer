package top.playereg.pix_vision.service;

import top.playereg.pix_vision.pojo.ContentAuditResult;

/**
 * AI 文案审核服务接口
 * <p>
 * 调用 Python AI 审核服务对评论文本进行内容安全审核
 * </p>
 *
 * @author PlayerEG
 */
public interface ContentAuditService {

    /**
     * 对评论文本进行 AI 审核
     * <p>
     * 调用 Python 审核 API，返回审核结果。
     * 如果审核服务调用失败，返回 {@code null}，调用方应降级处理。
     * </p>
     *
     * <h3>审核结果映射</h3>
     * <ul>
     *   <li>{@code normal} - 正常内容，对应审核状态 10（直接发布）</li>
     *   <li>{@code neutral} - 中立/存疑，对应审核状态 20（待审核）</li>
     *   <li>{@code violation} - 违规内容，对应审核状态 30（未过审）</li>
     * </ul>
     *
     * @param text 待审核文本
     * @return 审核结果，调用失败时返回 {@code null}
     * @author PlayerEG
     */
    ContentAuditResult auditContent(String text);
}
