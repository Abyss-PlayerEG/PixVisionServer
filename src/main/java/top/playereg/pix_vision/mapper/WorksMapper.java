package top.playereg.pix_vision.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;
import top.playereg.pix_vision.pojo.Works;

/**
 * 作品数据访问层
 * <p>
 * 继承 MyBatis-Plus 的 BaseMapper，自动提供 CRUD 方法
 *
 * @author PlayerEG
 * @see Works
 */
@Mapper
@Repository
public interface WorksMapper extends BaseMapper<Works> {

    /**
     * 分页查询首页作品列表（支持多条件查询）
     *
     * @param page       分页对象
     * @param workTitle  作品标题（可选，模糊查询）
     * @param userId     用户 ID（可选，精确查询）
     * @param seriesId   系列 ID（可选，精确查询）
     * @param isOriginal 是否原创（可选，精确查询）
     * @return 分页结果
     * @author PlayerEG
     */
    IPage<Works> selectHomepageWorks(
        Page<Works> page,
        @Param("workTitle") String workTitle,
        @Param("userId") Integer userId,
        @Param("seriesId") Integer seriesId,
        @Param("isOriginal") Boolean isOriginal
    );

    /**
     * 批量逻辑删除作品（SQL 层面验证用户权限）
     *
     * @param workIds 作品 ID 列表
     * @param userId  用户 ID（确保只能删除自己的作品）
     * @return 影响的行数
     * @author PlayerEG
     */
    int batchDeleteWorks(java.util.List<Integer> workIds, Integer userId);

    /**
     * 将指定系列的所有作品 series_id 置空（SQL 层面验证用户权限）
     *
     * @param seriesId 系列 ID
     * @param userId   用户 ID（确保只能操作自己的作品）
     * @return 影响的行数
     */
    int clearSeriesIdBySeriesId(@Param("seriesId") Integer seriesId, @Param("userId") Integer userId);

    /**
     * 根据系列 ID 查询所有作品 ID（用于删除系列内作品）
     *
     * @param seriesId 系列 ID
     * @param userId   用户 ID（确保只能查询自己的作品）
     * @return 作品 ID 列表
     */
    java.util.List<Integer> selectWorkIdsBySeriesId(@Param("seriesId") Integer seriesId, @Param("userId") Integer userId);

    /**
     * 根据作品 ID 查询作品信息
     *
     * @param workId 作品 ID
     * @return 作品对象
     */
    Works selectWorkById(@Param("workId") Integer workId);

    /**
     * 更新作品信息（SQL 层面验证用户权限）
     *
     * @param workId             作品 ID
     * @param userId             用户 ID（确保只能修改自己的作品）
     * @param workTitle          作品标题（可为 null）
     * @param imgUrl             图片 URL（可为 null）
     * @param thumbUrl           封面缩略图文件名（可为 null）
     * @param seriesId           系列 ID（可为 null，null 表示不更新或设置为 NULL）
     * @param shouldUpdateSeries 是否应该更新 series_id（true=更新，false=不更新）
     * @param isOriginal         是否原创（可为 null）
     * @param outUrl             外部转载链接（可为 null）
     * @return 影响的行数
     */
    int updateWorkInfo(
        @Param("workId") Integer workId,
        @Param("userId") Integer userId,
        @Param("workTitle") String workTitle,
        @Param("imgUrl") String imgUrl,
        @Param("thumbUrl") String thumbUrl,
        @Param("seriesId") Integer seriesId,
        @Param("shouldUpdateSeries") Boolean shouldUpdateSeries,
        @Param("isOriginal") Boolean isOriginal,
        @Param("outUrl") String outUrl
    );

    /**
     * 增加作品浏览次数
     *
     * @param workId 作品 ID
     * @return 影响的行数
     * @author PlayerEG
     */
    int incrementViewCount(@Param("workId") Integer workId);

    /**
     * 管理员批量逻辑删除作品（不验证用户权限）
     *
     * @param workIds 作品 ID 列表
     * @return 影响的行数
     * @author PlayerEG
     */
    int adminBatchDeleteWorks(java.util.List<Integer> workIds);

