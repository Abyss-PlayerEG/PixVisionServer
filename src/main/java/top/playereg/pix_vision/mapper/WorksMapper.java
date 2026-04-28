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
     * @param username   用户名（可选，模糊查询）
     * @param nickname   昵称（可选，模糊查询）
     * @param isOriginal 是否原创（可选，精确查询）
     * @return 分页结果
     * @author PlayerEG
     */
    IPage<Works> selectHomepageWorks(
        Page<Works> page,
        @Param("workTitle") String workTitle,
        @Param("userId") Integer userId,
        @Param("username") String username,
        @Param("nickname") String nickname,
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
        @Param("seriesId") Integer seriesId,
        @Param("shouldUpdateSeries") Boolean shouldUpdateSeries,
        @Param("isOriginal") Boolean isOriginal,
        @Param("outUrl") String outUrl
    );
}
