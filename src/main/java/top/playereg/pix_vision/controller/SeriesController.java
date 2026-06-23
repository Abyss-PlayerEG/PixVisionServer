package top.playereg.pix_vision.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import top.playereg.pix_vision.pojo.ResponsePojo;
import top.playereg.pix_vision.pojo.dto.SeriesOperationResult;
import top.playereg.pix_vision.pojo.entity.Series;
import top.playereg.pix_vision.service.SeriesService;
import top.playereg.pix_vision.service.TokenWhitelistService;
import top.playereg.pix_vision.util.Annotation.PublicAccess;
import top.playereg.pix_vision.util.Annotation.RequireRole;
import top.playereg.pix_vision.util.JWTUtils;
import top.playereg.pix_vision.util.PageUtils;
import top.playereg.pix_vision.util.PixVisionLogger;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 系列管理控制器
 *
 * @author PlayerEG
 */
@Tag(name = "用户接口 - 作品合集", description = "提供作品系列的新增、查询、删除等功能")
@RestController
@RequestMapping("/api/work/series")
public class SeriesController {

    private static final PixVisionLogger log = PixVisionLogger.create(SeriesController.class);

    private final SeriesService seriesService;
    private final TokenWhitelistService tokenWhitelistService;

    public SeriesController(SeriesService seriesService, TokenWhitelistService tokenWhitelistService) {
        this.seriesService = seriesService;
        this.tokenWhitelistService = tokenWhitelistService;
    }

    /**
     * 新增作品系列
     *
     * @param request     HTTP 请求对象，用于从 Header 或 URL 参数中获取 Token
     * @param seriesTitle 系列标题（最多 16 个中文字符）
     * @param aboutText   系列描述文本（最多 24 个中文字符，可选）
     * @return 响应数据，表示系列是否新增成功
     * @author PlayerEG
     */
    @PostMapping("/add")
    @RequireRole(value = {22, 77})
    @Operation(
        summary = "新增作品系列接口",
        description = """
            # 新增作品系列（需要登录认证 + 角色权限[22,77]）

            ## 特性
            - Token 认证（支持 Header 和 URL 参数两种方式）
            - 系列标题长度限制（≤16 个中文字符）
            - 系列描述长度限制（≤24 个中文字符）
            - 自动关联当前登录用户
            - AI 内容安全审核（标题和描述）

            ## 参数说明：
            - Authorization: Header 中的 Token，格式为 `Bearer <token>`，或通过 URL 参数 `?token=<token>` 传递
            - seriesTitle: 系列标题，字符串类型，必填，长度不超过 16 个中文字符
            - aboutText: 系列描述文本，字符串类型，可选，长度不超过 24 个中文字符

            ## 返回说明：
            - **审核通过**：返回 **{"data": true}** 和"系列新增成功"提示
            - **AI 审核不通过（违规）**：返回 **{"data": false}** 和"违规内容：{原因}"提示
            - **AI 审核存疑（待审核）**：返回 **{"data": true}** 和"系列新增成功，等待人工审核"提示
            - **Token 失效**：返回 **{"data": null}** 和 "Token 已失效" 提示
            - **标题为空**：返回 **{"data": false}** 和 "系列标题不能为空" 提示
            - **标题过长**：返回 **{"data": false}** 和 "系列标题长度不能超过 16 个字符" 提示
            - **描述过长**：返回 **{"data": false}** 和 "系列描述长度不能超过 24 个字符" 提示
            - **标题重复**：返回 **{"data": false}** 和 "系列标题已存在，请使用其他标题" 提示
            - **新增失败**：返回 **{"data": false}** 和 "系列新增失败" 提示

            ## 业务逻辑：
            1. 从请求中提取并验证 Token
            2. 检查 Token 是否在白名单中
            3. 从 Token 中解析用户 ID
            4. 校验系列标题和描述的长度
            5. **检查系列标题是否已存在（同一用户下不能重复）**
            6. 调用 AI 审核服务对系列标题和描述进行内容安全审核
            7. 根据审核结果设置审核状态（通过-10/待审核-20/违规-30）
            8. 创建系列对象并插入数据库
            9. 根据审核状态返回差异化的响应消息

            ## 注意事项：
            - 系列标题**必填**，长度不超过 16 个字符
            - 系列描述**可选**，长度不超过 24 个字符
            - **同一用户下系列标题不能重复**
            - 系统自动记录创建者 ID 和创建时间
            - 系列新增后会自动调用 AI 审核服务进行内容安全审核（标题+描述）
            - AI 审核不通过（违规）时直接拦截，data 返回 false
            - AI 审核存疑时标记为待审核，系列需要人工审核后才会公开显示
            - AI 审核服务不可用时自动降级为待审核状态
            """
    )
    public ResponsePojo<Boolean> addSeries(
        @Parameter(description = "HTTP 请求对象，用于从 Header 或 URL 参数中获取 Token", required = true) HttpServletRequest request,
        @Parameter(description = "系列标题，最多 16 个中文字符", required = true, example = "我的作品集") @RequestParam String seriesTitle,
        @Parameter(description = "系列描述文本，最多 24 个中文字符", required = false, example = "这是一个展示我作品的系列") @RequestParam(required = false, defaultValue = "") String aboutText
    ) {
        log.debug("新增系列 - 标题: {}", seriesTitle);

        // 提取 Token
        String token = JWTUtils.extractTokenWithLog(request, "新增系列接口");

        if (token == null || token.isEmpty()) {
            log.error("新增系列失败 - Token 不存在");
            return ResponsePojo.error(false, "Token 不存在，请在 Header 中添加 Authorization: Bearer <token> 或在 URL 参数中添加 ?token=<token>");
        }

        // 检查 Token 是否在白名单中
        if (!tokenWhitelistService.isInWhitelist(token)) {
            log.warn("Token 不在白名单中，可能已过期或被移除");
            return ResponsePojo.error(false, "Token 已失效");
        }

        // 从 Token 中获取用户 ID
        Integer userId = JWTUtils.getUserIdFromToken(token);
        if (userId == null) {
            log.error("从 Token 中解析用户 ID 失败");
            return ResponsePojo.error(false, "Token 无效");
        }

        String username = JWTUtils.getUsernameFromToken(token);
        log.info("开始新增系列，用户 ID: {}, 用户名: {}, 系列标题: {}", userId, username, seriesTitle);

        // 校验系列标题参数
        if (seriesTitle == null || seriesTitle.isEmpty()) {
            log.warn("系列标题为空，用户 ID: {}", userId);
            return ResponsePojo.error(false, "系列标题不能为空");
        }

        if (seriesTitle.length() > 16) {
            log.warn("系列标题长度不符合要求，用户 ID: {}, 标题长度: {}", userId, seriesTitle.length());
            return ResponsePojo.error(false, "系列标题长度不能超过 16 个字符");
        }

        // 校验系列描述参数
        if (aboutText != null && aboutText.length() > 24) {
            log.warn("系列描述长度不符合要求，用户 ID: {}, 描述长度: {}", userId, aboutText.length());
            return ResponsePojo.error(false, "系列描述长度不能超过 24 个字符");
        }

        // 调用服务层新增系列
        try {
            SeriesOperationResult result = seriesService.addSeries(userId, seriesTitle, aboutText);

            if (result.getSuccess() == null || !result.getSuccess()) {
                log.error("系列新增失败，用户 ID: {}", userId);
                return ResponsePojo.error(false, "系列新增失败");
            }

            Integer approvalStatus = result.getApprovalStatus();
            String auditReason = result.getAuditReason();

            // 根据审核状态返回差异化响应
            if (approvalStatus != null && approvalStatus == 30) {
                // 违规内容
                String message = auditReason != null ? auditReason : "未知原因";
                log.warn("系列审核不通过（违规），用户 ID: {}, 原因: {}", userId, message);
                return ResponsePojo.error(false, "违规内容：" + message);
            }

            if (approvalStatus != null && approvalStatus == 20) {
                // 待审核
                log.info("系列新增成功，待人工审核，用户 ID: {}", userId);
                return ResponsePojo.success(true, "系列新增成功，等待人工审核");
            }

            // 审核通过（10）
            log.info("系列新增成功，用户 ID: {}", userId);
            return ResponsePojo.success(true, "系列新增成功");
        } catch (IllegalArgumentException e) {
            log.warn("系列新增参数错误，用户 ID: {}, 错误: {}", userId, e.getMessage());
            return ResponsePojo.error(false, e.getMessage());
        }
    }

