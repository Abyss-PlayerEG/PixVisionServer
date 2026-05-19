package top.playereg.pix_vision.controller.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import top.playereg.pix_vision.pojo.ResponsePojo;
import top.playereg.pix_vision.pojo.adminPojo.AdminBatchOperateWorkResult;
import top.playereg.pix_vision.service.SeriesService;
import top.playereg.pix_vision.util.Annotation.RequireRole;
import top.playereg.pix_vision.util.PixVisionLogger;

import java.util.List;

@RestController
@RequestMapping("/api/admin/series")
@RequiredArgsConstructor
@Tag(name = "系统管理员接口 - 作品合集管理", description = "提供作品合集管理的后台接口，包括批量更新审核状态等操作")
@RequireRole(value = {55, 77})
public class AdminSeriesController {
    private static final PixVisionLogger log = PixVisionLogger.create(AdminSeriesController.class);

    @Autowired
    private final SeriesService seriesService;

    /**
     * 批量修改审核状态
     *
     * @param seriesIds      合集ID列表
     * @param approvalStatus 新状态（10-正常、20-待审核、30-未过审）
     * @return 批量操作结果（包含总数、成功数、失败ID列表）
     * @author blue_sky_ks
     */
    @Operation(
        summary = "批量更新作品合集审核状态",
        description = """
            # 批量更新作品合集审核状态（需要登录认证 + 角色权限[55, 77]）

            ## 特性
            - 需要审核员或系统管理员角色（role=55 或 77）才能访问
            - 支持批量更新多个作品合集的审核状态
            - 支持三种审核状态：10-正常、20-待审核、30-未过审
            - 返回详细的操作结果统计信息

            ## 参数说明：
            - **seriesIds**: **作品合集 ID 列表**，List<Integer> 类型，请求参数，必填，不能为空
            - **approvalStatus**: **审核状态**，Integer 类型，请求参数，必填，可选值：
              - 10: 正常（解封）
              - 20: 待审核
              - 30: 未过审（封禁）

            ## 返回说明：
            - **成功**：返回包含总数、成功数、失败 ID 列表的统计信息
            - **全部失败**：返回错误提示
            - **参数错误**：返回错误提示

            ## 业务逻辑：
            1. 验证当前用户是否为审核员或系统管理员（由拦截器自动验证）
            2. 校验作品合集 ID 列表和审核状态参数的有效性
            3. 批量更新 tb_series 表中对应合集的 approval_status 字段
            4. 记录操作结果并返回统计信息

            ## 注意事项：
            - 设置为 30（未过审）后，合集将在前端不可见
            - 设置为 10（正常）后，合集将在前端重新可见
            - 此操作会立即生效，无需重启服务
            - 建议谨慎使用，操作前请确合集 ID 和状态的正确性
            - 即使部分合集更新失败，其他合集仍会成功更新
            """
    )
    @PostMapping("/update/approval-status")
    public ResponsePojo<AdminBatchOperateWorkResult> batchUpdateApprovalStatus(
        HttpServletRequest request,
        @Parameter(description = "目标作品合集 ID 列表", required = true, example = "1,2,3") @RequestParam List<Integer> seriesIds,
        @Schema(description = "审核状态：10-正常、20-待审核、30-未过审", allowableValues = {"10", "20", "30"}, example = "30") @RequestParam Integer approvalStatus
    ) {
        // 参数校验
        if (seriesIds == null || seriesIds.isEmpty()) {
            log.warn("作品合集 ID 列表为空");
            return ResponsePojo.error(null, "作品合集 ID 列表不能为空");
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
            // 调用服务层批量更新作品合集审核状态
            AdminBatchOperateWorkResult result = seriesService.batchUpdateApprovalStatus(seriesIds, approvalStatus, userId);

            String statusName = getStatusName(approvalStatus);
            if (result.getSuccessCount() > 0) {
                log.info("批量更新作品合集审核状态完成 - 总数: {}, 成功: {}, 失败: {}, 新状态: {} ({}), 操作者 ID: {}",
                    result.getTotalCount(), result.getSuccessCount(), result.getFailedWorkIds().size(), approvalStatus, statusName, userId);
                return ResponsePojo.success(result, "批量更新作品合集审核状态处理完成");
            } else {
                log.warn("批量更新作品合集审核状态全部失败，作品合集 ID 列表: {}, 目标状态: {} ({}), 操作者 ID: {}", seriesIds, approvalStatus, statusName, userId);
                return ResponsePojo.error(result, "更新失败，请检查作品合集 ID 是否正确");
            }
        } catch (Exception e) {
            log.error("批量更新作品合集审核状态异常，作品合集 ID 列表: {}, 目标状态: {}, 操作者 ID: {}, 错误: {}", seriesIds, approvalStatus, userId, e.getMessage(), e);
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

    /**
     * 批量删除作品合集
     *
     * @param seriesIds   要删除的作品合集 ID 列表
     * @param deleteWorks 是否删除系列内的作品（true=删除作品，false=将作品的 series_id 置空）
     * @return 批量操作结果（包含总数、成功数、失败ID列表）
     * @author blue_sky_ks
     */
    @Operation(
        summary = "批量删除作品合集",
        description = """
            # 批量删除作品合集（需要登录认证 + 角色权限[77]）

            ## 特性
            - 需要系统管理员角色（role=77）才能访问
            - 支持批量删除多个作品合集
            - 可选择是否删除合集内的作品
            - 逻辑删除合集（is_delete = 1）
            - 如果选择删除作品，会将作品图片文件重命名为 .del 后缀
            - 返回详细的操作结果统计信息

            ## 参数说明：
            - **seriesIds**: **作品合集 ID 列表**，List<Integer> 类型，请求参数，必填，不能为空
            - **deleteWorks**: **是否删除作品**，Boolean 类型，请求参数，可选，默认 false
              - true: 删除合集内的所有作品（文件重命名为 .del）
              - false: 保留作品，仅将作品的 series_id 置空

            ## 返回说明：
            - **成功**：返回包含总数、成功数、失败 ID 列表的统计信息
            - **全部失败**：返回错误提示
            - **参数错误**：返回错误提示

            ## 业务逻辑：
            1. 验证当前用户是否为系统管理员（由拦截器自动验证）
            2. 校验作品合集 ID 列表参数的有效性
            3. 查询合集信息，过滤不存在或已删除的合集
            4. 根据 deleteWorks 参数处理合集内的作品：
               - true: 查询作品 -> 重命名文件为 .del -> 逻辑删除作品
               - false: 将作品的 series_id 置空
            5. 逻辑删除合集（is_delete = 1）
            6. 记录操作结果并返回统计信息

            ## 注意事项：
            - 这是一个**受保护接口**，只有系统管理员（role=77）可以访问
            - 删除操作是逻辑删除，数据不会从数据库中物理移除
            - 如果选择删除作品，作品文件会被重命名为 .del 后缀
            - 删除后合集在前端不可见
            - 建议在执行前确认合集 ID 的正确性
            - 即使部分合集删除失败，其他合集仍会成功删除
            """
    )
    @PostMapping("/delete")
    @RequireRole(value = {77})
    public ResponsePojo<AdminBatchOperateWorkResult> batchDeleteSeries(
        HttpServletRequest request,
        @Parameter(description = "目标作品合集 ID 列表", required = true, example = "1,2,3") @RequestParam List<Integer> seriesIds,
        @Schema(description = "是否删除系列内的作品（true=删除作品，false=保留作品仅置空series_id）", allowableValues = {"true", "false"}, example = "false") @RequestParam(required = false, defaultValue = "false") Boolean deleteWorks
    ) {
        // 参数校验
        if (seriesIds == null || seriesIds.isEmpty()) {
            log.warn("作品合集 ID 列表为空");
            return ResponsePojo.error(null, "作品合集 ID 列表不能为空");
        }

        // 从 Token 中获取操作者 ID
        Integer userId = (Integer) request.getAttribute("userId");
        if (userId == null) {
            log.warn("无法获取用户 ID");
            return ResponsePojo.error(null, "未授权访问");
        }

        try {
            // 调用服务层批量删除作品合集
            AdminBatchOperateWorkResult result = seriesService.batchDeleteSeries(seriesIds, deleteWorks, userId);

            if (result.getSuccessCount() > 0) {
                log.info("批量删除作品合集完成 - 总数: {}, 成功: {}, 失败: {}, 删除作品: {}, 操作者 ID: {}",
                    result.getTotalCount(), result.getSuccessCount(), result.getFailedWorkIds().size(), deleteWorks, userId);
                return ResponsePojo.success(result, "批量删除作品合集处理完成");
            } else {
                log.warn("批量删除作品合集全部失败，作品合集 ID 列表: {}, 操作者 ID: {}", seriesIds, userId);
                return ResponsePojo.error(result, "删除失败，请检查作品合集 ID 是否正确");
            }
        } catch (Exception e) {
            log.error("批量删除作品合集异常，作品合集 ID 列表: {}, 操作者 ID: {}, 错误: {}", seriesIds, userId, e.getMessage(), e);
            return ResponsePojo.error(null, "删除失败：" + e.getMessage());
        }
    }

    /**
     * 批量修改合集信息
     *
     * @param seriesIds         要修改的合集 ID 列表
     * @param seriesName        修改后的合集名称（可选，最多 16 个字符）
     * @param seriesDescription 修改后的合集描述（可选，最多 24 个字符）
     * @return 批量操作结果（包含总数、成功数、失败ID列表）
     * @author blue_sky_ks
     */
    @Operation(
        summary = "批量更新作品合集信息",
        description = """
            # 批量更新作品合集信息（需要登录认证 + 角色权限[55, 77]）

            ## 特性
            - 需要审核员或系统管理员角色（role=55 或 77）才能访问
            - 支持批量更新多个作品合集的标题和/或描述
            - 标题长度限制为最多 16 个字符
            - 描述长度限制为最多 24 个字符
            - 可以只更新标题、只更新描述，或同时更新两者
            - 返回详细的操作结果统计信息

            ## 参数说明：
            - **seriesIds**: **作品合集 ID 列表**，List<Integer> 类型，请求参数，必填，不能为空
            - **seriesName**: **合集名称**，String 类型，请求参数，可选，最多 16 个字符
            - **seriesDescription**: **合集描述**，String 类型，请求参数，可选，最多 24 个字符
            - **注意**: seriesName 和 seriesDescription 至少需要提供一个

            ## 返回说明：
            - **成功**：返回包含总数、成功数、失败 ID 列表的统计信息
            - **全部失败**：返回错误提示
            - **参数错误**：返回错误提示

            ## 业务逻辑：
            1. 验证当前用户是否为审核员或系统管理员（由拦截器自动验证）
            2. 校验作品合集 ID 列表参数的有效性
            3. 验证至少提供了一个更新字段（标题或描述）
            4. 验证标题长度（最多 16 个字符）和描述长度（最多 24 个字符）
            5. 查询合集信息，过滤不存在或已删除的合集
            6. 批量更新 tb_series 表中对应合集的 series_title 和/或 about_text 字段
            7. 记录操作结果并返回统计信息

            ## 注意事项：
            - 标题和描述会被自动去除首尾空格
            - 可以只更新其中一个字段，另一个保持不变
            - 此操作会立即生效，无需重启服务
            - 建议谨慎使用，操作前请确认合集 ID 的正确性
            - 即使部分合集更新失败，其他合集仍会成功更新
            """
    )
    @PostMapping("/update/series-info")
    @RequireRole(value = {77})
    public ResponsePojo<AdminBatchOperateWorkResult> batchUpdateSeriesInfo(
        HttpServletRequest request,
        @Parameter(description = "目标作品合集 ID 列表", required = true, example = "1,2,3") @RequestParam List<Integer> seriesIds,
        @Schema(description = "合集名称（可选，最多 16 个字符）", example = "新合集名称") @RequestParam(required = false) String seriesName,
        @Schema(description = "合集描述（可选，最多 24 个字符）", example = "新合集描述") @RequestParam(required = false) String seriesDescription
    ) {
        // 参数校验
        if (seriesIds == null || seriesIds.isEmpty()) {
            log.warn("作品合集 ID 列表为空");
            return ResponsePojo.error(null, "作品合集 ID 列表不能为空");
        }

        // 验证至少提供了一个更新字段
        boolean hasName = seriesName != null && !seriesName.trim().isEmpty();
        boolean hasDescription = seriesDescription != null && !seriesDescription.trim().isEmpty();

        if (!hasName && !hasDescription) {
            log.warn("合集名称和描述不能同时为空");
            return ResponsePojo.error(null, "合集名称和描述至少需要提供一个");
        }

        // 从 Token 中获取操作者 ID
        Integer userId = (Integer) request.getAttribute("userId");
        if (userId == null) {
            log.warn("无法获取用户 ID");
            return ResponsePojo.error(null, "未授权访问");
        }

        try {
            // 调用服务层批量更新作品合集信息
            AdminBatchOperateWorkResult result = seriesService.batchUpdateSeriesInfo(seriesIds, seriesName, seriesDescription, userId);

            if (result.getSuccessCount() > 0) {
                log.info("批量更新作品合集信息完成 - 总数: {}, 成功: {}, 失败: {}, 操作者 ID: {}",
                    result.getTotalCount(), result.getSuccessCount(), result.getFailedWorkIds().size(), userId);
                return ResponsePojo.success(result, "批量更新作品合集信息处理完成");
            } else {
                log.warn("批量更新作品合集信息全部失败，作品合集 ID 列表: {}, 操作者 ID: {}", seriesIds, userId);
                return ResponsePojo.error(result, "更新失败，请检查作品合集 ID 是否正确");
            }
        } catch (Exception e) {
            log.error("批量更新作品合集信息异常，作品合集 ID 列表: {}, 操作者 ID: {}, 错误: {}", seriesIds, userId, e.getMessage(), e);
            return ResponsePojo.error(null, "更新失败：" + e.getMessage());
        }
    }
}
