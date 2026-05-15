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
import top.playereg.pix_vision.pojo.adminPojo.AdminBatchOperateWorkResult;
import top.playereg.pix_vision.service.PendingReviewsService;
import top.playereg.pix_vision.util.Annotation.RequireRole;
import top.playereg.pix_vision.util.PixVisionLogger;

import java.util.List;

@RestController
@RequestMapping("/api/admin/works")
@RequiredArgsConstructor
@Tag(name = "系统管理员接口 - 作品管理")
@RequireRole(value = {55, 77})
public class AdminWorksController {
    private static final PixVisionLogger log = PixVisionLogger.create(AdminWorksController.class);

    private final PendingReviewsService pendingReviewsService;


    /**
     * 封禁作品 - 管理员
     *
     * @param workIds ID 列表
     * @return 批量操作结果（包含总数、成功数、失败ID列表）
     * @author blue_sky_ks
     */
    @Operation(
        summary = "封禁作品接口 - 管理员",
        description = """
            ## 功能说明：
            批量封禁指定作品，将作品状态设置为封禁状态。

            ## 参数说明：
            - workIds: 要封禁的作品 ID 列表，不能为空

            ## 返回说明：
            - totalCount: 需要封禁的作品总数
            - successCount: 成功封禁的作品数量
            - failedWorkIds: 封禁失败的作品 ID 列表

            ## 业务逻辑：
            1. 验证当前用户是否为系统管理员（由拦截器自动验证）
            2. 校验作品 ID 列表不为空
            3. 逐个更新 tb_pending_reviews 表中对应作品的状态为 30（封禁）
            4. 只更新 data_type = 100（作品类型）的记录
            5. 记录每个作品的操作结果，返回详细的统计信息

            ## 注意事项：
            - 封禁后作品将在前端不可见
            - 此操作会立即生效，无需重启服务
            - 建议谨慎使用，封禁前请确认作品确实违规
            - 即使部分作品封禁失败，其他作品仍会成功封禁
            """
    )
    @PostMapping("/banned")
    public ResponsePojo<AdminBatchOperateWorkResult> adminBannedWorks(
        @Parameter(description = "目标作品 ID 列表", required = true, example = "1,2,3") @RequestParam List<Integer> workIds
    ){
        // 参数校验
        if (workIds == null || workIds.isEmpty()) {
            log.warn("作品 ID 列表为空");
            return ResponsePojo.error(null, "作品 ID 列表不能为空");
        }

        try {
            // 调用服务层批量更新作品状态为 30（封禁）
            AdminBatchOperateWorkResult result = pendingReviewsService.updateWorkStatusBatch(workIds, 30);

            if (result.getSuccessCount() > 0) {
                log.info("批量封禁作品完成 - 总数: {}, 成功: {}, 失败: {}",
                    result.getTotalCount(), result.getSuccessCount(), result.getFailedWorkIds().size());
                return ResponsePojo.success(result, "批量封禁作品处理完成");
            } else {
                log.warn("批量封禁作品全部失败，作品 ID 列表: {}", workIds);
                return ResponsePojo.error(result, "封禁失败，请检查作品 ID 是否正确");
            }
        } catch (Exception e) {
            log.error("批量封禁作品异常，作品 ID 列表: {}, 错误: {}", workIds, e.getMessage(), e);
            return ResponsePojo.error(null, "封禁失败：" + e.getMessage());
        }
    }

