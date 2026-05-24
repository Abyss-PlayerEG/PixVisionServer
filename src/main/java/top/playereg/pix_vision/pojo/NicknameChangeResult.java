package top.playereg.pix_vision.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 昵称修改结果
 * <p>封装昵称修改操作的结果，包含审核状态和审核原因</p>
 *
 * @author PlayerEG
 */
@Data
@AllArgsConstructor
public class NicknameChangeResult {

    /** 是否操作成功（数据库操作成功即为 true） */
    private Boolean success;

    /**
     * 审核状态
     * <ul>
     *   <li>10 - 审核通过，昵称已更新</li>
     *   <li>20 - 待审核，昵称暂未更新</li>
     *   <li>30 - 未过审（违规），昵称未更新</li>
     * </ul>
     */
    private Integer approvalStatus;

    /** 审核原因（违规时记录 AI 审核返回的原因） */
    private String auditReason;
}
