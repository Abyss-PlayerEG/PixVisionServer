
package top.playereg.pix_vision.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;
import top.playereg.pix_vision.pojo.Like;

/**
 * 点赞 Mapper 接口
 *
 * @author PlayerEG
 */
@Mapper
@Repository
public interface LikesMapper {

    /**
     * 查询用户是否已点赞某作品
     *
     * @param userId 用户 ID
     * @param workId 作品 ID
     * @return 点赞记录对象，若不存在则返回 null
     */
    Like selectByUserAndWork(@Param("userId") Integer userId, @Param("workId") Integer workId);

    /**
     * 插入点赞记录
     *
     * @param like 点赞实体对象
     * @return 影响行数
     */
    int insertLike(Like like);

    /**
     * 更新点赞记录的删除状态
     *
     * @param userId   用户 ID
     * @param workId   作品 ID
     * @param isDelete 删除状态（true - 已删除/取消点赞, false - 未删除/点赞）
     * @return 影响行数
     */
    int updateDeleteStatus(@Param("userId") Integer userId, @Param("workId") Integer workId, @Param("isDelete") Boolean isDelete);

    /**
     * 统计作品的点赞总数
     *
     * @param workId 作品 ID
     * @return 点赞数量
     */
    Integer countLikesByWorkId(@Param("workId") Integer workId);
}
