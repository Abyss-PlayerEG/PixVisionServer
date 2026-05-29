package top.playereg.pix_vision.controller.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import top.playereg.pix_vision.pojo.ResponsePojo;
import top.playereg.pix_vision.pojo.VO.admin.AdminAuditRecordVO;
import top.playereg.pix_vision.service.ContentAuditRecordService;
import top.playereg.pix_vision.util.Annotation.LogRecord;
import top.playereg.pix_vision.util.Annotation.RequireRole;
import top.playereg.pix_vision.util.PageUtils;
import top.playereg.pix_vision.util.PixVisionLogger;

/**
 * 管理员控制器 - AI 审核记录查询
 *
 * @author PlayerEG
 */
@RestController
@RequestMapping("/api/admin/audit-records")
@RequiredArgsConstructor
@Tag(name = "系统管理员接口 - AI 审核记录", description = "提供 AI 审核记录的分页查询，支持按内容类型、审核状态、关键词筛选")
@RequireRole(value = {55, 77})
public class AdminAuditRecordController {
    private static final PixVisionLogger log = PixVisionLogger.create(AdminAuditRecordController.class);

    private final ContentAuditRecordService contentAuditRecordService;

    /**
     * 分页查询 AI 审核记录
     *
     * @param current        当前页码
     * @param size           每页大小
     * @param contentType    内容类型（可选）
     * @param approvalStatus 审核状态（可选）
     * @param keyword        关键词（可选）
     * @param orderBy        排序方式（可选）
     * @return 分页审核记录列表
     * @author PlayerEG
     */
    @Operation(
        summary = "分页查询 AI 审核记录",
        description = """
            # 分页查询 AI 审核记录（需要登录认证 + 角色权限[55, 77]）

            ## 特性
            - 需要审核员或系统管理员角色（role=55 或 77）才能访问
            - MyBatis-Plus 分页支持
            - 支持多条件筛选：内容类型、审核状态、关键词
            - 支持按审核时间排序（最新/最早）
            - 返回完整的审核记录字段（审核原因、命中敏感词、原始内容等），original_content 直接从数据库读取

            ## 参数说明：
            - **current**: **当前页码**，Long 类型，路径参数，必填，从 1 开始
            - **size**: **每页大小**，Long 类型，路径参数，必填，范围 1-500
            - **contentType**: **内容类型**，Integer 类型，查询参数，可选
              - 100: 作品
              - 200: 评论
              - 300: 系列
              - 400: 昵称
            - **approvalStatus**: **审核状态**，Integer 类型，查询参数，可选
              - 10: 通过
              - 20: 待审核
              - 30: 违规
            - **keyword**: **关键词**，String 类型，查询参数，可选，模糊搜索审核原因
            - **orderBy**: **排序方式**，String 类型，查询参数，可选
              - 'newest': 按审核时间倒序（默认）
              - 'oldest': 按审核时间正序

            ## 返回说明：
            - **成功**：返回 IPage<AdminAuditRecordVO> 对象，包含审核记录列表、分页信息及原始内容（original_content 字段）
            - **无数据**：返回空的分页结果（total=0, records=[]）

            ## original_content 字段说明：
            - 该字段在 AI 审核时已写入数据库，直接返回，无需额外查询
            - 作品（100）：作品标题
            - 评论（200）：评论文本
            - 系列（300）：格式为「标题|描述」（无描述时仅返回标题）
            - 昵称（400）：昵称

            ## 业务逻辑：
            1. 校验分页参数（current>=1, 1<=size<=500）
            2. 构建 MyBatis-Plus 分页对象
            3. 根据可选参数构建动态 SQL 查询
            4. 根据排序参数动态调整 ORDER BY 子句
            5. 返回分页结果集（original_content 直接从数据库读取）

            ## 注意事项：
            - 所有筛选条件均为可选，可以自由组合
            - 关键词搜索使用模糊匹配（LIKE '%keyword%'）
            - 排序仅支持按审核时间，不支持其他字段排序
            - 返回所有审核记录（不区分内容类型），方便审核员统一查看
            """
    )
    @LogRecord(module = "AI 审核记录", event = "分页查询审核记录")
    @GetMapping("/page/{current}/{size}")
    public ResponsePojo<IPage<AdminAuditRecordVO>> getAuditRecordsPage(
        @Parameter(description = "当前页码（从 1 开始）", required = true, example = "1") @PathVariable Long current,
        @Parameter(description = "每页大小（范围 1-500）", required = true, example = "10") @PathVariable Long size,
        @Schema(description = "内容类型（可选，100-作品、200-评论、300-系列、400-昵称）",
            allowableValues = {"100", "200", "300", "400"}, example = "100")
        @RequestParam(required = false) Integer contentType,
        @Schema(description = "审核状态（可选，10-通过、20-待审核、30-违规）",
            allowableValues = {"10", "20", "30"}, example = "30")
        @RequestParam(required = false) Integer approvalStatus,
        @Parameter(description = "关键词（可选，模糊搜索审核原因）", required = false)
        @RequestParam(required = false) String keyword,
        @Schema(description = "排序方式：'newest' - 最新优先（默认）、'oldest' - 最早优先",
            allowableValues = {"newest", "oldest"}, example = "newest")
        @RequestParam(required = false, defaultValue = "newest") String orderBy
    ) {
        // 参数校验
        ResponsePojo<?> error = PageUtils.validatePageParams(current, size);
        if (error != null) {
            return (ResponsePojo<IPage<AdminAuditRecordVO>>) (ResponsePojo<?>) error;
        }

        try {
            IPage<AdminAuditRecordVO> result = contentAuditRecordService.getAuditRecordsPage(
                current, size, contentType, approvalStatus, keyword, orderBy
            );
            return ResponsePojo.success(result, "查询成功");
        } catch (Exception e) {
            log.error("分页查询审核记录异常，错误: {}", e.getMessage(), e);
            return ResponsePojo.error(null, "查询失败：" + e.getMessage());
        }
    }
}
