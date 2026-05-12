package top.playereg.pix_vision.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;
import top.playereg.pix_vision.pojo.commentsPojo.Comments;

/**
 * 评论数据访问层
 * <p>
 * 继承 MyBatis-Plus 的 BaseMapper，自动提供 CRUD 方法
 *
 * @author PlayerEG
 * @see Comments
 */
@Mapper
@Repository
public interface CommentsMapper extends BaseMapper<Comments> {

    /**
     * 新增评论
     *
     * @param comment 评论对象
     * @return 影响的行数
     * @author PlayerEG
     */
    int insertComment(Comments comment);

    /**
     * 根据作品 ID 查询评论列表
     *
     * @param workId 作品 ID
     * @param orderBy 排序方式：'oldest' - 按最早发布，其他值或 null - 按最新发布
     * @return 评论列表
     * @author PlayerEG
     */
    java.util.List<Comments> selectCommentsByWorkId(@Param("workId") Integer workId, @Param("orderBy") String orderBy);

    /**
     * 根据评论 ID 查询评论信息
     *
     * @param commentId 评论 ID
     * @return 评论对象
     * @author PlayerEG
     */
    Comments selectCommentById(@Param("commentId") Integer commentId);
}
