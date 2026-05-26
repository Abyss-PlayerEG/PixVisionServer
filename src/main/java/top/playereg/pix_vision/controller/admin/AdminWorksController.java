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
import top.playereg.pix_vision.pojo.Works;
import top.playereg.pix_vision.pojo.adminPojo.AdminBatchOperateWorkResult;
import top.playereg.pix_vision.service.WorkService;
import top.playereg.pix_vision.util.Annotation.LogRecord;
import top.playereg.pix_vision.util.Annotation.RequireRole;
import top.playereg.pix_vision.util.PageUtils;
import top.playereg.pix_vision.util.PixVisionLogger;

import java.util.List;

@RestController
@RequestMapping("/api/admin/works")
@RequiredArgsConstructor
@Tag(name = "系统管理员接口 - 作品管理", description = "提供作品管理的后台接口，包括批量更新审核状态、删除作品等操作")
@RequireRole(value = {55, 77})
public class AdminWorksController {
    private static final PixVisionLogger log = PixVisionLogger.create(AdminWorksController.class);

    private final WorkService workService;


    /**
     * 批量更新作品审核状态 - 管理员
     *
     * @param workIds        作品 ID 列表
     * @param approvalStatus 审核状态（10-正常、20-待审核、30-未过审）
     * @return 批量操作结果（包含总数、成功数、失败ID列表）
     * @author PlayerEG
     */
    @Operation(
        summary = "批量更新作品审核状态",
        description = """
            # 批量更新作品审核状态（需要登录认证 + 角色权限[55, 77]）

            ## 特性
            - 需要审核员或系统管理员角色（role=55 或 77）才能访问
            - 支持批量更新多个作品的审核状态
            - 支持三种审核状态：10-正常、20-待审核、30-未过审
            - 返回详细的操作结果统计信息

            ## 参数说明：
            - **workIds**: **作品 ID 列表**，List<Integer> 类型，请求参数，必填，不能为空
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
            2. 校验作品 ID 列表和审核状态参数的有效性
            3. 批量更新 tb_works 表中对应作品的 approval_status 字段
            4. 记录操作结果并返回统计信息

            ## 注意事项：
            - 设置为 30（未过审）后，作品将在前端不可见
            - 设置为 10（正常）后，作品将在前端重新可见
            - 此操作会立即生效，无需重启服务
            - 建议谨慎使用，操作前请确认作品 ID 和状态的正确性
            - 即使部分作品更新失败，其他作品仍会成功更新
            """
    )
    @LogRecord(module = "作品管理", event = "批量更新作品审核状态")
    @PostMapping("/update/approval-status")
    public ResponsePojo<AdminBatchOperateWorkResult> batchUpdateApprovalStatus(
        HttpServletRequest request,
        @Parameter(description = "目标作品 ID 列表", required = true, example = "1,2,3") @RequestParam List<Integer> workIds,
        @Schema(description = "审核状态：10-正常、20-待审核、30-未过审", allowableValues = {"10", "20", "30"}, example = "30") @RequestParam Integer approvalStatus
    ) {
        // 参数校验
        if (workIds == null || workIds.isEmpty()) {
            log.warn("作品 ID 列表为空");
            return ResponsePojo.error(null, "作品 ID 列表不能为空");
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
            // 调用服务层批量更新作品审核状态
            AdminBatchOperateWorkResult result = workService.batchUpdateApprovalStatus(workIds, approvalStatus, userId);

            String statusName = getStatusName(approvalStatus);
            if (result.getSuccessCount() > 0) {
                log.info("批量更新作品审核状态完成 - 总数: {}, 成功: {}, 失败: {}, 新状态: {} ({}), 操作者 ID: {}",
                    result.getTotalCount(), result.getSuccessCount(), result.getFailedWorkIds().size(), approvalStatus, statusName, userId);
                return ResponsePojo.success(result, "批量更新作品审核状态处理完成");
            } else {
                log.warn("批量更新作品审核状态全部失败，作品 ID 列表: {}, 目标状态: {} ({}), 操作者 ID: {}", workIds, approvalStatus, statusName, userId);
                return ResponsePojo.error(result, "更新失败，请检查作品 ID 是否正确");
            }
        } catch (Exception e) {
            log.error("批量更新作品审核状态异常，作品 ID 列表: {}, 目标状态: {}, 操作者 ID: {}, 错误: {}", workIds, approvalStatus, userId, e.getMessage(), e);
            return ResponsePojo.error(null, "更新失败：" + e.getMessage());
        }
    }