    /**
     * 解封作品 - 管理员
     *
     * @param workIds ID 列表
     * @return 批量操作结果（包含总数、成功数、失败ID列表）
     * @author blue_sky_ks
     */
    @Operation(
        summary = "解封作品接口 - 管理员",
        description = """
            ## 功能说明：
            批量解封指定作品，将作品状态设置为正常状态。

            ## 参数说明：
            - workIds: 要解封的作品 ID 列表，不能为空

            ## 返回说明：
            - totalCount: 需要解封的作品总数
            - successCount: 成功解封的作品数量
            - failedWorkIds: 解封失败的作品 ID 列表

            ## 业务逻辑：
            1. 验证当前用户是否为系统管理员（由拦截器自动验证）
            2. 校验作品 ID 列表不为空
            3. 逐个更新 tb_pending_reviews 表中对应作品的状态为 10（正常）
            4. 只更新 data_type = 100（作品类型）的记录
            5. 记录每个作品的操作结果，返回详细的统计信息

            ## 注意事项：
            - 解封后作品将在前端重新可见
            - 此操作会立即生效，无需重启服务
            - 仅对已封禁的作品有效
            - 即使部分作品解封失败，其他作品仍会成功解封
            """
    )
    @PostMapping("/unban")
    public ResponsePojo<AdminBatchOperateWorkResult> adminUnbanWorks(
        @Parameter(description = "目标作品 ID 列表", required = true, example = "1,2,3") @RequestParam List<Integer> workIds
    ){
        // 参数校验
        if (workIds == null || workIds.isEmpty()) {
            log.warn("作品 ID 列表为空");
            return ResponsePojo.error(null, "作品 ID 列表不能为空");
        }

        try {
            // 调用服务层批量更新作品状态为 10（正常）
            AdminBatchOperateWorkResult result = pendingReviewsService.updateWorkStatusBatch(workIds, 10);

            if (result.getSuccessCount() > 0) {
                log.info("批量解封作品完成 - 总数: {}, 成功: {}, 失败: {}",
                    result.getTotalCount(), result.getSuccessCount(), result.getFailedWorkIds().size());
                return ResponsePojo.success(result, "批量解封作品处理完成");
            } else {
                log.warn("批量解封作品全部失败，作品 ID 列表: {}", workIds);
                return ResponsePojo.error(result, "解封失败，请检查作品 ID 是否正确");
            }
        } catch (Exception e) {
            log.error("批量解封作品异常，作品 ID 列表: {}, 错误: {}", workIds, e.getMessage(), e);
            return ResponsePojo.error(null, "解封失败：" + e.getMessage());
        }
    }


    /**
     * 删除作品 - 管理员
     *
     * @param workIds ID 列表
     * @return 批量操作结果（包含总数、成功数、失败ID列表）
     * @author blue_sky_ks
     */
    @Operation(
        summary = "删除作品接口 - 管理员",
        description = """
            # 批量删除作品（需要系统管理员权限）

            ## 特性
            - 需要系统管理员角色（role=77）才能访问
            - 支持批量删除多个作品
            - 返回详细的操作结果统计信息
            - 包含成功和失败的作品 ID 列表

            ## 参数说明：
            - workIds: **作品 ID 列表**，List<Integer> 类型，请求参数，必填，不能为空

            ## 返回说明：
            - **删除成功**：返回 `AdminBatchOperateWorkResult` 对象，包含总数、成功数、失败ID列表等信息
            - **全部失败**：返回错误提示 "删除失败，请检查作品 ID 是否正确"
            - **参数错误**：返回错误提示 "作品 ID 列表不能为空"
            - **异常处理**：捕获并返回具体的异常信息

            ## 业务逻辑：
            1. 校验作品 ID 列表参数的有效性（非空）
            2. 调用服务层执行批量删除操作
            3. 记录删除操作的日志信息
            4. 根据删除结果返回相应的响应信息
            5. 对异常情况做统一处理

            ## 注意事项：
            - 这是一个**受保护接口**，只有系统管理员（role=77）可以访问
            - 删除操作是高级操作，一般被删除的作品，不允许恢复
            - 建议在执行前确认作品 ID 的正确性
            - 返回的结果中包含详细的成功/失败统计信息
            """
    )
    @PostMapping("/delete")
    @RequireRole(value = {77})
    public ResponsePojo<AdminBatchOperateWorkResult> adminDeleteWorks(
        @Parameter(description = "目标作品 ID 列表", required = true, example = "1,2,3") @RequestParam List<Integer> workIds
    ){
        // 参数校验
        if (workIds == null || workIds.isEmpty()) {
            log.warn("作品 ID 列表为空");
            return ResponsePojo.error(null, "作品 ID 列表不能为空");
        }

        try {
            // 调用服务层批量删除
            AdminBatchOperateWorkResult result = pendingReviewsService.batchDeleteWorks(workIds, 1);

            if (result.getSuccessCount() > 0) {
                log.info("批量删除作品完成 - 总数: {}, 成功: {}, 失败: {}",
                    result.getTotalCount(), result.getSuccessCount(), result.getFailedWorkIds().size());
                return ResponsePojo.success(result, "批量删除作品处理完成");
            } else {
                log.warn("批量删除作品全部失败，作品 ID 列表: {}", workIds);
                return ResponsePojo.error(result, "删除失败，请检查作品 ID 是否正确");
            }
        } catch (Exception e) {
            log.error("批量删除作品异常，作品 ID 列表: {}, 错误: {}", workIds, e.getMessage(), e);
            return ResponsePojo.error(null, "删除失败：" + e.getMessage());
        }
    }
}
