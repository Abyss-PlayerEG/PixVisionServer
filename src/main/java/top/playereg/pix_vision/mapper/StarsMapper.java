package top.playereg.pix_vision.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;
import top.playereg.pix_vision.pojo.Star;
import top.playereg.pix_vision.pojo.Works;

/**
 * 收藏 Mapper 接口
 *
 * @author PlayerEG
 */
@Mapper
@Repository
public interface StarsMapper {

    /**
     * 查询用户是否已收藏某作品
     *
     * @param userId 用户 ID
     * @param workId 作品 ID
     * @return 收藏记录对象，若不存在则返回 null
     */
    Star selectByUserAndWork(@Param("userId") Integer userId, @Param("workId") Integer workId);

    /**
     * 插入收藏记录
     *
     * @param star 收藏实体对象
     * @return 影响行数
     */
    int insertStar(Star star);

    /**
     * 更新收藏记录的删除状态
     *
     * @param userId   用户 ID
     * @param workId   作品 ID
     * @param isDelete 删除状态（true - 已删除/取消收藏, false - 未删除/收藏）
     * @return 影响行数
     */
    int updateDeleteStatus(@Param("userId") Integer userId, @Param("workId") Integer workId, @Param("isDelete") Boolean isDelete);

    /**
     * 统计作品的收藏总数
     *
     * @param workId 作品 ID
     * @return 收藏数量
     */
    Integer countStarsByWorkId(@Param("workId") Integer workId);

    /**
     * 分页查询用户收藏过的作品列表（只返回审核通过的作品）
     *
     * @param page   分页对象
     * @param userId 用户 ID
     * @return 分页结果
     * @author PlayerEG
     */
    IPage<Works> selectUserStarredWorks(Page<Works> page, @Param("userId") Integer userId);
}
