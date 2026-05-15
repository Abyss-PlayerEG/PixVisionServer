package top.playereg.pix_vision.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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
     * @param workId  作品 ID
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


    /**
     * 批量删除评论
     *
     * @param commentIds 评论ID列表
     * @param isDelete   是否删除
     * @return 批量操作结果（包含总数、成功数、失败ID列表）
     */
    boolean deleteComments(java.util.List<Integer> commentIds, Integer isDelete);

    /**
     * 查询一级评论的所有二级评论 ID
     *
     * @param parentCommentId 一级评论 ID
     * @return 二级评论 ID 列表
     * @author PlayerEG
     */
    java.util.List<Integer> selectChildCommentIds(@Param("parentCommentId") Integer parentCommentId);

    /**
     * 分页查询评论列表（支持多条件过滤）
     *
     * @param page           分页对象
     * @param workId         作品ID（可选）
     * @param userId         用户ID（可选）
     * @param commentFloor   评论层级（可选，1-一级评论、2-二级评论）
     * @param approvalStatus 审核状态（可选，10-正常、20-待审核、30-未过审）
     * @param keyword        评论关键字（可选，模糊搜索）
     * @return 分页结果
     * @author PlayerEG
     */
    IPage<Comments> selectCommentsPage(Page<Comments> page,
                                       @Param("workId") Integer workId,
                                       @Param("userId") Integer userId,
                                       @Param("commentFloor") Integer commentFloor,
                                       @Param("approvalStatus") Integer approvalStatus,
                                       @Param("keyword") String keyword);
}
