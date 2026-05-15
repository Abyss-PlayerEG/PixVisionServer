package top.playereg.pix_vision.service.Impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.playereg.pix_vision.mapper.CommentsMapper;
import top.playereg.pix_vision.mapper.UserMapper;
import top.playereg.pix_vision.pojo.adminPojo.AdminBatchOperateCommentResult;
import top.playereg.pix_vision.pojo.commentsPojo.Comments;
import top.playereg.pix_vision.pojo.commentsPojo.VO.PrimaryComment;
import top.playereg.pix_vision.pojo.commentsPojo.VO.SecondaryComment;
import top.playereg.pix_vision.pojo.userPojo.User;
import top.playereg.pix_vision.service.CommentService;
import top.playereg.pix_vision.util.PixVisionLogger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 评论服务实现类
 *
 * @author PlayerEG
 */
@Service
public class CommentServiceImpl implements CommentService {

    private static final PixVisionLogger log = PixVisionLogger.create(CommentServiceImpl.class);

    @Autowired
    private CommentsMapper commentsMapper;

    @Autowired
    private UserMapper userMapper;

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
    @Override
    public Boolean addComment(Integer userId, Integer workId, Integer parentCommentId,
                             Integer commentFloor, String commentText) {
        // 参数校验
        if (userId == null || workId == null || commentFloor == null || commentText == null) {
            log.warn("新增评论失败 - 必要参数为空");
            return false;
        }

        // 验证评论层级
        if (commentFloor != 1 && commentFloor != 2) {
            log.warn("新增评论失败 - 评论层级无效: {}", commentFloor);
            return false;
        }

        // 验证评论内容长度（限制125个汉字）
        if (commentText.length() > 125) {
            log.warn("新增评论失败 - 评论内容过长: {} 字符", commentText.length());
            return false;
        }

        // 如果评论层级为2，父评论ID不能为空
        if (commentFloor == 2 && parentCommentId == null) {
            log.warn("新增评论失败 - 二级评论必须提供父评论ID");
            return false;
        }

        // 如果评论层级为1，父评论ID应该为空
        if (commentFloor == 1 && parentCommentId != null) {
            log.warn("新增评论失败 - 一级评论不应提供父评论ID");
            return false;
        }

        // 如果是二级评论，验证父评论是否存在并获取父评论信息
        Comments parentComment = null;
        if (commentFloor == 2 && parentCommentId != null) {
            parentComment = commentsMapper.selectCommentById(parentCommentId);
            if (parentComment == null) {
                log.warn("新增评论失败 - 父评论不存在: {}", parentCommentId);
                return false;
            }

            // 验证父评论是否属于同一作品
            if (!parentComment.getWork_id().equals(workId)) {
                log.warn("新增评论失败 - 父评论不属于当前作品");
                return false;
            }
        }

        try {
            // 创建评论对象
            Comments comment = new Comments();
            comment.setUser_id(userId);
            comment.setWork_id(workId);
            comment.setParent_comment_id(parentCommentId);

            // 设置 in_comment_id（所属一级评论ID）
            if (commentFloor == 1) {
                // 一级评论的 in_comment_id 暂时设为 null，插入后再更新为自己的 ID
                comment.setIn_comment_id(null);
            } else if (commentFloor == 2 && parentComment != null) {
                // 二级评论的 in_comment_id 继承父评论的 in_comment_id
                // 如果父评论是一级评论，parentComment.getIn_comment_id() 可能为 null，此时使用 parentCommentId
                if (parentComment.getIn_comment_id() != null) {
                    comment.setIn_comment_id(parentComment.getIn_comment_id());
                } else {
                    // 父评论是一级评论且 in_comment_id 为 null，则使用父评论ID
                    comment.setIn_comment_id(parentCommentId);
                }
            }

            comment.setComment_floor(commentFloor);
            comment.setComment_text(commentText);
            comment.setApproval_status(20); // 新增评论默认为待审核状态
            comment.setIs_delete(false);
            // time字段由数据库自动设置为当前时间，无需手动设置

            // 插入评论
            int result = commentsMapper.insertComment(comment);

            if (result > 0) {
                // 如果是一级评论，插入后需要更新 in_comment_id 为自己的 ID
                if (commentFloor == 1) {
                    comment.setIn_comment_id(comment.getComment_id());
                    commentsMapper.updateById(comment);
                    log.info("一级评论新增成功并更新 in_comment_id - 用户ID: {}, 作品ID: {}, 评论ID: {}", userId, workId, comment.getComment_id());
                } else {
                    log.info("二级评论新增成功 - 用户ID: {}, 作品ID: {}, 评论ID: {}, 所属一级评论ID: {}",
                            userId, workId, comment.getComment_id(), comment.getIn_comment_id());
                }
                return true;
            } else {
                log.warn("评论新增失败 - 用户ID: {}, 作品ID: {}", userId, workId);
                return false;
            }
        } catch (Exception e) {
            log.error("评论新增异常 - 用户ID: {}, 作品ID: {}, 错误: {}", userId, workId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 根据作品 ID 查询评论列表
     *
     * @param workId 作品 ID
     * @return 评论列表
     * @author PlayerEG
     */
    @Override
    public List<Comments> getCommentsByWorkId(Integer workId) {
        if (workId == null) {
            log.warn("查询评论失败 - 作品ID为空");
            return null;
        }

        try {
            // 默认按最新发布排序（orderBy 为 null 或其他非 'oldest' 值）
            List<Comments> comments = commentsMapper.selectCommentsByWorkId(workId, null);
            log.info("查询作品评论成功 - 作品ID: {}, 评论数量: {}", workId, comments != null ? comments.size() : 0);
            return comments;
        } catch (Exception e) {
            log.error("查询作品评论异常 - 作品ID: {}, 错误: {}", workId, e.getMessage(), e);
            return null;
        }
    }

    /**
     * 根据评论 ID 查询评论信息
     *
     * @param commentId 评论 ID
     * @return 评论对象
     * @author PlayerEG
     */
    @Override
    public Comments getCommentById(Integer commentId) {
        if (commentId == null) {
            log.warn("查询评论失败 - 评论ID为空");
            return null;
        }

        try {
            Comments comment = commentsMapper.selectCommentById(commentId);
            if (comment != null) {
                log.info("查询评论成功 - 评论ID: {}", commentId);
            } else {
                log.warn("评论不存在 - 评论ID: {}", commentId);
            }
            return comment;
        } catch (Exception e) {
            log.error("查询评论异常 - 评论ID: {}, 错误: {}", commentId, e.getMessage(), e);
            return null;
        }
    }

    /**
     * 根据作品 ID 查询评论列表（包含用户信息和嵌套回复）
     *
     * @param workId 作品 ID
     * @param orderBy 排序方式：'oldest' - 按最早发布，其他值或 null - 按最新发布
     * @return 一级评论列表（每个一级评论包含二级评论列表）
     * @author PlayerEG
     */
    @Override
    public List<PrimaryComment> getCommentsWithUserInfoByWorkId(Integer workId, String orderBy) {
        if (workId == null) {
            log.warn("查询评论失败 - 作品ID为空");
            return null;
        }

        try {
            // 查询所有评论
            List<Comments> allComments = commentsMapper.selectCommentsByWorkId(workId, orderBy);
            if (allComments == null || allComments.isEmpty()) {
                log.info("该作品暂无评论，作品 ID: {}", workId);
                return new ArrayList<>();
            }

            // 获取所有用户 ID
            List<Integer> userIds = allComments.stream()
                    .map(Comments::getUser_id)
                    .distinct()
                    .collect(Collectors.toList());

            // 批量查询用户信息
            Map<Integer, User> userMap = new HashMap<>();
            for (Integer userId : userIds) {
                User user = userMapper.selectAllUserInfoById(userId);
                if (user != null) {
                    userMap.put(userId, user);
                }
            }

            // 转换评论为响应对象
            Map<Integer, PrimaryComment> primaryCommentMap = new HashMap<>();
            Map<Integer, Comments> parentCommentMap = new HashMap<>();
            List<PrimaryComment> rootComments = new ArrayList<>();

            // 先处理一级评论
            for (Comments comment : allComments) {
                if (comment.getComment_floor() == 1) {
                    PrimaryComment primaryComment = convertToPrimaryComment(comment, userMap);
                    primaryComment.setChildren(new ArrayList<>());
                    primaryCommentMap.put(comment.getComment_id(), primaryComment);
                    parentCommentMap.put(comment.getComment_id(), comment);
                    rootComments.add(primaryComment);
                } else {
                    // 将所有评论加入父评论映射表，方便二级评论查找被回复者
                    parentCommentMap.put(comment.getComment_id(), comment);
                }
            }

            // 再处理二级评论，添加到对应一级评论的 children 列表中
            for (Comments comment : allComments) {
                if (comment.getComment_floor() >= 2 && comment.getIn_comment_id() != null) {
                    SecondaryComment secondaryComment = convertToSecondaryComment(comment, userMap, parentCommentMap);
                    PrimaryComment primaryComment = primaryCommentMap.get(comment.getIn_comment_id());
                    if (secondaryComment != null && primaryComment != null) {
                        primaryComment.getChildren().add(secondaryComment);
                    }
                }
            }

            // 对每个一级评论的二级评论列表按最早发布排序（comment_id ASC）
            for (PrimaryComment primaryComment : rootComments) {
                if (primaryComment.getChildren() != null && !primaryComment.getChildren().isEmpty()) {
                    primaryComment.getChildren().sort((c1, c2) ->
                        Integer.compare(c1.getComment_id(), c2.getComment_id())
                    );
                }
            }

            log.info("查询作品评论成功 - 作品ID: {}, 一级评论数量: {}, 总评论数量: {}",
                    workId, rootComments.size(), allComments.size());
            return rootComments;
        } catch (Exception e) {
            log.error("查询作品评论异常 - 作品ID: {}, 错误: {}", workId, e.getMessage(), e);
            return null;
        }
    }

    /**
     * 将 Comments 实体转换为 PrimaryComment（一级评论）
     *
     * @param comment 评论实体
     * @param userMap 用户信息映射表
     * @return 一级评论响应对象
     */
    private PrimaryComment convertToPrimaryComment(Comments comment, Map<Integer, User> userMap) {
        PrimaryComment vo = new PrimaryComment();
        vo.setComment_id(comment.getComment_id());
        vo.setUser_id(comment.getUser_id());
        vo.setWork_id(comment.getWork_id());
        vo.setParent_comment_id(comment.getParent_comment_id());
        vo.setIn_comment_id(comment.getIn_comment_id());
        vo.setComment_floor(comment.getComment_floor());
        vo.setComment_text(comment.getComment_text());
        vo.setIs_delete(comment.getIs_delete());
        vo.setTime(comment.getTime());

        // 设置用户信息
        User user = userMap.get(comment.getUser_id());
        if (user != null) {
            vo.setNickname(user.getNickname());
            vo.setUser_avatar(user.getAvatar_url());
        } else {
            vo.setNickname("未知用户");
            vo.setUser_avatar("");
        }

        return vo;
    }

    /**
     * 将 Comments 实体转换为 SecondaryComment（二级评论）
     *
     * @param comment 评论实体
     * @param userMap 用户信息映射表
     * @param parentCommentMap 父评论映射表（用于获取被回复者信息）
     * @return 二级评论响应对象
     */
    private SecondaryComment convertToSecondaryComment(Comments comment, Map<Integer, User> userMap,
                                                       Map<Integer, Comments> parentCommentMap) {
        SecondaryComment vo = new SecondaryComment();
        vo.setComment_id(comment.getComment_id());
        vo.setUser_id(comment.getUser_id());
        vo.setWork_id(comment.getWork_id());
        vo.setParent_comment_id(comment.getParent_comment_id());
        vo.setIn_comment_id(comment.getIn_comment_id());
        vo.setComment_floor(comment.getComment_floor());
        vo.setComment_text(comment.getComment_text());
        vo.setIs_delete(comment.getIs_delete());
        vo.setTime(comment.getTime());

        // 设置用户信息
        User user = userMap.get(comment.getUser_id());
        if (user != null) {
            vo.setNickname(user.getNickname());
            vo.setUser_avatar(user.getAvatar_url());
        } else {
            vo.setNickname("未知用户");
            vo.setUser_avatar("");
        }

        // 设置被回复者的昵称和 ID（二级评论特有）
        if (comment.getParent_comment_id() != null) {
            Comments parentComment = parentCommentMap.get(comment.getParent_comment_id());
            if (parentComment != null) {
                User parentUser = userMap.get(parentComment.getUser_id());
                if (parentUser != null) {
                    vo.setReplied_nickname(parentUser.getNickname());
                    vo.setReplied_user_id(parentUser.getUser_id());
                } else {
                    vo.setReplied_nickname("未知用户");
                    vo.setReplied_user_id(null);
                }
            }
        }

        return vo;
    }

    /**
     * 批量删除评论
     * @param commentIds 评论ID列表
     * @return 批量操作结果（包含总数、成功数、失败ID列表）
     * */
    public AdminBatchOperateCommentResult batchDeleteComments(List<Integer> commentIds){

        if (commentIds == null || commentIds.isEmpty()) {
            return new AdminBatchOperateCommentResult(0, 0, new ArrayList<>());
        }

        // 收集所有一级评论的二级评论 ID
        List<Integer> allCommentIds = new ArrayList<>(commentIds);
        for (Integer commentId : commentIds) {
            try {
                // 查询该一级评论的所有二级评论
                List<Integer> childCommentIds = commentsMapper.selectChildCommentIds(commentId);
                if (childCommentIds != null && !childCommentIds.isEmpty()) {
                    allCommentIds.addAll(childCommentIds);
                    log.info("一级评论 {} 有 {} 个二级评论，将一起删除", commentId, childCommentIds.size());
                }
            } catch (Exception e) {
                log.warn("查询一级评论 {} 的二级评论失败: {}", commentId, e.getMessage());
            }
        }

        int totalCount = allCommentIds.size();
        List<Integer> failedWorkIds = new ArrayList<>();
        int successCount = 0;

        // 批量删除所有一级和二级评论
        for (Integer commentId : allCommentIds) {
            try {
                boolean result = commentsMapper.deleteComments(
                    java.util.Collections.singletonList(commentId),
                    1
                );

                if (result) {
                    successCount++;
                } else {
                    failedWorkIds.add(commentId);
                }
            } catch (Exception e) {
                // 如果更新异常，也视为失败
                failedWorkIds.add(commentId);
            }
        }

        log.info("批量删除评论完成 - 总数: {}, 成功: {}, 失败: {}", totalCount, successCount, failedWorkIds.size());
        return new AdminBatchOperateCommentResult(totalCount, successCount, failedWorkIds);
    }

    /**
     * 分页查询评论列表（支持多条件过滤）
     *
     * @param current 当前页码
     * @param size 每页大小
     * @param workId 作品ID（可选）
     * @param userId 用户ID（可选）
     * @param commentFloor 评论层级（可选，1-一级评论、2-二级评论）
     * @param approvalStatus 审核状态（可选，10-正常、20-待审核、30-未过审）
     * @param keyword 评论关键字（可选）
     * @return 分页结果
     * @author PlayerEG
     */
    @Override
    public IPage<Comments> getCommentsPage(Long current, Long size,
                                            Integer workId, Integer userId,
                                            Integer commentFloor, Integer approvalStatus,
                                            String keyword) {
        // 参数校验
        if (current == null || current < 1) {
            current = 1L;
        }
        if (size == null || size < 1 || size > 100) {
            size = 10L;
        }

        log.info("开始分页查询评论，页码: {}, 每页大小: {}, 作品ID: {}, 用户ID: {}, 评论层级: {}, 审核状态: {}, 关键字: {}",
                current, size, workId, userId, commentFloor, approvalStatus, keyword);

        // 创建分页对象
        Page<Comments> page = new Page<>(current, size);

        // 调用 Mapper 层查询
        IPage<Comments> result = commentsMapper.selectCommentsPage(page, workId, userId, commentFloor, approvalStatus, keyword);

        log.info("分页查询评论完成，总数: {}, 当前页: {}, 每页大小: {}",
                result.getTotal(), result.getCurrent(), result.getSize());

        return result;
    }
}
