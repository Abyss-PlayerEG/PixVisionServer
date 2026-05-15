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
@RequireRole(value = {77})
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
    @RequireRole(value = {77})
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
    @RequireRole(value = {77})
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

}
