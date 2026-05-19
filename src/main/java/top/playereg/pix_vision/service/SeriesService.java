package top.playereg.pix_vision.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import top.playereg.pix_vision.pojo.Series;

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
     * @return 新增的系列对象
     */
    Series addSeries(Integer userId, String seriesTitle, String aboutText);

    /**
     * 根据用户 ID 分页查询所有作品系列
     *
     * @param userId  用户 ID
     * @param current 当前页码
     * @param size    每页数量
     * @return 分页作品系列列表
     */
    IPage<Series> getSeriesByUserId(Integer userId, Integer current, Integer size);

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
     * @return 修改结果
     */
    Boolean updateSeriesInfo(Integer seriesId, Integer userId, String seriesTitle, String aboutText);

    /**
     * 批量更新系列审核状态（管理员操作）
     *
     * @param seriesIds       系列 ID 列表
     * @param approvalStatus  审核状态（10-正常、20-待审核、30-未过审）
     * @param userId          操作者 ID
     * @return 批量操作结果（包含总数、成功数、失败ID列表）
     */
    top.playereg.pix_vision.pojo.adminPojo.AdminBatchOperateWorkResult batchUpdateApprovalStatus(
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
    top.playereg.pix_vision.pojo.adminPojo.AdminBatchOperateWorkResult batchDeleteSeries(
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
    top.playereg.pix_vision.pojo.adminPojo.AdminBatchOperateWorkResult batchUpdateSeriesInfo(
        java.util.List<Integer> seriesIds,
        String seriesTitle,
        String aboutText,
        Integer userId
    );
}
