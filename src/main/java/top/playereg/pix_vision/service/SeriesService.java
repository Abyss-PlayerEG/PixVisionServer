package top.playereg.pix_vision.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import top.playereg.pix_vision.pojo.VO.admin.AdminSeriesVO;
import top.playereg.pix_vision.pojo.dto.SeriesOperationResult;
import top.playereg.pix_vision.pojo.entity.Series;

/**
 * 系列服务接口
 *
 * @author PlayerEG
 */
public interface SeriesService {

    /**
     * 新增作品系列
     *
     * @param userId      用户 ID（从 Token 中获取）
     * @param seriesTitle 系列标题
     * @param aboutText   系列描述文本
     * @return 系列操作结果，包含操作是否成功、AI审核状态和审核原因
     */
    SeriesOperationResult addSeries(Integer userId, String seriesTitle, String aboutText);

    /**
     * 根据用户 ID 分页查询所有作品系列（支持关键词搜索）
     *
     * @param userId  用户 ID
     * @param current 当前页码
     * @param size    每页数量
     * @param keyword 搜索关键词（可选，同时匹配标题和描述，标题匹配优先排序）
     * @return 分页作品系列列表
     */
    IPage<Series> getSeriesByUserId(Integer userId, Integer current, Integer size, String keyword);

    /**
     * 删除作品系列（支持保留或删除系列内作品）
     *
     * @param seriesId    系列 ID
     * @param userId      当前用户 ID（用于权限验证）
     * @param deleteWorks 是否删除系列内的作品（true=删除作品，false=将作品的 series_id 置空）
     * @return 删除结果
     */
    Boolean deleteSeries(Integer seriesId, Integer userId, Boolean deleteWorks);

    /**
     * 更新系列信息（支持部分字段修改）
     *
     * @param seriesId    系列 ID
     * @param userId      当前用户 ID（用于权限验证）
     * @param seriesTitle 系列标题（可选，最多 16 个中文字符）
     * @param aboutText   系列描述（可选，最多 24 个中文字符）
     * @return 系列操作结果，包含操作是否成功、AI审核状态和审核原因
     */
    SeriesOperationResult updateSeriesInfo(Integer seriesId, Integer userId, String seriesTitle, String aboutText);

    /**
     * 批量将作品添加到指定合集
     *
     * @param seriesId 合集 ID
     * @param workIds  作品 ID 列表
     * @param userId   当前用户 ID（用于权限验证）
     * @return 添加结果
     */
    Boolean batchAddWorksToSeries(Integer seriesId, java.util.List<Integer> workIds, Integer userId);

    /**
     * 批量将作品从指定合集中移除
     *
     * @param seriesId 合集 ID
     * @param workIds  作品 ID 列表
     * @param userId   当前用户 ID（用于权限验证）
     * @return 移除结果
     */
    Boolean batchRemoveWorksFromSeries(Integer seriesId, java.util.List<Integer> workIds, Integer userId);

    /**
     * 批量更新系列审核状态（管理员操作）
     *
     * @param seriesIds       系列 ID 列表
     * @param approvalStatus  审核状态（10-正常、20-待审核、30-未过审）
     * @param userId          操作者 ID
     * @return 批量操作结果（包含总数、成功数、失败ID列表）
     */
    top.playereg.pix_vision.pojo.admin.AdminBatchOperateWorkResult batchUpdateApprovalStatus(
        java.util.List<Integer> seriesIds,
        Integer approvalStatus,
        Integer userId
    );

    /**
     * 批量删除作品合集（管理员操作）
     *
     * @param seriesIds   系列 ID 列表
     * @param deleteWorks 是否删除系列内的作品（true=删除作品，false=将作品的 series_id 置空）
     * @param userId      操作者 ID
     * @return 批量操作结果（包含总数、成功数、失败ID列表）
     */
    top.playereg.pix_vision.pojo.admin.AdminBatchOperateWorkResult batchDeleteSeries(
        java.util.List<Integer> seriesIds,
        Boolean deleteWorks,
        Integer userId
    );

    /**
     * 批量更新系列标题和描述（管理员操作）
     *
     * @param seriesIds      系列 ID 列表
     * @param seriesTitle    系列标题（可选，最多 16 个字符）
     * @param aboutText      系列描述（可选，最多 24 个字符）
     * @param userId         操作者 ID
     * @return 批量操作结果（包含总数、成功数、失败ID列表）
     * @author blue_sky_ks
     */
    top.playereg.pix_vision.pojo.admin.AdminBatchOperateWorkResult batchUpdateSeriesInfo(
        java.util.List<Integer> seriesIds,
        String seriesTitle,
        String aboutText,
        Integer userId
    );

    /**
     * 管理员分页查询作品合集（支持多条件筛选和排序）
     *
     * @param current        当前页码
     * @param size           每页数量
     * @param keyword        搜索关键词（可选，同时匹配标题和描述，标题匹配优先排序）
     * @param approvalStatus 审核状态（可选，10-正常、20-待审核、30-未过审）
     * @param isDelete       是否删除（可选，true-已删除、false-未删除）
     * @param userId         用户 ID（可选）
     * @param orderBy        排序方式（可选，'oldest'-按最早创建，其他值-按最新创建）
     * @return 分页作品合集列表
     * @author blue_sky_ks
     */
    IPage<AdminSeriesVO> getAdminSeriesPage(
        Long current,
        Long size,
        String keyword,
        Integer approvalStatus,
        Boolean isDelete,
        Integer userId,
        String orderBy
    );
}
