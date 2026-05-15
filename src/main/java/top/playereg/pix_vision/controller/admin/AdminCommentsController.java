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
import top.playereg.pix_vision.service.CommentService;
import top.playereg.pix_vision.util.Annotation.RequireRole;
import top.playereg.pix_vision.util.PixVisionLogger;

import java.util.List;

@RestController
@RequestMapping("/api/admin/comments")
@RequiredArgsConstructor
@Tag(name = "系统管理员接口 - 评论管理")
@RequireRole(value = {77})
public class AdminCommentsController {
    private static final PixVisionLogger log = PixVisionLogger.create(AdminCommentsController.class);

    private final CommentService commentService;

    /**
     * 批量删除评论 - 管理员
     *
     * @param commentIds 目标评论 ID 列表
     * @return 批量操作结果（包含总数、成功数、失败ID列表）
     * @author blue_sky_ks
     */
    @Operation(
        summary = "删除评论接口 - 管理员",
        description = """
            # 批量删除评论（需要系统管理员权限）

            ## 特性
            - 需要系统管理员角色（role=77）才能访问
            - 支持批量删除多个评论
            - 返回详细的操作结果统计信息
            - 包含成功和失败的评论 ID 列表

            ## 参数说明：
            - commentIds: **评论 ID 列表**，List<Integer> 类型，请求参数，必填，不能为空

            ## 返回说明：
            - **删除成功**：返回 `AdminBatchOperateCommentResult` 对象，包含总数、成功数、失败ID列表等信息
            - **全部失败**：返回错误提示 "删除失败，请检查评论 ID 是否正确"
            - **参数错误**：返回错误提示 "评论 ID 列表不能为空"
            - **异常处理**：捕获并返回具体的异常信息

            ## 业务逻辑：
            1. 校验评论 ID 列表参数的有效性（非空）
            2. 调用服务层执行批量删除操作
            3. 记录删除操作的日志信息
            4. 根据删除结果返回相应的响应信息
            5. 对异常情况做统一处理

            ## 注意事项：
            - 这是一个**受保护接口**，只有系统管理员（role=77）可以访问
            - 删除操作是高级操作，一般被删除的评论，不允许恢复
            - 建议在执行前确认评论 ID 的正确性
            - 返回的结果中包含详细的成功/失败统计信息
            """
    )
    @PostMapping("/delete")
    public ResponsePojo<AdminBatchOperateCommentResult> adminDeleteComments(
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
                log.warn("批量删除评论全部失败，评论 ID 列表: {}", commentIds);
                return ResponsePojo.error(result, "删除失败，请检查评论 ID 是否正确");
            }
        } catch (Exception e) {
            log.error("批量删除评论异常，评论 ID 列表: {}, 错误: {}", commentIds, e.getMessage(), e);
            return ResponsePojo.error(null, "删除失败：" + e.getMessage());
        }
    }

}