    /**
     * 分页查询作品系列列表
     *
     * @param current 当前页码（从 1 开始）
     * @param size    每页数量（范围 1-500）
     * @param userId  用户 ID（可选，不传则查询所有用户）
     * @param keyword 搜索关键词（可选）
     * @return 响应数据，包含分页的作品系列列表
     * @author PlayerEG
     */
    @GetMapping("/page/{current}/{size}")
    @PublicAccess("分页查询作品系列，无需认证")
    @Operation(
        summary = "分页查询作品系列接口",
        description = """
            # 分页查询作品系列（无需登录认证）

            ## 特性
            - 公开接口（无需 Token 认证）
            - 支持分页查询
            - **支持按用户筛选**（可选，不传则查询所有用户的系列）
            - **支持关键词搜索**（可选，同时匹配系列标题和描述）
            - 自动过滤逻辑删除数据
            - **只返回审核通过的系列**（approval_status = 10）
            - **关键词匹配时，标题匹配优先排序**，其次按创建时间倒序
            - 返回封面缩略图（取自系列内最新发布的审核通过作品）

            ## 参数说明：
            - **current**: 当前页码，Integer 类型，必填，从 1 开始
            - **size**: 每页数量，Integer 类型，必填，范围 1-500
            - **userId**: 用户 ID，Integer 类型，可选，不传则查询所有用户的系列
            - **keyword**: 搜索关键词，String 类型，可选，同时搜索系列标题和描述

            ## 返回说明：
            - **查询成功**：返回 `{"data": {IPage<Series>对象}}`，包含分页信息和系列列表，每个系列含封面缩略图（thumb_url），系列内无作品时 thumb_url 为 null
            - **无匹配结果**：返回空列表 `[]`，状态码 200

            ## 业务逻辑：
            1. 校验页码和每页数量参数有效性
            2. 如果提供了 userId，只查询该用户的系列；否则查询所有用户的系列
            3. 如果提供了关键词，对系列标题和描述进行模糊匹配（LIKE %keyword%）
            4. 调用 Service 层进行分页查询
            5. 自动排除逻辑删除的数据（is_delete=0）
            6. **只返回审核通过的系列**（approval_status=10）
            7. 待审核（approval_status=20）和未过审（approval_status=30）的系列不会返回
            8. **通过子查询获取系列内最新发布作品（is_delete=0, approval_status=10）的封面缩略图**
            9. **排序规则**：有关键词时，标题匹配的系列优先（CASE WHEN 排序）；再按创建时间倒序

            ## 注意事项：
            - **此接口为公开接口，无需登录即可访问**
            - 如果没有匹配的系列，返回空列表 []
            - **只能查看审核通过的系列**，待审核和未过审的系列不可见
            - keyword 为空或不传时，返回所有系列（按创建时间倒序）
            - 关键词同时搜索系列标题和描述，标题匹配的结果排在最前面
            - 封面取自系列内最新发布且审核通过的作品，无作品时 thumb_url 为 null
            - 封面缩略图访问路径：`/api/image/works?filePath={thumb_url}`
            - 建议合理设置每页数量（size），避免一次性加载过多数据
            """
    )
    public ResponsePojo<IPage<Series>> getSeriesList(
        @Parameter(description = "当前页码，从 1 开始", required = true, example = "1") @PathVariable Integer current,
        @Parameter(description = "每页数量，范围 1-500", required = true, example = "10") @PathVariable Integer size,
        @Parameter(description = "用户 ID（可选，不传则查询所有用户）", example = "1") @RequestParam(required = false) Integer userId,
        @Parameter(description = "搜索关键词（可选），同时匹配系列标题和描述", example = "风景") @RequestParam(required = false) String keyword
    ) {
        log.debug("分页查询作品系列 - 用户 ID: {}, 页码: {}, 每页数量: {}, 关键词: {}", userId, current, size, keyword);

        // 参数校验
        ResponsePojo<?> error = PageUtils.validatePageParams(current.longValue(), size.longValue());
        if (error != null) {
            return (ResponsePojo<IPage<Series>>) (ResponsePojo<?>) error;
        }

        // 如果提供了 userId，校验其有效性
        if (userId != null && userId <= 0) {
            log.warn("用户 ID 无效: {}", userId);
            return ResponsePojo.error(null, "用户 ID 无效");
        }

        // 调用服务层分页查询
        IPage<Series> seriesPage = seriesService.getSeriesByUserId(userId, current, size, keyword);

        if (seriesPage != null) {
            log.info("分页查询成功 - 用户 ID: {}, 总记录数: {}, 当前页记录数: {}",
                userId, seriesPage.getTotal(), seriesPage.getRecords().size());
            return ResponsePojo.success(seriesPage, "查询成功");
        } else {
            log.error("分页查询失败 - 用户 ID: {}", userId);
            return ResponsePojo.error(null, "查询失败");
        }
    }

