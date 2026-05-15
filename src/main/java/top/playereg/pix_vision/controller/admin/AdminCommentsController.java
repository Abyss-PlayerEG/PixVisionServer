package top.playereg.pix_vision.controller.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.playereg.pix_vision.pojo.ResponsePojo;
import top.playereg.pix_vision.pojo.adminPojo.AdminBatchOperateCommentResult;
import top.playereg.pix_vision.pojo.adminPojo.AdminBatchOperateWorkResult;
import top.playereg.pix_vision.service.CommentService;
import top.playereg.pix_vision.service.PendingReviewsService;
import top.playereg.pix_vision.util.Annotation.RequireRole;
import top.playereg.pix_vision.util.PixVisionLogger;

import java.util.List;

@RestController
@RequestMapping("/api/admin/comments")
@RequiredArgsConstructor
@Tag(name = "系统管理员接口 - 评论管理")
@RequireRole(value = {77})
public class AdminCommentsController {
    private static final PixVisionLogger log = PixVisionLogger.create(AdminWorksController.class);

    private final CommentService commentService;

    /**
     * 批量删除评论 - 管理员
     *
     * @param commentIds ID 列表
     * @return 批量操作结果（包含总数、成功数、失败ID列表）
     * @author blue_sky_ks
     */
    @Operation(
        summary = "删除评论接口 - 管理员",
        description = ""
    )
    @PostMapping("/delete")
    public ResponsePojo<AdminBatchOperateCommentResult> adminBannedWorks(
        @Parameter(description = "目标评论 ID 列表", required = true, example = "1,2,3") @RequestParam List<Integer> commentIds
    ){
        // 参数校验
        if (commentIds == null || commentIds.isEmpty()) {
            log.warn("评论 ID 列表为空");
            return ResponsePojo.error(null, "评论 ID 列表不能为空");
        }

        try {
            // 调用服务层批量评论删除
            AdminBatchOperateCommentResult result = commentService.batchDeleteComments(commentIds);

            if (result.getSuccessCount() > 0) {
                log.info("批量删除评论完成 - 总数: {}, 成功: {}, 失败: {}",
                    result.getTotalCount(), result.getSuccessCount(), result.getFailedWorkIds().size());
                return ResponsePojo.success(result, "批量删除评论处理完成");
            } else {
                log.warn("批量删除评论全部失败，作品 ID 列表: {}", commentIds);
                return ResponsePojo.error(result, "删除失败，请检查评论 ID 是否正确");
            }
        } catch (Exception e) {
            log.error("批量删除评论异常，作品 ID 列表: {}, 错误: {}", commentIds, e.getMessage(), e);
            return ResponsePojo.error(null, "删除失败：" + e.getMessage());
        }
    }

}
