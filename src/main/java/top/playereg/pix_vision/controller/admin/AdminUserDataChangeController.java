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
import top.playereg.pix_vision.pojo.VO.UserDataChangeLockVO;
import top.playereg.pix_vision.pojo.admin.AdminBatchOperateWorkResult;
import top.playereg.pix_vision.service.TokenWhitelistService;
import top.playereg.pix_vision.service.UserService;
import top.playereg.pix_vision.util.Annotation.LogRecord;
import top.playereg.pix_vision.util.Annotation.RequireRole;
import top.playereg.pix_vision.util.PageUtils;

import java.util.List;

/**
 * 管理员控制器 - 用户数据变更审核
 *
 * @author PlayerEG
 */
@RestController
@RequestMapping("/api/admin/user-data-change")
@RequiredArgsConstructor
@Tag(name = "系统管理员接口 - 用户数据变更审核", description = "提供用户数据变更审核的后台接口，包括批量审核和分页查询待审核记录等操作")
@RequireRole(value = {55, 77})
public class AdminUserDataChangeController extends AdminBaseController {

    private final UserService userService;
    private final TokenWhitelistService tokenWhitelistService;

    /**
     * 批量审核用户数据变更
     *
     * @param request      HTTP 请求对象
     * @param lockIds  lock_id 列表
     * @param approved true-通过 / false-拒绝
     * @return 批量操作结果
     * @author PlayerEG
     */
    @Operation(
        summary = "批量审核用户数据变更",
        description = """
            # 批量审核用户数据变更（需要登录认证 + 角色权限[55, 77]）

            ## 特性
            - 支持批量通过或拒绝待审核的用户数据变更
            - 审核通过时自动更新用户昵称/角色/头像
            - 头像类型审核通过时自动重命名 .pend 文件为正常后缀
            - 头像类型审核拒绝时自动重命名 .pend 文件为 .fail
            - 权限变更审核通过时自动清除用户角色缓存

            ## 参数说明：
            - **lockIds**: lock_id 列表（必填）
            - **approved**: 审核结果（必填）
              - true: 审核通过，连带更新用户数据
              - false: 审核拒绝，头像文件重命名为 .fail

            ## 返回说明：
            - **成功**：返回包含总数、成功数、失败 ID 列表的统计信息
            - **失败**：返回错误提示

            ## 业务逻辑：
            1. 校验 lockIds 非空，approved 非空
            2. 从 request 中获取当前管理员 ID
            3. 遍历 lockIds，逐个审核：
               a. 验证记录存在且状态为 20（待审核）
               b. 根据 type 执行连带操作
               c. 更新审核状态为 10（通过）或 30（拒绝）
            4. 返回统计结果

            ## 注意事项：
            - 只能审核当前状态为 20（待审核）的记录
            - 审核通过的头像文件将从 .pend 重命名为正常后缀
            - 审核拒绝的头像文件将从 .pend 重命名为 .fail
            - 权限变更通过后会清除用户角色缓存
            """
    )
    @LogRecord(module = "用户数据变更审核", event = "批量审核用户数据变更")
    @PostMapping("/review")
    public ResponsePojo<AdminBatchOperateWorkResult> batchReview(
        HttpServletRequest request,
        @Parameter(description = "lock_id 列表", required = true, example = "1,2,3")
        @RequestParam List<Integer> lockIds,
        @Schema(description = "true-通过, false-拒绝", allowableValues = {"true", "false"}, defaultValue = "true")
        @RequestParam Boolean approved
    ) {
        log.info("管理员开始批量审核用户数据变更 - lockIds: {}, approved: {}", lockIds, approved);

        // 参数校验
        if (lockIds == null || lockIds.isEmpty()) {
            log.warn("lock_id 列表为空");
            return ResponsePojo.error(null, "lock_id 列表不能为空");
        }

        if (approved == null) {
            log.warn("approved 参数为空");
            return ResponsePojo.error(null, "approved 不能为空（true-通过, false-拒绝）");
        }

        try {
            // 统一Token验证
            Integer adminId = validateToken(request, "批量审核用户数据变更", tokenWhitelistService);
            if (adminId == null) {
                return ResponsePojo.error(null, "Token无效或已失效，请重新登录");
            }

            AdminBatchOperateWorkResult result = userService.batchReviewUserDataChange(
                lockIds, approved, adminId
            );

            log.info("批量审核完成 - 总数: {}, 成功: {}, 失败: {}",
                result.getTotalCount(), result.getSuccessCount(),
                result.getFailedWorkIds() != null ? result.getFailedWorkIds().size() : 0);

            return ResponsePojo.success(result, "批量审核处理完成");
        } catch (Exception e) {
            log.error("批量审核异常 - 错误: {}", e.getMessage(), e);
            return ResponsePojo.error(null, "系统错误: " + e.getMessage());
        }
    }

    /**
     * 分页查询待审核记录
     *
     * @param current 当前页码
     * @param size    每页大小
     * @param type    变更类型筛选（可选）
     * @return 分页结果
     * @author PlayerEG
     */
    @Operation(
        summary = "分页查询待审核记录",
        description = """
            # 分页查询待审核的用户数据变更记录（需要登录认证 + 角色权限[55, 77]）

            ## 特性
            - 仅返回待审核状态（approval_status=20）的记录
            - 支持按变更类型筛选
            - 按提交时间倒序排列（最新提交在前）
            - 关联查询用户名和当前昵称

            ## 参数说明：
            - **current**: 当前页码，从 1 开始（路径参数，必填）
            - **size**: 每页大小（路径参数，必填）
            - **type**: 变更类型筛选（可选）
              - 100: 昵称修改
              - 200: 权限申请
              - 300: 头像修改

            ## 返回说明：
            - **成功**：返回分页结果，每条记录包含 lockId、userId、username、type、新数据、旧数据、AI 审核记录（audit_reason、insult_words）等
            - **结果为空**：返回空分页对象和成功状态

            ## 注意事项：
            - 不返回审核状态字段，因为所有记录都是待审核状态
            - 查询结果为空时返回空分页对象和成功状态
            """
    )
    @LogRecord(module = "用户数据变更审核", event = "分页查询待审核记录")
    @GetMapping("/pending/{current}/{size}")
    public ResponsePojo<IPage<UserDataChangeLockVO>> getPendingPage(
        @Parameter(description = "当前页码，从 1 开始", required = true, example = "1") @PathVariable Long current,
        @Parameter(description = "每页大小", required = true, example = "10") @PathVariable Long size,
        @Parameter(description = "变更类型（可选，100-昵称, 200-权限, 300-头像）")
        @RequestParam(required = false) Integer type
    ) {
        log.info("分页查询待审核记录 - 页码: {}, 每页: {}, 类型: {}", current, size, type);

        // 分页参数校验
        ResponsePojo<?> error = PageUtils.validatePageParams(current, size);
        if (error != null) {
            return (ResponsePojo<IPage<UserDataChangeLockVO>>) (ResponsePojo<?>) error;
        }

        try {
            IPage<UserDataChangeLockVO> result = userService.getPendingLockPage(current, size, type);

            log.info("分页查询待审核记录完成 - 总条数: {}, 当前页条数: {}",
                result.getTotal(), result.getRecords().size());
            return ResponsePojo.success(result, "查询成功，共 " + result.getTotal() + " 条待审核记录");
        } catch (Exception e) {
            log.error("分页查询待审核记录异常 - 错误: {}", e.getMessage(), e);
            return ResponsePojo.error(null, "系统错误: " + e.getMessage());
        }
    }
}
