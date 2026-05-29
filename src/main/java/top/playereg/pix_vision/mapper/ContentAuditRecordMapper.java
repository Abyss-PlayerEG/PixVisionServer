package top.playereg.pix_vision.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import top.playereg.pix_vision.pojo.entity.ContentAuditRecord;

import java.util.List;

/**
 * AI 内容审核记录 Mapper 接口
 *
 * @author PlayerEG
 */
public interface ContentAuditRecordMapper extends BaseMapper<ContentAuditRecord> {

    /**
     * 插入审核记录
     *
     * @param record 审核记录对象
     * @return 影响行数
     */
    int insertRecord(@Param("record") ContentAuditRecord record);

    /**
     * 批量查询指定内容的最新审核记录
     * <p>
     * 每个 contentId 取最新一条（按 create_time DESC），用于管理端列表展示
     * </p>
     *
     * @param contentType 内容类型（100/200/300/400）
     * @param contentIds  内容 ID 列表
     * @return 每个 contentId 的最新审核记录
     */
    List<ContentAuditRecord> selectLatestByContentIds(
        @Param("contentType") Integer contentType,
        @Param("contentIds") List<Integer> contentIds
    );

    /**
     * 分页查询审核记录
     * <p>
     * 支持按内容类型、审核状态、关键词筛选，支持时间升序/降序排列
     * </p>
     *
     * @param page           分页对象
     * @param contentType    内容类型（可选，100/200/300/400）
     * @param approvalStatus 审核状态（可选，10/20/30）
     * @param keyword        关键词（可选，模糊搜索审核原因）
     * @param sortOrder      排序方向（DESC/ASC）
     * @return 分页审核记录列表
     */
    IPage<ContentAuditRecord> selectAuditRecordsPage(
        Page<ContentAuditRecord> page,
        @Param("contentType") Integer contentType,
        @Param("approvalStatus") Integer approvalStatus,
        @Param("keyword") String keyword,
        @Param("sortOrder") String sortOrder
    );
}