    /**
     * 删除作品 - 管理员
     *
     * @param workIds ID 列表
     * @return 批量操作结果（包含总数、成功数、失败ID列表）
     * @author blue_sky_ks, PlayerEG
     */
    @Operation(
        summary = "批量删除作品",
        description = """
            # 批量删除作品（需要登录认证 + 角色权限[77]）

            ## 特性
            - 需要系统管理员角色（role=77）才能访问
            - 支持批量删除多个作品
            - 逻辑删除作品（is_delete = 1）
            - 原图和封面文件同步重命名为 .del 后缀
            - 返回详细的操作结果统计信息

            ## 参数说明：
            - **workIds**: **作品 ID 列表**，List<Integer> 类型，请求参数，必填，不能为空

            ## 返回说明：
            - **成功**：返回包含总数、成功数、失败 ID 列表的统计信息
            - **全部失败**：返回错误提示
            - **参数错误**：返回错误提示

            ## 业务逻辑：
            1. 校验作品 ID 列表参数的有效性（非空）
            2. 查询作品信息并获取文件名
            3. 将原图文件重命名为 .del 后缀
            4. 同步将封面文件重命名为 .del 后缀（封面不存在时静默跳过）
            5. 执行数据库逻辑删除（is_delete = 1）
            6. 记录操作日志并返回统计信息

            ## 注意事项：
            - 这是一个**受保护接口**，只有系统管理员（role=77）可以访问
            - 删除操作是逻辑删除，数据不会从数据库中物理移除
            - **原图和封面文件**都会被重命名为 .del 后缀（如 123.png → 123.png.del，123_thumb.jpg → 123_thumb.jpg.del）
            - 封面文件不存在时（thumb_url 为 NULL），静默跳过，仅重命名原图文件
            - 支持任意当前状态的文件重命名（正常/.pend/.fail → .del）
            - 删除后作品在前端不可见
            - 建议在执行前确认作品 ID 的正确性
            """
    )
    @LogRecord(module = "作品管理", event = "批量删除作品")
    @PostMapping("/delete")
    @RequireRole(value = {77})
    public ResponsePojo<AdminBatchOperateWorkResult> adminDeleteWorks(
        @Parameter(description = "目标作品 ID 列表", required = true, example = "1,2,3") @RequestParam List<Integer> workIds
    ) {
        // 参数校验
        if (workIds == null || workIds.isEmpty()) {
            log.warn("作品 ID 列表为空");
            return ResponsePojo.error(null, "作品 ID 列表不能为空");
        }

        try {
            // 调用服务层批量删除作品
            AdminBatchOperateWorkResult result = workService.adminBatchDeleteWorks(workIds);

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
     * 分页查询作品列表 - 管理员
     *
     * @param current 当前页码
     * @param size    每页大小
     * @param keyword 关键字（可选）
     * @param orderBy 排序方式：'oldest' - 按最早发布，其他值或 null - 按最新发布（默认）
     * @return 分页结果
     * @author PlayerEG
     */
    @Operation(
        summary = "分页查询作品列表",
        description = """
            # 分页查询作品列表（需要登录认证 + 角色权限[55, 77]）

            ## 特性
            - 需要审核员或系统管理员角色（role=55 或 77）才能访问
            - MyBatis-Plus 分页支持
            - 支持关键字过滤（标题模糊搜索）
            - 支持一级评论按时间排序（最新/最早）
            - 自动填充最新的浏览量数据

            ## 参数说明：
            - **current**: **当前页码**，Long 类型，必填，从 1 开始
            - **size**: **每页大小**，Long 类型，必填，范围 1-500
            - **keyword**: **关键字**，String 类型，可选，模糊搜索作品标题
            - **orderBy**: **排序方式**，String 类型，可选
              - 'oldest': 按最早发布排列
              - 其他值或 null: 按最新发布排列（默认）

            ## 返回说明：
            - **成功**：返回 IPage<Works> 对象，包含作品列表和分页信息
            - **无数据**：返回空的分页结果（total=0, records=[]）

            ## 业务逻辑：
            1. 校验分页参数（current>=1, 1<=size<=500）
            2. 构建 MyBatis-Plus 分页对象
            3. 根据关键字参数构建动态 SQL 查询
            4. 根据排序参数动态调整 ORDER BY 子句
            5. 为每个作品填充最新的浏览量数据
            6. 返回分页结果集

            ## 注意事项：
            - 关键字搜索使用模糊匹配（LIKE '%keyword%'）
            - 只查询未删除的作品（is_delete = 0）
            - 不过滤审核状态，返回所有状态的作品
            - 返回完整的 Works 实体字段
            """
    )
    @LogRecord(module = "作品管理", event = "分页查询作品")
    @GetMapping("/page/{current}/{size}")
    public ResponsePojo<IPage<Works>> getAdminWorksPage(
        @Parameter(description = "当前页码（从 1 开始）", required = true, example = "1") @PathVariable Long current,
        @Parameter(description = "每页大小（范围 1-500）", required = true, example = "10") @PathVariable Long size,
        @Parameter(description = "关键字（可选，模糊搜索标题）", required = false) @RequestParam(required = false) String keyword,
        @Schema(description = "排序方式：'oldest' - 按最早发布，其他值或 null - 按最新发布（默认）", allowableValues = {"newest", "oldest"}, example = "newest") @RequestParam(required = false, defaultValue = "newest") String orderBy
    ) {
        // 参数校验
        ResponsePojo<?> error = PageUtils.validatePageParams(current, size);
        if (error != null) {
            return (ResponsePojo<IPage<Works>>) (ResponsePojo<?>) error;
        }

        try {
            IPage<Works> result = workService.getAdminWorksPage(current, size, keyword, orderBy);
            return ResponsePojo.success(result, "查询成功");
        } catch (Exception e) {
            log.error("分页查询作品异常，错误: {}", e.getMessage(), e);
            return ResponsePojo.error(null, "查询失败：" + e.getMessage());
        }
    }

    /**
     * 批量更新作品标题
     *
     * @param workIds   作品 ID 列表
     * @param workTitle 作品标题
     * @return 批量操作结果（包含总数、成功数、失败ID列表）
     * @author blue_sky_ks
     */
    @Operation(
        summary = "批量更新作品标题",
        description = """
            # 批量更新作品标题（需要登录认证 + 角色权限[55, 77]）

            ## 特性
            - 需要审核员或系统管理员角色（role=55 或 77）才能访问
            - 支持批量更新多个作品的标题
            - 标题长度限制为最多 16 个字符
            - 返回详细的操作结果统计信息

            ## 参数说明：
            - **workIds**: **作品 ID 列表**，List<Integer> 类型，请求参数，必填，不能为空
            - **workTitle**: **作品标题**，String 类型，请求参数，必填，最多 16 个字符

            ## 返回说明：
            - **成功**：返回包含总数、成功数、失败 ID 列表的统计信息
            - **全部失败**：返回错误提示
            - **参数错误**：返回错误提示

            ## 业务逻辑：
            1. 验证当前用户是否为审核员或系统管理员（由拦截器自动验证）
            2. 校验作品 ID 列表和标题参数的有效性
            3. 验证标题长度（最多 16 个字符）
            4. 查询作品信息，过滤不存在或已删除的作品
            5. 批量更新 tb_works 表中对应作品的 work_title 字段
            6. 记录操作结果并返回统计信息

            ## 注意事项：
            - 标题会被自动去除首尾空格
            - 设置为相同标题也会执行更新操作
            - 此操作会立即生效，无需重启服务
            - 建议谨慎使用，操作前请确认作品 ID 和标题的正确性
            - 即使部分作品更新失败，其他作品仍会成功更新
            """
    )
    @LogRecord(module = "作品管理", event = "批量更新作品标题")
    @PostMapping("/update/work-title")
    public ResponsePojo<AdminBatchOperateWorkResult> batchUpdateWorkTitle(
        HttpServletRequest request,
        @Parameter(description = "目标作品 ID 列表", required = true, example = "1,2,3") @RequestParam List<Integer> workIds,
        @Schema(description = "作品标题（最多 16 个字符）", example = "新标题") @RequestParam String workTitle
    ) {
        // 参数校验
        if (workIds == null || workIds.isEmpty()) {
            log.warn("作品 ID 列表为空");
            return ResponsePojo.error(null, "作品 ID 列表不能为空");
        }

        if (workTitle == null || workTitle.trim().isEmpty()) {
            log.warn("作品标题为空");
            return ResponsePojo.error(null, "作品标题不能为空");
        }

        // 验证标题长度
        String trimmedTitle = workTitle.trim();
        if (trimmedTitle.length() > 16) {
            log.warn("作品标题长度不符合要求，标题长度: {}", trimmedTitle.length());
            return ResponsePojo.error(null, "作品标题长度不能超过 16 个字符");
        }

        // 从 Token 中获取操作者 ID
        Integer userId = (Integer) request.getAttribute("userId");
        if (userId == null) {
            log.warn("无法获取用户 ID");
            return ResponsePojo.error(null, "未授权访问");
        }

        try {
            // 调用服务层批量更新作品标题
            AdminBatchOperateWorkResult result = workService.batchUpdateWorkTitle(workIds, trimmedTitle, userId);

            if (result.getSuccessCount() > 0) {
                log.info("批量更新作品标题完成 - 总数: {}, 成功: {}, 失败: {}, 新标题: {}, 操作者 ID: {}",
                    result.getTotalCount(), result.getSuccessCount(), result.getFailedWorkIds().size(), trimmedTitle, userId);
                return ResponsePojo.success(result, "批量更新作品标题处理完成");
            } else {
                log.warn("批量更新作品标题全部失败，作品 ID 列表: {}, 新标题: {}, 操作者 ID: {}", workIds, trimmedTitle, userId);
                return ResponsePojo.error(result, "更新失败，请检查作品 ID 是否正确");
            }
        } catch (Exception e) {
            log.error("批量更新作品标题异常，作品 ID 列表: {}, 新标题: {}, 操作者 ID: {}, 错误: {}", workIds, trimmedTitle, userId, e.getMessage(), e);
            return ResponsePojo.error(null, "更新失败：" + e.getMessage());
        }
    }
}
