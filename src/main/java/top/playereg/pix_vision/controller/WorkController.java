package top.playereg.pix_vision.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import top.playereg.pix_vision.pojo.ResponsePojo;
import top.playereg.pix_vision.pojo.Works;
import top.playereg.pix_vision.service.TokenWhitelistService;
import top.playereg.pix_vision.service.WorkService;
import top.playereg.pix_vision.util.Annotation.PublicAccess;
import top.playereg.pix_vision.util.Annotation.RequireRole;
import top.playereg.pix_vision.util.JWTUtils;
import top.playereg.pix_vision.util.PixVisionLogger;

import java.util.List;

/**
 * 作品控制器 - 提供作品相关的接口
 *
 * @author PlayerEG
 */
@RestController
@RequestMapping("/api/work")
@RequiredArgsConstructor
@Tag(name = "作品接口")
public class WorkController {
    private static final PixVisionLogger log = PixVisionLogger.create(WorkController.class);

    private final WorkService workService;
    private final TokenWhitelistService tokenWhitelistService;

    /**
     * 删除作品（支持单条和批量删除，需要登录）
     *
     * @param request HTTP 请求对象，用于从 Header 或 URL 参数中获取 Token
     * @param workIds 要删除的作品 ID 列表（支持单个或多个）
     * @return 响应数据，表示作品是否删除成功
     * @author PlayerEG
     */
    @Operation(
        summary = "删除作品接口",
        description = """
            # 删除作品（需要登录认证 + 角色权限[22,77]）

            ## 特性
            - Token 认证（支持 Header 和 URL 参数两种方式）
            - 支持单条/批量删除
            - SQL 层面权限验证（只能删除自己的作品）
            - 逻辑删除（数据不真正从数据库移除）
            - 文件重命名（将 .png/.jpg 改为 .del）

            ## 参数说明：
            - Authorization: Header 中的 Token，格式为 `Bearer <token>`，或通过 URL 参数 `?token=<token>` 传递
            - workIds: 要删除的作品 ID 列表，整形数组类型，必填
              * 单条删除：传入 [1]
              * 批量删除：传入 [1, 2, 3]

            ## 返回说明：
            - **删除成功**：返回 **{"data": true}** 和“作品删除成功”提示
            - **Token 不存在**：返回 **{"data": null}** 和“Token 不存在”提示
            - **Token 已失效**：返回 **{"data": null}** 和“Token 已失效”提示
            - **作品 ID 列表为空**：返回 **{"data": false}** 和“作品 ID 列表不能为空”提示
            - **无权删除**：返回 **{"data": false}** 和“部分或全部作品无权删除”提示（作品不属于当前用户）
            - **删除失败**：返回 **{"data": false}** 和“作品删除失败”提示

            ## 业务逻辑：
            1. 从请求头或 URL 参数中提取 Token（支持 Bearer 前缀）
            2. 验证 Token 是否在白名单中
            3. 从 Token 中解析用户 ID
            4. 校验作品 ID 列表参数有效性
            5. 检查用户是否存在
            6. 查询作品信息并验证所有权（只能删除自己的作品）
            7. 将作品文件后缀名改为 .del（如 123.png → 123.png.del）
            8. 执行批量逻辑删除（SQL 层面验证 user_id，确保只能删除自己的作品）
            9. 返回删除结果

            ## 注意事项：
            - **需要携带有效的 Token 才能删除作品**
            - Token 必须在白名单中（未过期、未登出）
            - **用户只能删除自己的作品**，无法删除他人的作品
            - 采用逻辑删除方式，数据不会真正从数据库中移除
            - 作品文件会被重命名为 .del 后缀（如 123.png → 123.png.del）
            - 如果部分作品不属于当前用户，只会删除属于当前用户的作品
            - 删除后，这些作品在查询接口中将不再显示
            - 建议单次批量删除不超过 100 个作品
            - 单条删除时传入单个元素的数组，如 [1]
            """
    )
    @RequireRole(value = {22, 77})
    @PostMapping("/delete")
    public ResponsePojo<Boolean> deleteWorks(
        @Parameter(description = "HTTP 请求对象，用于从 Header 或 URL 参数中获取 Token", required = true) HttpServletRequest request,
        @Parameter(description = "要删除的作品 ID 列表（支持单条或多条）", required = true, example = "1,2,3") @RequestParam List<Integer> workIds
    ) {
        log.debug("删除作品 - 作品 ID: {}", workIds);

        // 提取 Token
        String token = JWTUtils.extractTokenWithLog(request, "删除作品接口");

        if (token == null || token.isEmpty()) {
            log.error("删除作品失败 - Token 不存在");
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
        int workCount = workIds != null ? workIds.size() : 0;
        log.info("开始删除作品，用户 ID: {}, 用户名: {}, 作品 ID 数量: {}", userId, username, workCount);

        // 校验作品 ID 列表参数
        if (workIds == null || workIds.isEmpty()) {
            log.warn("作品 ID 列表为空，用户 ID: {}", userId);
            return ResponsePojo.error(false, "作品 ID 列表不能为空");
        }

        // 调用服务层批量删除作品（统一使用批量方法）
        Boolean result = workService.batchDeleteWorks(workIds, userId);

        if (result) {
            String successMsg = workCount == 1 ? "作品删除成功" : "批量删除作品成功";
            log.info("{}，用户 ID: {}, 用户名: {}, 删除数量: {}", successMsg, userId, username, workCount);
            return ResponsePojo.success(true, successMsg);
        } else {
            log.warn("作品删除失败，用户 ID: {}, 用户名: {}", userId, username);
            return ResponsePojo.error(false, "作品删除失败");
        }
    }

    /**
     * 分页查询作品列表（支持多条件查询）
     *
     * @param current    当前页码（从 1 开始）
     * @param size       每页大小（范围 1-100）
     * @param workTitle  作品标题（可选，模糊查询）
     * @param userId     用户 ID（可选，精确查询）
     * @param seriesId   系列 ID（可选，精确查询）
     * @param isOriginal 是否原创（可选，精确查询）
     * @return 响应数据，包含分页的作品列表
     * @author PlayerEG
     */
    @Operation(
        summary = "分页查询作品列表",
        description = """
            # 分页查询作品列表（无需登录认证）

            ## 特性
            - 公开接口（无需认证）
            - MyBatis-Plus 分页支持
            - 多条件组合查询（作品标题/用户 ID/系列 ID/是否原创）
            - 模糊匹配与精确匹配
            - 仅返回未删除且审核通过的作品
            - 按创建时间倒序排列（最新作品优先）

            ## 参数说明：
            - current: **当前页码**，Long 类型，必填，**从 1 开始**，默认为 1
            - size: **每页大小**，Long 类型，必填，范围 1-100，默认为 10
            - workTitle: **作品标题**（可选），String 类型，支持模糊查询
            - userId: **用户 ID**（可选），Integer 类型，支持精确查询
            - seriesId: **系列 ID**（可选），Integer 类型，支持精确查询
            - isOriginal: **是否原创**（可选），Boolean 类型，支持精确查询（true=原创，false=转载）

            ## 返回说明：
            - **查询成功**：返回 **{"data": {IPage<Works>对象}}** ，包含作品列表和分页信息
            - **参数错误**：返回 **{"data": null}** 和"页码或每页大小错误"提示
            - **无数据**：返回 **{"data": null}** 和"查询失败，返回结果为空"提示

            ## 业务逻辑：
            1. 校验页码和每页大小参数（current>=1, 1<=size<=100）
            2. 构建 MyBatis-Plus 分页对象
            3. 根据条件查询作品信息（支持多条件组合）
            4. 仅返回未删除且审核通过的作品（is_delete = 0 AND approval_status = 10）
            5. 按创建时间倒序排列（create_time DESC）
            6. 返回分页结果集（IPage<Works>）

            ## 注意事项：
            - 所有查询条件均为**可选参数**，可不传
            - 支持多个条件组合查询
            - 作品标题支持**模糊匹配**
            - 用户 ID、系列 ID 和是否原创支持**精确匹配**
            - 默认返回完整 Works 实体字段
            - 已自动过滤逻辑删除的作品（is_delete=0）
            - **仅返回审核通过的作品**（approval_status=10），待审核和未过审的作品不会显示
            - 每页大小限制：**1-100**
            - 图片 URL 为文件名，完整访问路径为：`/api/image/works?filePath={img_url}`
            - 使用 **RESTful 风格**路径参数，格式：`/homepage/{current}/{size}`
            """
    )
    @PublicAccess("分页查询作品列表，无需认证")
    @GetMapping("/page/{current}/{size}")
    public ResponsePojo<IPage<Works>> getHomepageWorks(
        @Parameter(description = "当前页码，从 1 开始", required = true, example = "1") @PathVariable Long current,
        @Parameter(description = "每页大小，范围 1-100", required = true, example = "10") @PathVariable Long size,
        @Parameter(description = "作品标题（可选），支持模糊查询") @RequestParam(required = false) String workTitle,
        @Parameter(description = "用户 ID（可选），支持精确查询") @RequestParam(required = false) Integer userId,
        @Parameter(description = "系列 ID（可选），支持精确查询") @RequestParam(required = false) Integer seriesId,
        @Schema(description = "是否原创（可选）", allowableValues = {"true", "false"}) @RequestParam(required = false) Boolean isOriginal
    ) {
        // 参数校验
        if (current == null || current < 1) {
            return ResponsePojo.error(null, "页码必须大于 0");
        }
        if (size == null || size < 1 || size > 100) {
            return ResponsePojo.error(null, "每页大小必须在 1-100 之间");
        }

        // 构建分页对象
        Page<Works> page = new Page<>(current, size);

        // 调用服务层查询作品列表
        IPage<Works> result = workService.selectHomepageWorks(page, workTitle, userId, seriesId, isOriginal);

        log.info("分页查询成功 - 页码：{}, 每页：{}, 总数：{}, 返回：{}",
            current, size, result.getTotal(), result.getRecords().size());

        return ResponsePojo.success(result, "查询成功");
    }

    /**
     * 根据 ID 查询单个作品
     *
     * @param request HTTP 请求对象，用于从 Header 或 URL 参数中获取 Token（可选，提供则记录访问历史）
     * @param workId  作品 ID
     * @return 响应数据，包含作品详细信息
     * @author PlayerEG
     */
    @Operation(
        summary = "查询单个作品",
        description = """
            # 查询单个作品（无需登录认证）

            ## 特性
            - 公开接口（无需认证）
            - 根据作品 ID 精确查询
            - 仅返回未删除且审核通过的作品
            - 返回完整的 Works 实体字段

            ## 参数说明：
            - Authorization: Header 中的 Token（可选），格式为 `Bearer <token>`，或通过 URL 参数 `?token=<token>` 传递。如果提供有效 Token，将记录访问历史。
            - workId: **作品 ID**，Integer 类型，必填

            ## 返回说明：
            - **查询成功**：返回 **{"data": {Works对象}}** ，包含作品详细信息
            - **作品不存在**：返回 **{"data": null}** 和"作品不存在或已删除"提示
            - **参数错误**：返回 **{"data": null}** 和"作品 ID 无效"提示

            ## 业务逻辑：
            1. 校验作品 ID 参数有效性
            2. 查询作品信息
            3. 验证作品是否存在、未删除且审核通过
            4. 返回作品详细信息

            ## 注意事项：
            - 这是一个**公开接口**，无需 Token 认证
            - 只能查询**未删除且审核通过**的作品（is_delete=0 AND approval_status=10）
            - 待审核（approval_status=20）和未过审（approval_status=30）的作品无法查询
            - 图片 URL 为文件名，完整访问路径为：`/api/image/works?filePath={img_url}`
            - 如果作品不存在、已删除或未审核通过，返回 null
            """
    )
    @PublicAccess("查询单个作品，无需认证")
    @GetMapping("/detail/{workId}")
    public ResponsePojo<Works> getWorkById(
        @Parameter(description = "HTTP 请求对象，用于从 Header 或 URL 参数中获取 Token", required = true) HttpServletRequest request,
        @Parameter(description = "作品 ID", required = true, example = "1") @PathVariable Integer workId
    ) {
        // 参数校验
        if (workId == null || workId <= 0) {
            log.warn("作品 ID 无效: {}", workId);
            return ResponsePojo.error(null, "作品 ID 无效");
        }

        // 调用服务层查询作品
        Works work = workService.getWorkById(workId);

        // 返回结果为空，则返回错误信息
        if (work == null) {
            log.warn("作品不存在或已删除，作品 ID: {}", workId);
            return ResponsePojo.error(null, "作品不存在或已删除");
        }

        // 增加浏览次数（异步执行，不影响主流程）
        try {
            workService.incrementViewCount(workId);
        } catch (Exception e) {
            // 即使增加浏览次数失败，也不影响查询结果
            log.error("增加浏览次数异常，作品 ID: {}, 错误: {}", workId, e.getMessage());
        }

        // 记录访问历史（如果提供了有效的 Token）
        String token = JWTUtils.extractTokenWithLog(request, "查询作品详情接口");
        if (token != null && !token.isEmpty()) {
            Integer userId = JWTUtils.getUserIdFromToken(token);
            if (userId != null) {
                try {
                    workService.addHistory(userId, workId);
                } catch (Exception e) {
                    log.error("添加历史记录异常，用户 ID: {}, 作品 ID: {}, 错误: {}", userId, workId, e.getMessage());
                }
            }
        }

        log.info("查询作品成功，作品 ID: {}, 标题: {}", workId, work.getWork_title());
        return ResponsePojo.success(work, "查询成功");
    }

    /**
     * 修改作品信息（支持部分字段修改）
     *
     * @param request    HTTP 请求对象，用于从 Header 或 URL 参数中获取 Token
     * @param workId     作品 ID
     * @param workTitle  作品标题（可选，最多 16 个中文字符）
     * @param file       新的图片文件（可选，MultipartFile 类型）
     * @param seriesId   系列 ID（可选，0 表示不属于任何系列）
     * @param isOriginal 是否原创（可选）
     * @param outUrl     外部转载链接（可选）
     * @return 响应数据，表示作品是否修改成功
     * @author PlayerEG
     */
    @Operation(
        summary = "修改作品信息接口",
        description = """
            # 修改作品信息（需要登录认证 + 角色权限[22,77]）

            ## 特性
            - Token 认证（支持 Header 和 URL 参数两种方式）
            - 支持部分字段修改（所有参数可选）
            - 支持重新上传图片（MultipartFile）
            - SQL 层面权限验证（只能修改自己的作品）
            - 动态更新非空字段
            - 完整的参数校验
            - 自动删除旧图片文件

            ## 参数说明：
            - Authorization: Header 中的 Token，格式为 `Bearer <token>`，或通过 URL 参数 `?token=<token>` 传递
            - workId: **作品 ID**，Integer 类型，必填
            - workTitle: **作品标题**，String 类型，可选，最多 16 个中文字符（48 字节）
            - file: **新的图片文件**，MultipartFile 类型，可选，仅支持 JPG/JPEG/PNG 格式，最大 32MB
            - seriesId: **系列 ID**，Integer 类型，可选，传入 0 表示不属于任何系列（清空系列），传入正整数表示关联到对应系列
            - isOriginal: **是否原创**，Boolean 类型，可选，下拉框选择（true=原创，false=转载）
            - outUrl: **外部转载链接**，String 类型，可选，isOriginal=false 时必填

            ## 返回说明：
            - **修改成功**：返回 **{"data": true}** 和“作品修改成功”提示
            - **无修改内容**：返回 **{"data": null}** 和“无修改内容”提示（所有参数均为空）
            - **Token 不存在**：返回 **{"data": null}** 和“Token 不存在”提示
            - **Token 已失效**：返回 **{"data": null}** 和“Token 已失效”提示
            - **作品 ID 无效**：返回 **{"data": false}** 和“作品 ID 无效”提示
            - **无权修改**：返回 **{"data": false}** 和“无权修改该作品”提示（作品不属于当前用户）
            - **作品不存在**：返回 **{"data": false}** 和“作品不存在或已删除”提示
            - **标题过长**：返回 **{"data": false}** 和“作品标题过长”提示
            - **文件格式错误**：返回 **{"data": false}** 和“不支持的文件格式”提示
            - **文件大小超限**：返回 **{"data": false}** 和“文件大小超出限制”提示
            - **原创作品填写链接**：返回 **{"data": false}** 和“原创作品不能填写外部链接”提示
            - **转载缺少链接**：返回 **{"data": false}** 和“转载作品必须提供外部链接”提示
            - **URL 格式错误**：返回 **{"data": false}** 和“外部链接格式不正确”提示

            ## 业务逻辑：
            1. 从请求头或 URL 参数中提取 Token（支持 Bearer 前缀）
            2. 验证 Token 是否在白名单中
            3. 从 Token 中解析用户 ID
            4. 校验作品 ID 参数有效性
            5. 检查是否所有修改参数都为空，如果是则返回"无修改内容"
            6. 查询作品信息并验证所有权（只能修改自己的作品）
            7. **如果提供了新图片文件**：
               - 验证文件格式（JPG/JPEG/PNG）
               - 验证文件大小（最大 32MB）
               - 验证图片真实性（魔数检查）
               - 生成唯一文件名（UUID）并保存
               - 删除旧图片文件（重命名为 .del）
            8. **验证作品标题**（如果提供）：
               - 长度不超过 48 字节（16 个中文字符）
               - 去除首尾空格
            9. **处理系列 ID**（如果提供）：
               - **seriesId = 0**：将作品的 series_id 设置为 NULL（不属于任何系列）
               - **seriesId > 0**：验证系列是否存在且属于当前用户，然后关联到该系列
               - **seriesId = null**：不修改系列 ID
            10. **处理原创/转载逻辑**：
               - **设置为原创（isOriginal=true）**：
                 * 不能填写 outUrl，否则报错
                 * 自动将 outUrl 清空为空字符串 ""（即使原来有转载链接）
               - **设置为转载（isOriginal=false）**：
                 * 必须提供有效的 outUrl
                 * 验证 URL 格式
               - **不修改原创状态（isOriginal=null）**：
                 * 可以单独修改 outUrl
                 * 如果提供了 outUrl，验证 URL 格式
            11. 执行动态更新（只更新非空字段）
            12. 返回修改结果

            ## 注意事项：
            - **需要携带有效的 Token 才能修改作品**
            - Token 必须在白名单中（未过期、未登出）
            - **用户只能修改自己的作品**，无法修改他人的作品
            - 所有参数都是可选的，可以只修改部分字段
            - **如果所有参数都为空或空字符串，将返回"无修改内容"**
            - 作品标题限制：**最多 16 个中文字符**（48 字节）
            - 图片文件仅支持：**JPG、JPEG、PNG** 格式
            - 图片文件大小限制：**最大 32MB**
            - **系列 ID 特殊规则**：
              * seriesId = 0：将作品从系列中移除（series_id 设为 NULL）
              * seriesId > 0：必须验证系列存在且属于当前用户
              * seriesId = null：不修改系列 ID
            - **原创与转载链接约束**：
              * 原创作品（isOriginal=true）：不能填写 outUrl，系统会自动清空原有的转载链接
              * 转载作品（isOriginal=false）：必须提供有效的 outUrl
            - 采用动态更新机制，只提供要修改的字段即可
            - 上传新图片后，旧图片会被自动删除（重命名为 .del）
            - 修改成功后，update_time 和 update_user 会自动更新
            - isOriginal 参数使用下拉框选择，避免输入错误
            """
    )
    @RequireRole(value = {22, 77})
    @PostMapping("/update")
    public ResponsePojo<Boolean> updateWork(
        @Parameter(description = "HTTP 请求对象，用于从 Header 或 URL 参数中获取 Token", required = true) HttpServletRequest request,
        @Parameter(description = "作品 ID", required = true, example = "1") @RequestParam Integer workId,
        @Parameter(description = "作品标题（可选，最多 16 个中文字符）", required = false, example = "我的新作品") @RequestParam(required = false) String workTitle,
        @Parameter(description = "新的图片文件（可选，仅支持 JPG/JPEG/PNG，最大 32MB）", required = false) @RequestParam(required = false) MultipartFile file,
        @Parameter(description = "系列 ID（可选，0 表示不属于任何系列）", required = false, example = "5") @RequestParam(required = false) Integer seriesId,
        @Schema(description = "是否原创", allowableValues = {"true", "false"}, example = "true") @RequestParam(required = false) Boolean isOriginal,
        @Parameter(description = "外部转载链接（可选，isOriginal=false 时必填）", required = false, example = "https://example.com") @RequestParam(required = false) String outUrl
    ) {
        log.debug("修改作品 - 作品 ID: {}", workId);

        // 提取 Token
        String token = JWTUtils.extractTokenWithLog(request, "修改作品接口");

        if (token == null || token.isEmpty()) {
            log.error("修改作品失败 - Token 不存在");
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
        log.info("开始修改作品，用户 ID: {}, 用户名: {}, 作品 ID: {}", userId, username, workId);

        // 校验作品 ID 参数
        if (workId == null || workId <= 0) {
            log.warn("作品 ID 无效，用户 ID: {}", userId);
            return ResponsePojo.error(false, "作品 ID 无效");
        }

        try {
            // 调用服务层修改作品
            Boolean result = workService.updateWork(workId, userId, workTitle, file, seriesId, isOriginal, outUrl);

            // 检查是否为无修改内容
            if (result == null) {
                log.info("无修改内容，用户 ID: {}, 用户名: {}, 作品 ID: {}", userId, username, workId);
                return ResponsePojo.error(null, "无修改内容");
            }

            if (result) {
                log.info("作品修改成功，用户 ID: {}, 用户名: {}, 作品 ID: {}", userId, username, workId);
                return ResponsePojo.success(true, "作品修改成功");
            } else {
                log.warn("作品修改失败，用户 ID: {}, 用户名: {}, 作品 ID: {}", userId, username, workId);
                return ResponsePojo.error(false, "作品修改失败");
            }
        } catch (IllegalArgumentException e) {
            log.warn("参数验证失败，用户 ID: {}, 作品 ID: {}, 错误: {}", userId, workId, e.getMessage());
            return ResponsePojo.error(false, e.getMessage());
        } catch (SecurityException e) {
            log.warn("权限验证失败，用户 ID: {}, 作品 ID: {}, 错误: {}", userId, workId, e.getMessage());
            return ResponsePojo.error(false, e.getMessage());
        } catch (Exception e) {
            log.error("作品修改异常，用户 ID: {}, 作品 ID: {}, 错误: {}", userId, workId, e.getMessage(), e);
            return ResponsePojo.error(false, "作品修改失败：" + e.getMessage());
        }
    }
}
