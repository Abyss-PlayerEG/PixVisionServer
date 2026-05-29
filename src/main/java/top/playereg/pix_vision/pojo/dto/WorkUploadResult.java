package top.playereg.pix_vision.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 作品上传结果
 * <p>
 * 封装作品上传操作的完整结果，包含作品 ID、审核状态和审核原因，
 * 供 Controller 层根据审核状态返回差异化的响应消息
 * </p>
 *
 * @author PlayerEG
 */
@Data
@AllArgsConstructor
public class WorkUploadResult {

    /**
     * 新创建的作品 ID
     */
    private Integer work_id;

    /**
     * 审核状态
     * <ul>
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