    /**
     * 删除作品系列（支持保留或删除系列内作品）
     *
     * @param request     HTTP 请求对象，用于从 Header 或 URL 参数中获取 Token
     * @param seriesId    系列 ID
     * @param deleteWorks 是否删除系列内的作品（true=删除作品，false=保留作品但移除系列关联）
     * @return 响应数据，表示系列是否删除成功
     * @author PlayerEG
     */
    @PostMapping("/delete")
    @RequireRole(value = {22, 77})
    @Operation(
        summary = "删除作品系列接口",
        description = """
            # 删除作品系列（需要登录认证 + 角色权限[22,77]）

            ## 特性
            - Token 认证（支持 Header 和 URL 参数两种方式）
            - 支持选择是否删除系列内的作品
            - SQL 层面权限验证（只能删除自己的系列）
            - 逻辑删除（数据不真正从数据库移除）
            - 如果选择删除作品，会将作品文件重命名为 .del 后缀

            ## 参数说明：
            - Authorization: Header 中的 Token，格式为 `Bearer <token>`，或通过 URL 参数 `?token=<token>` 传递
            - seriesId: 系列 ID，Integer 类型，必填
            - deleteWorks: 是否删除系列内的作品，Boolean 类型，必填
              * true: 删除系列及其所有作品（作品文件重命名为 .del）
              * false: 仅删除系列，作品保留但 series_id 置空

            ## 返回说明：
            - **删除成功**：返回 **{"data": true}** 和“系列删除成功”提示
            - **Token 不存在**：返回 **{"data": null}** 和“Token 不存在”提示
            - **Token 已失效**：返回 **{"data": null}** 和“Token 已失效”提示
            - **系列 ID 无效**：返回 **{"data": false}** 和“系列 ID 无效”提示
            - **无权删除**：返回 **{"data": false}** 和“无权删除该系列”提示（系列不属于当前用户）
            - **系列不存在**：返回 **{"data": false}** 和“系列不存在或已删除”提示
            - **删除失败**：返回 **{"data": false}** 和“系列删除失败”提示

            ## 业务逻辑：
            1. 从请求头或 URL 参数中提取 Token（支持 Bearer 前缀）
            2. 验证 Token 是否在白名单中
            3. 从 Token 中解析用户 ID
            4. 校验系列 ID 参数有效性
            5. 查询系列信息并验证所有权（只能删除自己的系列）
            6. 根据 deleteWorks 参数处理系列内的作品：
               - 如果 deleteWorks=true：查询系列下所有作品，重命名文件为 .del，执行逻辑删除
               - 如果 deleteWorks=false：将系列下所有作品的 series_id 置空
            7. 执行系列的逻辑删除（SQL 层面验证 user_id）
            8. 返回删除结果

            ## 注意事项：
            - **需要携带有效的 Token 才能删除系列**
            - Token 必须在白名单中（未过期、未登出）
            - **用户只能删除自己的系列**，无法删除他人的系列
            - 采用逻辑删除方式，数据不会真正从数据库中移除
            - 如果选择删除作品，作品文件会被重命名为 .del 后缀（如 123.png → 123.png.del）
            - 如果选择保留作品，这些作品的 series_id 会被置空，但仍存在于系统中
            - 删除系列后，这些系列在查询接口中将不再显示
            - 建议先确认系列内是否有重要作品，再决定是否删除
            """
    )
    public ResponsePojo<Boolean> deleteSeries(
        @Parameter(description = "HTTP 请求对象，用于从 Header 或 URL 参数中获取 Token", required = true) HttpServletRequest request,
        @Parameter(description = "系列 ID", required = true, example = "1") @RequestParam Integer seriesId,
        @Schema(
            description = "是否删除系列内的作品（true=删除作品，false=保留作品）",
            allowableValues = {"true", "false"},
            example = "false"
        ) @RequestParam Boolean deleteWorks
    ) {
        log.debug("删除系列 - 系列 ID: {}, 是否删除作品: {}", seriesId, deleteWorks);

        // 提取 Token
        String token = JWTUtils.extractTokenWithLog(request, "删除系列接口");

        if (token == null || token.isEmpty()) {
            log.error("删除系列失败 - Token 不存在");
            return ResponsePojo.error(null, "Token 不存在，请在 Header 中添加 Authorization: Bearer <token> 或在 URL 参数中添加 ?token=<token>");
        }

        // 检查 Token 是否在白名单中
        if (!tokenWhitelistService.isInWhitelist(token)) {
            log.warn("Token 不在白名单中，可能已过期或被移除");
            return ResponsePojo.error(null, "Token 已失效");
        }

        // 从 Token 中获取用户 ID
        Integer userId = JWTUtils.getUserIdFromToken(token);
        if (userId == null) {
            log.error("从 Token 中解析用户 ID 失败");
            return ResponsePojo.error(null, "Token 无效");
        }

        String username = JWTUtils.getUsernameFromToken(token);
        log.info("开始删除系列，用户 ID: {}, 用户名: {}, 系列 ID: {}, 是否删除作品: {}", userId, username, seriesId, deleteWorks);

        // 校验系列 ID 参数
        if (seriesId == null || seriesId <= 0) {
            log.warn("系列 ID 无效，用户 ID: {}", userId);
            return ResponsePojo.error(false, "系列 ID 无效");
        }

        // 调用服务层删除系列
        Boolean result = seriesService.deleteSeries(seriesId, userId, deleteWorks);

        if (result) {
            String successMsg = deleteWorks ? "系列及作品删除成功" : "系列删除成功，作品已保留";
            log.info("{}，用户 ID: {}, 用户名: {}, 系列 ID: {}", successMsg, userId, username, seriesId);
            return ResponsePojo.success(true, successMsg);
        } else {
            log.warn("系列删除失败，用户 ID: {}, 用户名: {}, 系列 ID: {}", userId, username, seriesId);
            return ResponsePojo.error(false, "系列删除失败");
        }
    }