    /**
     * 管理员批量更新作品审核状态（不验证用户权限）
     *
     * @param workIds        作品 ID 列表
     * @param approvalStatus 审核状态：10 - 正常、20 - 待审核、30 - 未过审
     * @param userId         操作者 ID
     * @return 影响的行数
     * @author PlayerEG
     */
    int adminBatchUpdateApprovalStatus(
        @Param("workIds") java.util.List<Integer> workIds,
        @Param("approvalStatus") Integer approvalStatus,
        @Param("userId") Integer userId
    );

    /**
     * 分页查询用户自己的作品列表（只过滤已删除，不过滤审核状态）
     *
     * @param page           分页对象
     * @param userId         用户 ID
     * @param approvalStatus 审核状态（可选，10-正常、20-待审核、30-未过审）
     * @return 分页结果
     * @author PlayerEG
     */
    IPage<Works> selectMyWorks(
        Page<Works> page,
        @Param("userId") Integer userId,
        @Param("approvalStatus") Integer approvalStatus
    );

    /**
     * 原子更新作品点赞数
     *
     * @param workId 作品 ID
     * @param delta  变化量（+1 或 -1）
     * @return 影响的行数
     * @author PlayerEG
     */
    int updateLikeCount(@Param("workId") Integer workId, @Param("delta") int delta);

    /**
     * 原子更新作品收藏数
     *
     * @param workId 作品 ID
     * @param delta  变化量（+1 或 -1）
     * @return 影响的行数
     * @author PlayerEG
     */
    int updateStarCount(@Param("workId") Integer workId, @Param("delta") int delta);

    /**
     * 批量更新作品的浏览量计数（用于启动预热同步）
     *
     * @param viewData 包含 work_id 和 count 的列表
     * @author PlayerEG
     */
    void batchUpdateViewCounts(java.util.List<java.util.Map<String, Object>> viewData);

    /**
     * 统计所有作品的总浏览记录数（登录用户 + 游客，用于启动预热）
     *
     * @return 作品 ID 与对应总浏览数的列表
     * @author PlayerEG
     */
    java.util.List<java.util.Map<String, Object>> selectAllWorkTotalViewCounts();

    /**
     * 统计单个作品的总浏览记录数（登录用户 + 游客，用于缓存回源）
     *
     * @param workId 作品 ID
     * @return 总浏览数
     * @author PlayerEG
     */
    int selectTotalViewCountByWorkId(@Param("workId") Integer workId);

    /**
     * 管理员分页查询作品列表（支持多条件过滤）
     *
     * @param page    分页对象
     * @param keyword 关键字（可选，模糊搜索标题）
     * @param orderBy 排序方式：'oldest' - 按最早发布，其他值或 null - 按最新发布（默认）
     * @return 分页结果
     * @author PlayerEG
     */
    IPage<Works> adminSelectWorks(
        Page<Works> page,
        @Param("keyword") String keyword,
        @Param("orderBy") String orderBy
    );

    /**
     * 查询用户统计数据（作品数、点赞总数、收藏总数、查看总数）
     *
     * @param userId 用户 ID
     * @return 包含 work_count, total_likes, total_stars, total_views 的 Map
     * @author PlayerEG
     */
    java.util.Map<String, Object> selectUserStats(@Param("userId") Integer userId);

    /**
     * 管理员批量更新作品标题（不验证用户权限）
     *
     * @param workIds   作品 ID 列表
     * @param workTitle 作品标题
     * @param userId    操作者 ID
     * @return 影响的行数
     * @author blue_sky_ks
     */
    int adminBatchUpdateWorkTitle(
        @Param("workIds") java.util.List<Integer> workIds,
        @Param("workTitle") String workTitle,
        @Param("userId") Integer userId
    );

    /**
     * 查询最后一个公开作品的 work_id（仅统计未删除且审核通过的作品）
     * <p>
     * 使用 MAX(work_id) 直接走主键索引，性能极佳。
     * 仅过滤 is_delete=0 且 approval_status=10 的可见作品。
     *
     * @return 最大 work_id，如果不存在符合条件的作品则返回 null
     * @author PlayerEG
     */
    Integer selectLastWorkId();

    /**
     * 随机获取一个可见作品（未删除且审核通过）
     * <p>
     * 使用 ORDER BY RAND() LIMIT 1 实现完全随机。
     * 仅返回 work_id，无可见作品时返回 null。
     *
     * @return 随机作品 ID，无作品时返回 null
     * @author PlayerEG
     */
    Integer selectRandomWorkId();
}
