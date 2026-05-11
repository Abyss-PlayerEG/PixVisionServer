package top.playereg.pix_vision.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import top.playereg.pix_vision.pojo.Series;

/**
 * 系列数据访问层
 * <p>
 * 继承 MyBatis-Plus 的 BaseMapper，自动提供 CRUD 方法
 *
 * @author PlayerEG
 * @see Series
 */
@Mapper
public interface SeriesMapper extends BaseMapper<Series> {

    /**
     * 新增作品系列
     *
     * @param series 系列对象（包含用户 ID、标题、描述等）
     * @return 影响的行数
     */
    int insertSeries(Series series);

    /**
     * 根据用户 ID 分页查询所有作品系列（排除逻辑删除）
     *
     * @param page   分页对象
     * @param userId 用户 ID
     * @return 分页作品系列列表
     */
    IPage<Series> selectSeriesByUserId(IPage<Series> page, @Param("userId") Integer userId);

    /**
     * 根据系列 ID 查询系列信息
     *
     * @param seriesId 系列 ID
     * @return 系列对象
     */
    Series selectSeriesById(@Param("seriesId") Integer seriesId);

    /**
     * 检查用户的系列标题是否已存在（排除逻辑删除）
     *
     * @param userId      用户 ID
     * @param seriesTitle 系列标题
     * @return 存在的系列数量
     */
    int countSeriesByTitle(@Param("userId") Integer userId, @Param("seriesTitle") String seriesTitle);

    /**
     * 逻辑删除系列（SQL 层面验证用户权限）
     *
     * @param seriesId 系列 ID
     * @param userId   用户 ID（确保只能删除自己的系列）
     * @return 影响的行数
     */
    int deleteSeriesById(@Param("seriesId") Integer seriesId, @Param("userId") Integer userId);

    /**
     * 更新系列信息（SQL 层面验证用户权限，动态更新非空字段）
     *
     * @param seriesId    系列 ID
     * @param userId      用户 ID（确保只能修改自己的系列）
     * @param seriesTitle 系列标题（可为 null）
     * @param aboutText   系列描述（可为 null）
     * @return 影响的行数
     */
    int updateSeriesInfo(
        @Param("seriesId") Integer seriesId,
        @Param("userId") Integer userId,
        @Param("seriesTitle") String seriesTitle,
        @Param("aboutText") String aboutText
    );
}