    /**
     * 更新系列信息（支持部分字段修改）
     *
     * @param request     HTTP 请求对象，用于从 Header 或 URL 参数中获取 Token
     * @param seriesId    系列 ID
     * @param seriesTitle 系列标题（可选，最多 16 个中文字符）
     * @param aboutText   系列描述（可选，最多 24 个中文字符）
     * @return 响应数据，表示系列信息是否更新成功
     * @author PlayerEG
     */
    @PostMapping("/update")
    @RequireRole(value = {22, 77})
    @Operation(
        summary = "更新系列信息接口",
        description = """
            # 更新系列信息（需要登录认证 + 角色权限[22,77]）

            ## 特性
            - Token 认证（支持 Header 和 URL 参数两种方式）
            - 支持部分字段修改（所有参数可选）
            - SQL 层面权限验证（只能修改自己的系列）
            - 动态更新非空字段
            - 完整的参数校验
            - 标题唯一性检查（同一用户下不能重复）
            - AI 内容安全审核（修改标题或描述时触发）

            ## 参数说明：
            - Authorization: Header 中的 Token，格式为 `Bearer <token>`，或通过 URL 参数 `?token=<token>` 传递
            - seriesId: **系列 ID**，Integer 类型，必填
            - seriesTitle: **系列标题**，String 类型，可选，最多 16 个中文字符
            - aboutText: **系列描述**，String 类型，可选，最多 24 个中文字符

            ## 返回说明：
            - **审核通过**：返回 **{"data": true}** 和"系列信息更新成功"提示
            - **AI 审核不通过（违规）**：返回 **{"data": false}** 和"违规内容：{原因}"提示
            - **AI 审核存疑（待审核）**：返回 **{"data": true}** 和"系列信息更新成功，等待人工审核"提示
            - **Token 不存在**：返回 **{"data": null}** 和"Token 不存在"提示
            - **Token 已失效**：返回 **{"data": null}** 和"Token 已失效"提示
            - **系列 ID 无效**：返回 **{"data": false}** 和"系列 ID 无效"提示
            - **无修改内容**：返回 **{"data": false}** 和"无修改内容"提示
            - **系列不存在**：返回 **{"data": false}** 和"系列不存在或已删除"提示
            - **无权修改**：返回 **{"data": false}** 和"无权修改该系列"提示
            - **标题过长**：返回 **{"data": false}** 和"系列标题长度不能超过 16 个字符"提示
            - **描述过长**：返回 **{"data": false}** 和"系列描述长度不能超过 24 个字符"提示
            - **标题重复**：返回 **{"data": false}** 和"系列标题已存在，请使用其他标题"提示
            - **更新失败**：返回 **{"data": false}** 和"系列信息更新失败"提示

            ## 业务逻辑：
            1. 从请求头或 URL 参数中提取 Token（支持 Bearer 前缀）
            2. 验证 Token 是否在白名单中
            3. 从 Token 中解析用户 ID
            4. 校验系列 ID 参数有效性
            5. 检查是否至少有一个参数不为空
            6. 查询系列信息并验证所有权（只能修改自己的系列）
            7. 如果提供了新标题，验证长度并检查是否与当前标题不同且未被其他系列使用
            8. 如果提供了新描述，验证长度并检查是否与当前描述不同
            9. 调用 AI 审核服务对更新后的系列内容进行安全审核
            10. 根据审核结果设置审核状态（通过-10/待审核-20/违规-30）
            11. 执行动态更新（只更新非空且发生变化的字段）
            12. 自动更新 update_time 和 update_user
            13. 根据审核状态返回差异化的响应消息

            ## 注意事项：
            - **需要携带有效的 Token 才能修改系列**
            - Token 必须在白名单中（未过期、未登出）
            - **用户只能修改自己的系列**，无法修改他人的系列
            - 所有参数都是可选的，可以只修改部分字段
            - **如果所有参数都为空或与当前值相同，将返回"无修改内容"**
            - 系列标题限制：**最多 16 个字符**
            - 系列描述限制：**最多 24 个字符**
            - **同一用户下系列标题不能重复**（修改时也会检查）
            - 采用动态更新机制，只提供要修改的字段即可
            - 修改成功后，update_time 和 update_user 会自动更新
            - 系列更新后会自动调用 AI 审核服务进行内容安全审核
            - AI 审核不通过（违规）时直接拦截，data 返回 false
            - AI 审核存疑时标记为待审核，系列需要人工审核后才会公开显示
            - AI 审核服务不可用时自动降级为待审核状态
            """
    )
    public ResponsePojo<Boolean> updateSeriesInfo(
        @Parameter(description = "HTTP 请求对象，用于从 Header 或 URL 参数中获取 Token", required = true) HttpServletRequest request,
        @Parameter(description = "系列 ID", required = true, example = "1") @RequestParam Integer seriesId,
        @Parameter(description = "系列标题（可选），最多 16 个中文字符", required = false) @RequestParam(required = false) String seriesTitle,
        @Parameter(description = "系列描述（可选），最多 24 个中文字符", required = false) @RequestParam(required = false) String aboutText
    ) {
        log.debug("更新系列信息 - 系列 ID: {}", seriesId);

        // 提取 Token
        String token = JWTUtils.extractTokenWithLog(request, "更新系列信息接口");

        if (token == null || token.isEmpty()) {
            log.error("更新系列信息失败 - Token 不存在");
            return ResponsePojo.error(null, "Token 不存在，请在 Header 中添加 Authorization: Bearer <token> 或在 URL 参数中添加 ?token=<token>");
        }

        // 检查 Token 是否在白名单中
        if (!tokenWhitelistService.isInWhitelist(token)) {
            log.warn("Token 不在白名单中，可能已过期或被移除");
            return ResponsePojo.error(null, "Token 已失效");
        }

        // 从 Token 中获取用户 ID
        Integer userId = JWTUtils.getUserIdFromToken(token);
        if (userId == null) {
            log.error("从 Token 中解析用户 ID 失败");
            return ResponsePojo.error(null, "Token 无效");
        }

        String username = JWTUtils.getUsernameFromToken(token);
        log.info("开始更新系列信息，用户 ID: {}, 用户名: {}, 系列 ID: {}", userId, username, seriesId);

        // 校验系列 ID 参数
        if (seriesId == null || seriesId <= 0) {
            log.warn("系列 ID 无效，用户 ID: {}", userId);
            return ResponsePojo.error(false, "系列 ID 无效");
        }

        // 调用服务层更新系列信息
        try {
            SeriesOperationResult result = seriesService.updateSeriesInfo(seriesId, userId, seriesTitle, aboutText);

            if (result.getSuccess() == null || !result.getSuccess()) {
                log.error("系列信息更新失败，用户 ID: {}, 用户名: {}, 系列 ID: {}", userId, username, seriesId);
                return ResponsePojo.error(false, "系列信息更新失败");
            }

            Integer approvalStatus = result.getApprovalStatus();
            String auditReason = result.getAuditReason();

            // 根据审核状态返回差异化响应
            if (approvalStatus != null && approvalStatus == 30) {
                // 违规内容
                String message = auditReason != null ? auditReason : "未知原因";
                log.warn("系列更新审核不通过（违规），用户 ID: {}, 原因: {}", userId, message);
                return ResponsePojo.error(false, "违规内容：" + message);
            }

            if (approvalStatus != null && approvalStatus == 20) {
                // 待审核
                log.info("系列信息更新成功，待人工审核，用户 ID: {}, 系列 ID: {}", userId, seriesId);
                return ResponsePojo.success(true, "系列信息更新成功，等待人工审核");
            }

            // 审核通过（10）
            log.info("系列信息更新成功，用户 ID: {}, 用户名: {}, 系列 ID: {}", userId, username, seriesId);
            return ResponsePojo.success(true, "系列信息更新成功");
        } catch (IllegalArgumentException e) {
            log.warn("系列信息更新参数错误，用户 ID: {}, 错误: {}", userId, e.getMessage());
            return ResponsePojo.error(false, e.getMessage());
        } catch (SecurityException e) {
            log.warn("系列信息更新权限错误，用户 ID: {}, 错误: {}", userId, e.getMessage());
            return ResponsePojo.error(false, e.getMessage());
        }
    }

