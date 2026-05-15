package top.playereg.pix_vision.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import top.playereg.pix_vision.pojo.adminPojo.AdminBatchOperateCommentResult;
import top.playereg.pix_vision.pojo.commentsPojo.Comments;
import top.playereg.pix_vision.pojo.commentsPojo.VO.PrimaryComment;

import java.util.List;

/**
 * 评论服务接口
 *
 * @author PlayerEG
 */
public interface CommentService {

    /**
     * 新增评论
     *
     * @param userId          用户 ID（从 Token 中获取）
     * @param workId          作品 ID
     * @param parentCommentId 父评论 ID（可选，二级评论时必填）
     * @param commentFloor    评论层级（1 - 作品评论、2 - 二级评论）
     * @param commentText     评论内容（限制长度 125 个汉字）
     * @return 是否新增成功
     * @author PlayerEG
     */
    Boolean addComment(Integer userId, Integer workId, Integer parentCommentId,
                       Integer commentFloor, String commentText);

    /**
     * 根据作品 ID 查询评论列表
     *
     * @param workId 作品 ID
     * @return 评论列表
     * @author PlayerEG
     */
    java.util.List<Comments> getCommentsByWorkId(Integer workId);

    /**
     * 根据评论 ID 查询评论信息
     *
     * @param commentId 评论 ID
     * @return 评论对象
     * @author PlayerEG
     */
    Comments getCommentById(Integer commentId);

    /**
     * 根据作品 ID 查询评论列表（包含用户信息和嵌套回复）
     *
     * @param workId  作品 ID
     * @param orderBy 排序方式：'oldest' - 按最早发布，其他值或 null - 按最新发布
     * @return 一级评论列表（每个一级评论包含二级评论列表）
     * @author PlayerEG
     */
    java.util.List<PrimaryComment> getCommentsWithUserInfoByWorkId(Integer workId, String orderBy);

    /**
     * 批量删除评论
     *
     * @param commentIds 评论ID列表
     * @return 批量操作结果（包含总数、成功数、失败ID列表）
     */
    AdminBatchOperateCommentResult batchDeleteComments(List<Integer> commentIds);

    /**
     * 分页查询评论列表（支持多条件过滤）
     *
     * @param current        当前页码
     * @param size           每页大小
     * @param workId         作品ID（可选）
     * @param userId         用户ID（可选）
     * @param commentFloor   评论层级（可选，1-一级评论、2-二级评论）
     * @param approvalStatus 审核状态（可选，10-正常、20-待审核、30-未过审）
     * @param keyword        评论关键字（可选）
     * @return 分页结果
     * @author PlayerEG
     */
    IPage<Comments> getCommentsPage(Long current, Long size,
                                    Integer workId, Integer userId,
                                    Integer commentFloor, Integer approvalStatus,
                                    String keyword);
}
