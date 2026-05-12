package top.playereg.pix_vision.service;

import top.playereg.pix_vision.pojo.commentsPojo.Comments;
import top.playereg.pix_vision.pojo.commentsPojo.VO.PrimaryComment;

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
     * @param workId 作品 ID
     * @return 一级评论列表（每个一级评论包含二级评论列表）
     * @author PlayerEG
     */
    java.util.List<PrimaryComment> getCommentsWithUserInfoByWorkId(Integer workId);
}