    /**
     * 批量将作品添加到指定合集
     *
     * @param request  HTTP 请求对象，用于从 Header 或 URL 参数中获取 Token
     * @param workIds  作品 ID 列表
     * @param seriesId 合集 ID
     * @return 响应数据，表示作品是否批量添加成功
     * @author PlayerEG
     */
    @PostMapping("/batch-add-works")
    @RequireRole(value = {22, 77})
    @Operation(
        summary = "批量添加作品到合集接口",
        description = """
            # 批量添加作品到合集（需要登录认证 + 角色权限[22,77]）

            ## 特性
            - Token 认证（支持 Header 和 URL 参数两种方式）
            - 支持批量将多个作品添加到指定合集
            - SQL 层面双重权限验证（合集归属 + 作品归属）
            - 仅更新有效作品（未删除且属于当前用户）
            - 不重置审核状态（仅修改 series_id）

            ## 参数说明：
            - Authorization: Header 中的 Token，格式为 `Bearer <token>`，或通过 URL 参数 `?token=<token>` 传递
            - workIds: **作品 ID 列表**，字符串类型，必填，逗号分隔，如 "1,2,3"，单次最多 100 个
            - seriesId: **合集 ID**，Integer 类型，必填

            ## 返回说明：
            - **添加成功**：返回 `{"data": true}` 和 "作品批量添加成功" 提示
            - **Token 不存在**：返回 `{"data": null}` 和 "Token 不存在" 提示
            - **Token 已失效**：返回 `{"data": null}` 和 "Token 已失效" 提示
            - **作品 ID 列表为空**：返回 `{"data": false}` 和 "作品 ID 列表不能为空" 提示
            - **作品 ID 格式错误**：返回 `{"data": false}` 和 "作品 ID 格式错误" 提示
            - **合集 ID 无效**：返回 `{"data": false}` 和 "合集 ID 无效" 提示
            - **合集不存在**：返回 `{"data": false}` 和 "合集不存在或已删除" 提示
            - **无权操作合集**：返回 `{"data": false}` 和 "无权操作该合集" 提示
            - **无可操作作品**：返回 `{"data": false}` 和 "没有可操作的作品" 提示
            - **添加失败**：返回 `{"data": false}` 和 "作品批量添加失败" 提示

            ## 业务逻辑：
            1. 从请求中提取并验证 Token
            2. 从 Token 中解析用户 ID
            3. 解析逗号分隔的作品 ID 字符串为列表
            4. 校验作品 ID 列表和合集 ID 参数有效性
            5. 验证合集是否存在且未删除
            6. 验证合集归属权（只能操作自己的合集）
            7. 查询所有作品并过滤有效作品（未删除且属于当前用户）
            8. 批量更新作品的 series_id 为指定合集 ID（SQL 层面再次验证 user_id，确保只能更新自己的作品）
            9. 返回添加结果

            ## 注意事项：
            - **需要携带有效的 Token 才能操作**
            - Token 必须在白名单中（未过期、未登出）
            - **用户只能操作自己的合集和作品**
            - 不属于当前用户的作品会被自动过滤（不影响有效作品的处理）
            - 已删除的作品会被自动过滤
            - 建议单次批量添加不超过 100 个作品
            - 添加操作不会改变作品的审核状态
            - 作品原已属于其他合集时，将被移动到新合集
            - 空作品列表会报错，请确保至少有一个有效作品
            - 作品 ID 以逗号分隔传入，如 workIds=1,2,3
            """
    )
    public ResponsePojo<Boolean> batchAddWorksToSeries(
        @Parameter(description = "HTTP 请求对象，用于从 Header 或 URL 参数中获取 Token", required = true) HttpServletRequest request,
        @Parameter(description = "作品 ID 列表，逗号分隔，如 1,2,3", required = true, example = "1,2,3") @RequestParam String workIds,
        @Parameter(description = "合集 ID", required = true, example = "1") @RequestParam Integer seriesId
    ) {
        log.debug("批量添加作品到合集 - 合集 ID: {}, 作品 ID 字符串: {}", seriesId, workIds);

        // 提取 Token
        String token = JWTUtils.extractTokenWithLog(request, "批量添加作品到合集接口");

        if (token == null || token.isEmpty()) {
            log.error("批量添加作品到合集失败 - Token 不存在");
            return ResponsePojo.error(null, "Token 不存在，请在 Header 中添加 Authorization: Bearer <token> 或在 URL 参数中添加 ?token=<token>");
        }

        // 检查 Token 是否在白名单中
        if (!tokenWhitelistService.isInWhitelist(token)) {
            log.warn("Token 不在白名单中，可能已过期或被移除");
            return ResponsePojo.error(null, "Token 已失效");
        }

        // 从 Token 中获取用户 ID
        Integer userId = JWTUtils.getUserIdFromToken(token);
        if (userId == null) {
            log.error("从 Token 中解析用户 ID 失败");
            return ResponsePojo.error(null, "Token 无效");
        }

        String username = JWTUtils.getUsernameFromToken(token);
        log.info("开始批量添加作品到合集，用户 ID: {}, 用户名: {}, 合集 ID: {}, 作品 ID 字符串: {}", userId, username, seriesId, workIds);

        // 校验并解析作品 ID 列表
        List<Integer> workIdList;
        try {
            workIdList = parseWorkIds(workIds);
        } catch (IllegalArgumentException e) {
            log.warn("作品 ID 格式错误，用户 ID: {}, 作品 ID 字符串: {}", userId, workIds);
            return ResponsePojo.error(false, e.getMessage());
        }

        if (seriesId == null || seriesId <= 0) {
            log.warn("合集 ID 无效，用户 ID: {}", userId);
            return ResponsePojo.error(false, "合集 ID 无效");
        }

        try {
            Boolean result = seriesService.batchAddWorksToSeries(seriesId, workIdList, userId);

            if (result) {
                log.info("作品批量添加成功，用户 ID: {}, 用户名: {}, 合集 ID: {}, 作品数量: {}", userId, username, seriesId, workIdList.size());
                return ResponsePojo.success(true, "作品批量添加成功");
            } else {
                log.warn("作品批量添加失败，用户 ID: {}, 用户名: {}, 合集 ID: {}", userId, username, seriesId);
                return ResponsePojo.error(false, "作品批量添加失败");
            }
        } catch (IllegalArgumentException e) {
            log.warn("批量添加作品参数错误，用户 ID: {}, 合集 ID: {}, 错误: {}", userId, seriesId, e.getMessage());
            return ResponsePojo.error(false, e.getMessage());
        } catch (SecurityException e) {
            log.warn("批量添加作品权限错误，用户 ID: {}, 合集 ID: {}, 错误: {}", userId, seriesId, e.getMessage());
            return ResponsePojo.error(false, e.getMessage());
        }
    }

