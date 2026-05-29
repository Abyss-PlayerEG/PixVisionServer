package top.playereg.pix_vision.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 系列操作结果
 * <p>
 * 用于封装系列新增/更新操作的结果，包含操作是否成功、AI审核状态和审核原因。
 * 参照 {@link CommentAddResult} 设计。
 * </p>
 *
 * @author PlayerEG
 */
@Data
@AllArgsConstructor
public class SeriesOperationResult {

    /**
     * 是否操作成功（数据库操作成功即为 true）
     */
    private Boolean success;

    /**
     * 审核状态
     * <ul>
     *   <li>10 - 审核通过，直接发布</li>
     *   <li>20 - 待审核</li>
     *   <li>30 - 未过审（违规）</li>
     * </ul>
     */
    private Integer approvalStatus;

    /**
     * 审核原因（违规时记录 AI 审核返回的原因）
     */
    private String auditReason;
}
