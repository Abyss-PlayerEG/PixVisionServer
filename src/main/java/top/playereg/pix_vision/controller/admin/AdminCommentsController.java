package top.playereg.pix_vision.controller.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import top.playereg.pix_vision.pojo.ResponsePojo;
import top.playereg.pix_vision.pojo.admin.AdminBatchOperateCommentResult;
import top.playereg.pix_vision.pojo.entity.Comments;
import top.playereg.pix_vision.service.CommentService;
import top.playereg.pix_vision.util.Annotation.LogRecord;
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
    @LogRecord(module = "评论管理", event = "批量删除评论")
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
     * @param orderBy      排序方式：'oldest' - 按最早发布，其他值或 null - 按最新发布（默认）
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
            - 支持按时间排序（最新/最早）

            ## 参数说明：
            - **current**: **当前页码**，Long 类型，必填，从 1 开始
            - **size**: **每页大小**，Long 类型，必填，范围 1-500
            - **workId**: **作品ID**，Integer 类型，可选，用于过滤特定作品的评论
            - **userId**: **用户ID**，Integer 类型，可选，用于过滤特定用户的评论
            - **commentFloor**: **评论层级**，Integer 类型，可选，1-一级评论、2-二级评论
            - **approvalStatus**: **审核状态**，Integer 类型，可选，10-正常、20-待审核、30-未过审
            - **keyword**: **评论关键字**，String 类型，可选，模糊搜索评论内容
            - **orderBy**: **排序方式**，String 类型，可选
              - 'oldest': 按最早发布排列
              - 其他值或 null: 按最新发布排列（默认）

            ## 返回说明：
            - **成功**：返回 IPage<Comments> 对象，包含评论列表和分页信息
            - **无数据**：返回空的分页结果（total=0, records=[]）

            ## 业务逻辑：
            1. 校验分页参数（current>=1, 1<=size<=100）
            2. 构建 MyBatis-Plus 分页对象
            3. 根据可选参数构建动态 SQL 查询
            4. 根据排序参数动态调整 ORDER BY 子句
            5. 返回分页结果集

            ## 注意事项：
            - 所有过滤参数都是可选的，可以组合使用
            - 关键字搜索使用模糊匹配（LIKE '%keyword%'）
            - 只查询未删除的评论（is_delete = 0）
            - 返回完整的 Comments 实体字段
            """
    )
    @LogRecord(module = "评论管理", event = "分页查询评论")
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
        @Parameter(description = "评论关键字（可选）", required = false) @RequestParam(required = false) String keyword,
        @Schema(description = "排序方式：'oldest' - 按最早发布，其他值或 null - 按最新发布（默认）", allowableValues = {"newest", "oldest"}, example = "newest") @RequestParam(required = false, defaultValue = "newest") String orderBy
    ) {
        // 参数校验
        ResponsePojo<?> error = PageUtils.validatePageParams(current, size);
        if (error != null) {
            return (ResponsePojo<IPage<Comments>>) (ResponsePojo<?>) error;
        }

        try {
            IPage<Comments> result = commentService.getCommentsPage(current, size, workId, userId, commentFloor, approvalStatus, keyword, orderBy);
            return ResponsePojo.success(result, "查询成功");
        } catch (Exception e) {
            log.error("分页查询评论异常，错误: {}", e.getMessage(), e);
            return ResponsePojo.error(null, "查询失败：" + e.getMessage());
        }
    }


    /**
     * 批量更新评论审核状态 - 管理员
     *
     * @param request         HTTP 请求对象
     * @param commentIds      目标评论 ID 列表
     * @param approvalStatus  目标审核状态（10-正常、20-待审核、30-未过审）
     * @return 批量操作结果（包含总数、成功数、失败ID列表）
     * @author blue_sky_ks
     */
    @Operation(
        summary = "批量更新评论审核状态",
        description = """
            # 批量更新评论审核状态（需要登录认证 + 角色权限[55, 77]）

            ## 特性
            - 需要审核员或系统管理员角色（role=55 或 77）才能访问
            - 支持批量更新多个评论的审核状态
            - 更新一级评论时自动级联更新其所有二级评论的审核状态
            - 支持三种审核状态：10-正常、20-待审核、30-未过审
            - 返回详细的操作结果统计信息

            ## 参数说明：
            - **commentIds**: **评论 ID 列表**，List<Integer> 类型，请求参数，必填，不能为空
            - **approvalStatus**: **审核状态**，Integer 类型，请求参数，必填，可选值：
              - 10: 正常（通过审核）
              - 20: 待审核
              - 30: 未过审（违规）

            ## 返回说明：
            - **成功**：返回包含总数、成功数、失败 ID 列表的统计信息
            - **全部失败**：返回错误提示
            - **参数错误**：返回错误提示

            ## 业务逻辑：
            1. 验证当前用户是否为审核员或系统管理员（由拦截器自动验证）
            2. 校验评论 ID 列表和审核状态参数的有效性
            3. 如果是一级评论则查询其所有二级评论，一起更新审核状态
            4. 逐个更新 tb_comments 表中对应评论的 approval_status 字段
            5. 只更新未删除的评论（is_delete = 0）
            6. 记录操作结果并返回统计信息

            ## 注意事项：
            - 设置为 30（未过审）后，评论将在前端不可见
            - 设置为 10（正常）后，评论将在前端重新可见
            - 此操作会立即生效，无需重启服务
            - 更新一级评论时会自动级联更新所有二级评论
            - 已删除的评论不会被更新
            - 即使部分评论更新失败，其他评论仍会成功更新
            """
    )
    @LogRecord(module = "评论管理", event = "批量更新评论审核状态")
    @PostMapping("/update/approval-status")
    public ResponsePojo<AdminBatchOperateCommentResult> adminUpdateCommentsApprovalStatus(
        HttpServletRequest request,
        @Parameter(description = "目标评论 ID 列表", required = true, example = "1,2,3") @RequestParam List<Integer> commentIds,
        @Schema(description = "审核状态：10-正常、20-待审核、30-未过审", allowableValues = {"10", "20", "30"}, example = "30") @RequestParam Integer approvalStatus
    ) {
        // 参数校验
        if (commentIds == null || commentIds.isEmpty()) {
            log.warn("评论 ID 列表为空");
            return ResponsePojo.error(null, "评论 ID 列表不能为空");
        }

        if (approvalStatus == null) {
            log.warn("审核状态为空");
            return ResponsePojo.error(null, "审核状态不能为空");
        }

        // 验证审核状态的合法性
        if (approvalStatus != 10 && approvalStatus != 20 && approvalStatus != 30) {
            log.warn("无效的审核状态: {}", approvalStatus);
            return ResponsePojo.error(null, "审核状态无效，可选值：10-正常、20-待审核、30-未过审");
        }

        // 从 Token 中获取操作者 ID
        Integer userId = (Integer) request.getAttribute("userId");
        if (userId == null) {
            log.warn("无法获取用户 ID");
            return ResponsePojo.error(null, "未授权访问");
        }

        try {
            // 调用服务层批量更新评论审核状态
            AdminBatchOperateCommentResult result = commentService.batchApprovalComments(commentIds, approvalStatus);

            String statusName = getStatusName(approvalStatus);
            if (result.getSuccessCount() > 0) {
                log.info("批量更新评论审核状态完成 - 总数: {}, 成功: {}, 失败: {}, 新状态: {} ({}), 操作者 ID: {}",
                    result.getTotalCount(), result.getSuccessCount(), result.getFailedWorkIds().size(), approvalStatus, statusName, userId);
                return ResponsePojo.success(result, "批量更新评论审核状态处理完成");
            } else {
                log.warn("批量更新评论审核状态全部失败，评论 ID 列表: {}, 目标状态: {} ({}), 操作者 ID: {}", commentIds, approvalStatus, statusName, userId);
                return ResponsePojo.error(result, "更新失败，请检查评论 ID 是否正确");
            }
        } catch (Exception e) {
            log.error("批量更新评论审核状态异常，评论 ID 列表: {}, 目标状态: {}, 操作者 ID: {}, 错误: {}", commentIds, approvalStatus, userId, e.getMessage(), e);
            return ResponsePojo.error(null, "更新失败：" + e.getMessage());
        }
    }

    /**
     * 获取审核状态名称
     *
     * @param approvalStatus 审核状态代码
     * @return 状态名称
     */
    private String getStatusName(Integer approvalStatus) {
        return switch (approvalStatus) {
            case 10 -> "正常";
            case 20 -> "待审核";
            case 30 -> "未过审";
            default -> "未知";
        };
    }
}
