package top.playereg.pix_vision.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 评论新增结果
 * <p>
 * 封装评论新增操作的完整结果，包含是否成功、审核状态和审核原因，
 * 供 Controller 层根据审核状态返回差异化的响应消息
 * </p>
 *
 * @author PlayerEG
 */
@Data
@AllArgsConstructor
public class CommentAddResult {

    /**
     * 是否新增成功（数据库插入成功即为 true）
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