    /**
     * 批量从合集中移除作品
     *
     * @param request  HTTP 请求对象，用于从 Header 或 URL 参数中获取 Token
     * @param workIds  作品 ID 列表
     * @param seriesId 合集 ID
     * @return 响应数据，表示作品是否批量移除成功
     * @author PlayerEG
     */
    @PostMapping("/batch-remove-works")
    @RequireRole(value = {22, 77})
    @Operation(
        summary = "批量从合集移除作品接口",
        description = """
            # 批量从合集移除作品（需要登录认证 + 角色权限[22,77]）

            ## 特性
            - Token 认证（支持 Header 和 URL 参数两种方式）
            - 支持批量将多个作品从指定合集中移除
            - SQL 层面三重权限验证（合集归属 + 作品归属 + 系列归属）
            - 仅移除有效作品（未删除、属于当前用户、且当前在该合集中）
            - 不重置审核状态（仅清空 series_id）

            ## 参数说明：
            - Authorization: Header 中的 Token，格式为 `Bearer <token>`，或通过 URL 参数 `?token=<token>` 传递
            - workIds: **作品 ID 列表**，字符串类型，必填，逗号分隔，如 "1,2,3"，单次最多 100 个
            - seriesId: **合集 ID**，Integer 类型，必填

            ## 返回说明：
            - **移除成功**：返回 `{"data": true}` 和 "作品批量移除成功" 提示
            - **Token 不存在**：返回 `{"data": null}` 和 "Token 不存在" 提示
            - **Token 已失效**：返回 `{"data": null}` 和 "Token 已失效" 提示
            - **作品 ID 列表为空**：返回 `{"data": false}` 和 "作品 ID 列表不能为空" 提示
            - **作品 ID 格式错误**：返回 `{"data": false}` 和 "作品 ID 格式错误" 提示
            - **合集 ID 无效**：返回 `{"data": false}` 和 "合集 ID 无效" 提示
            - **合集不存在**：返回 `{"data": false}` 和 "合集不存在或已删除" 提示
            - **无权操作合集**：返回 `{"data": false}` 和 "无权操作该合集" 提示
            - **无可操作作品**：返回 `{"data": false}` 和 "没有可操作的作品" 提示
            - **作品不属于该合集**：返回 `{"data": false}` 和 "移除失败，作品可能不属于该合集" 提示
            - **移除失败**：返回 `{"data": false}` 和 "作品批量移除失败" 提示

            ## 业务逻辑：
            1. 从请求中提取并验证 Token
            2. 从 Token 中解析用户 ID
            3. 解析逗号分隔的作品 ID 字符串为列表
            4. 校验作品 ID 列表和合集 ID 参数有效性
            5. 验证合集是否存在且未删除
            6. 验证合集归属权（只能操作自己的合集）
            7. 查询所有作品并过滤有效作品（未删除且属于当前用户）
            8. 批量清空作品的 series_id 为 NULL（SQL 层面再次验证 user_id、series_id 和 is_delete）
            9. 如果 SQL 未更新任何行（作品不属于该合集），抛出异常
            10. 返回移除结果

            ## 注意事项：
            - **需要携带有效的 Token 才能操作**
            - Token 必须在白名单中（未过期、未登出）
            - **用户只能操作自己的合集和作品**
            - 不属于当前用户的作品会被自动过滤（不影响有效作品的处理）
            - 已删除的作品会被自动过滤
            - **只有当前属于该合集的作品才会被移除**，不属于该合集的作品在 SQL 层面被自动过滤
            - 建议单次批量移除不超过 100 个作品
            - 移除操作不会改变作品的审核状态
            - 移除后作品仍然存在，只是不再属于任何合集（series_id 变为 NULL）
            - 空作品列表会报错，请确保至少有一个有效作品
            - 作品 ID 以逗号分隔传入，如 workIds=1,2,3
            """
    )
    public ResponsePojo<Boolean> batchRemoveWorksFromSeries(
        @Parameter(description = "HTTP 请求对象，用于从 Header 或 URL 参数中获取 Token", required = true) HttpServletRequest request,
        @Parameter(description = "作品 ID 列表，逗号分隔，如 1,2,3", required = true, example = "1,2,3") @RequestParam String workIds,
        @Parameter(description = "合集 ID", required = true, example = "1") @RequestParam Integer seriesId
    ) {
        log.debug("批量从合集移除作品 - 合集 ID: {}, 作品 ID 字符串: {}", seriesId, workIds);

        // 提取 Token
        String token = JWTUtils.extractTokenWithLog(request, "批量从合集移除作品接口");

        if (token == null || token.isEmpty()) {
            log.error("批量从合集移除作品失败 - Token 不存在");
            return ResponsePojo.error(null, "Token 不存在，请在 Header 中添加 Authorization: Bearer <token> 或在 URL 参数中添加 ?token=<token>");
        }

        // 检查 Token 是否在白名单中
        if (!tokenWhitelistService.isInWhitelist(token)) {
            log.warn("Token 不在白名单中，可能已过期或被移除");
            return ResponsePojo.error(null, "Token 已失效");
        }

        // 从 Token 中获取用户 ID
        Integer userId = JWTUtils.getUserIdFromToken(token);
        if (userId == null) {
            log.error("从 Token 中解析用户 ID 失败");
            return ResponsePojo.error(null, "Token 无效");
        }

        String username = JWTUtils.getUsernameFromToken(token);
        log.info("开始批量从合集移除作品，用户 ID: {}, 用户名: {}, 合集 ID: {}, 作品 ID 字符串: {}", userId, username, seriesId, workIds);

        // 校验并解析作品 ID 列表
        List<Integer> workIdList;
        try {
            workIdList = parseWorkIds(workIds);
        } catch (IllegalArgumentException e) {
            log.warn("作品 ID 格式错误，用户 ID: {}, 作品 ID 字符串: {}", userId, workIds);
            return ResponsePojo.error(false, e.getMessage());
        }

        if (seriesId == null || seriesId <= 0) {
            log.warn("合集 ID 无效，用户 ID: {}", userId);
            return ResponsePojo.error(false, "合集 ID 无效");
        }

        try {
            Boolean result = seriesService.batchRemoveWorksFromSeries(seriesId, workIdList, userId);

            if (result) {
                log.info("作品批量移除成功，用户 ID: {}, 用户名: {}, 合集 ID: {}, 作品数量: {}", userId, username, seriesId, workIdList.size());
                return ResponsePojo.success(true, "作品批量移除成功");
            } else {
                log.warn("作品批量移除失败，用户 ID: {}, 用户名: {}, 合集 ID: {}", userId, username, seriesId);
                return ResponsePojo.error(false, "作品批量移除失败");
            }
        } catch (IllegalArgumentException e) {
            log.warn("批量移除作品参数错误，用户 ID: {}, 合集 ID: {}, 错误: {}", userId, seriesId, e.getMessage());
            return ResponsePojo.error(false, e.getMessage());
        } catch (SecurityException e) {
            log.warn("批量移除作品权限错误，用户 ID: {}, 合集 ID: {}, 错误: {}", userId, seriesId, e.getMessage());
            return ResponsePojo.error(false, e.getMessage());
        }
    }

    /**
     * 解析逗号分隔的作品 ID 字符串为 Integer 列表
     *
     * @param workIdsStr 逗号分隔的作品 ID 字符串，如 "1,2,3"
     * @return 作品 ID 列表
     * @throws IllegalArgumentException 格式无效时抛出
     */
    private List<Integer> parseWorkIds(String workIdsStr) {
        if (workIdsStr == null || workIdsStr.trim().isEmpty()) {
            throw new IllegalArgumentException("作品 ID 列表不能为空");
        }
        try {
            return Arrays.stream(workIdsStr.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Integer::parseInt)
                .collect(Collectors.toList());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("作品 ID 格式错误，请使用逗号分隔的整数，如 1,2,3");
        }
    }
}
