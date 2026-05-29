package top.playereg.pix_vision.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import top.playereg.pix_vision.pojo.VO.admin.AdminAuditRecordVO;

/**
 * 审核记录查询服务接口
 *
 * @author PlayerEG
 */
public interface ContentAuditRecordService {

    /**
     * 分页查询审核记录
     * <p>
     * 支持按内容类型、审核状态、关键词筛选，支持按审核时间升序/降序排列
     * </p>
     *
     * @param current        当前页码
     * @param size           每页大小
     * @param contentType    内容类型（可选，100-作品、200-评论、300-系列、400-昵称）
     * @param approvalStatus 审核状态（可选，10-通过、20-待审核、30-违规）
     * @param keyword        关键词（可选，模糊搜索审核原因）
     * @param orderBy        排序方式：newest-最新优先、oldest-最早优先
     * @return 分页审核记录列表
     * @author PlayerEG
     */
    IPage<AdminAuditRecordVO> getAuditRecordsPage(Long current, Long size,
                                                   Integer contentType, Integer approvalStatus,
                                                   String keyword, String orderBy);
}
