package top.playereg.pix_vision.controller.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import top.playereg.pix_vision.pojo.ResponsePojo;
import top.playereg.pix_vision.pojo.adminPojo.AdminBatchOperateCommentResult;
import top.playereg.pix_vision.pojo.commentsPojo.Comments;
import top.playereg.pix_vision.service.CommentService;
import top.playereg.pix_vision.util.Annotation.RequireRole;
import top.playereg.pix_vision.util.PageUtils;
import top.playereg.pix_vision.util.PixVisionLogger;

import java.util.List;

@RestController
@RequestMapping("/api/admin/comments")
@RequiredArgsConstructor
@Tag(name = "系统管理员接口 - 评论管理", description = "提供评论管理的后台接口，包括批量删除等操作")
@RequireRole(value = {55, 77})
public class AdminCommentsController {
    private static final PixVisionLogger log = PixVisionLogger.create(AdminCommentsController.class);

    private final CommentService commentService;

    /**
     * 批量删除评论 - 管理员
     *
     * @param commentIds 目标评论 ID 列表
     * @return 批量操作结果（包含总数、成功数、失败ID列表）
     * @author blue_sky_ks, PlayerEG
     */
    @Operation(
        summary = "批量删除评论",
        description = """
            # 批量删除评论（需要登录认证 + 角色权限[77]）

            ## 特性
            - 需要系统管理员角色（role=77）才能访问
            - 支持批量删除多个评论
            - 删除一级评论时自动级联删除其所有二级评论
            - 返回详细的操作结果统计信息

            ## 参数说明：
            - **commentIds**: **评论 ID 列表**，List<Integer> 类型，请求参数，必填，不能为空

            ## 返回说明：
            - **成功**：返回包含总数、成功数、失败 ID 列表的统计信息
            - **全部失败**：返回错误提示
            - **参数错误**：返回错误提示

            ## 业务逻辑：
            1. 校验评论 ID 列表参数的有效性（非空）
            2. 遍历每个评论 ID，如果是一级评论则查询其所有二级评论
            3. 将所有二级评论 ID 加入删除列表
            4. 批量删除所有一级和二级评论
            5. 记录删除操作的日志信息
            6. 根据删除结果返回相应的响应信息

            ## 注意事项：
            - 这是一个**受保护接口**，只有系统管理员（role=77）可以访问
            - 删除操作会级联删除一级评论的所有二级评论
            - 删除操作是逻辑删除，数据不会从数据库中物理移除
            - 建议在执行前确认评论 ID 的正确性
            """
    )
    @PostMapping("/delete")
    public ResponsePojo<AdminBatchOperateCommentResult> adminDeleteComments(
        @Parameter(description = "目标评论 ID 列表", required = true, example = "1,2,3") @RequestParam List<Integer> commentIds
    ) {
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

    /**
     * 分页查询评论列表 - 管理员
     *
     * @param current      当前页码
     * @param size         每页大小
     * @param workId       作品ID（可选）
     * @param userId       用户ID（可选）
     * @param commentFloor 评论层级（可选，1-一级评论、2-二级评论）
     * @param keyword      评论关键字（可选）
     * @return 分页结果
     * @author PlayerEG
     */
    @Operation(
        summary = "分页查询评论列表",
        description = """
            # 分页查询评论列表（需要登录认证 + 角色权限[55, 77]）

            ## 特性
            - 需要审核员或系统管理员角色（role=55 或 77）才能访问
            - MyBatis-Plus 分页支持
            - 支持多条件过滤：作品、用户、评论层级、审核状态、关键字
            - 按时间倒序排列（最新评论优先）

            ## 参数说明：
            - **current**: **当前页码**，Long 类型，必填，从 1 开始
            - **size**: **每页大小**，Long 类型，必填，范围 1-500
            - **workId**: **作品ID**，Integer 类型，可选，用于过滤特定作品的评论
            - **userId**: **用户ID**，Integer 类型，可选，用于过滤特定用户的评论
            - **commentFloor**: **评论层级**，Integer 类型，可选，1-一级评论、2-二级评论
            - **approvalStatus**: **审核状态**，Integer 类型，可选，10-正常、20-待审核、30-未过审
            - **keyword**: **评论关键字**，String 类型，可选，模糊搜索评论内容

            ## 返回说明：
            - **成功**：返回 IPage<Comments> 对象，包含评论列表和分页信息
            - **无数据**：返回空的分页结果（total=0, records=[]）

            ## 业务逻辑：
            1. 校验分页参数（current>=1, 1<=size<=100）
            2. 构建 MyBatis-Plus 分页对象
            3. 根据可选参数构建动态 SQL 查询
            4. 按时间倒序排列（time DESC）
            5. 返回分页结果集

            ## 注意事项：
            - 所有过滤参数都是可选的，可以组合使用
            - 关键字搜索使用模糊匹配（LIKE '%keyword%'）
            - 只查询未删除的评论（is_delete = 0）
            - 返回完整的 Comments 实体字段
            """
    )
    @GetMapping("/page/{current}/{size}")
    public ResponsePojo<IPage<Comments>> getCommentsPage(
        @Parameter(description = "当前页码（从 1 开始）", required = true, example = "1") @PathVariable Long current,
        @Parameter(description = "每页大小（范围 1-500）", required = true, example = "10") @PathVariable Long size,
        @Parameter(description = "作品ID（可选）", required = false) @RequestParam(required = false) Integer workId,
        @Parameter(description = "用户ID（可选）", required = false) @RequestParam(required = false) Integer userId,
        @Parameter(description = "评论层级（可选，1-一级评论、2-二级评论）", required = false) @RequestParam(required = false) Integer commentFloor,
        @Schema(
            description = "审核状态（可选，10-正常、20-待审核、30-未过审）",
            allowableValues = {"10", "20", "30"},
            example = "20"
        ) @RequestParam(required = false) Integer approvalStatus,
        @Parameter(description = "评论关键字（可选）", required = false) @RequestParam(required = false) String keyword
    ) {
        // 参数校验
        ResponsePojo<?> error = PageUtils.validatePageParams(current, size);
        if (error != null) {
            return (ResponsePojo<IPage<Comments>>) (ResponsePojo<?>) error;
        }

        try {
            IPage<Comments> result = commentService.getCommentsPage(current, size, workId, userId, commentFloor, approvalStatus, keyword);
            return ResponsePojo.success(result, "查询成功");
        } catch (Exception e) {
            log.error("分页查询评论异常，错误: {}", e.getMessage(), e);
            return ResponsePojo.error(null, "查询失败：" + e.getMessage());
        }
    }

}
