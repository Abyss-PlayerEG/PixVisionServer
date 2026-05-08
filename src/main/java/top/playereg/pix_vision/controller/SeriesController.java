package top.playereg.pix_vision.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import top.playereg.pix_vision.pojo.ResponsePojo;
import top.playereg.pix_vision.pojo.Series;
import top.playereg.pix_vision.service.SeriesService;
import top.playereg.pix_vision.service.TokenWhitelistService;
import top.playereg.pix_vision.util.Annotation.PublicAccess;
import top.playereg.pix_vision.util.Annotation.RequireRole;
import top.playereg.pix_vision.util.JWTUtils;
import top.playereg.pix_vision.util.PixVisionLogger;

import java.util.List;

/**
 * 系列管理控制器
 *
 * @author PlayerEG
 */
@Tag(name = "作品系列管理接口")
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
     * @param aboutText   系列描述文本（最多 24 个中文字符）
     * @return 新增的系列信息
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

            ## 参数说明：
            - Authorization: Header 中的 Token，格式为 `Bearer <token>`，或通过 URL 参数 `?token=<token>` 传递
            - seriesTitle: 系列标题，字符串类型，必填，长度不超过 16 个中文字符
            - aboutText: 系列描述文本，字符串类型，可选，长度不超过 24 个中文字符

            ## 返回说明：
            - **成功**：返回 **{"data": Series 对象}** 和提示信息
            - **Token 失效**：返回 **{"data": null}** 和 "Token 已失效" 提示
            - **标题为空**：返回 **{"data": false}** 和 "系列标题不能为空" 提示
            - **标题过长**：返回 **{"data": false}** 和 "系列标题长度不能超过 16 个字符" 提示
            - **描述过长**：返回 **{"data": false}** 和 "系列描述长度不能超过 24 个字符" 提示
            - **标题重复**：返回 **{"data": false}** 和 "系列标题已存在，请使用其他标题" 提示

            ## 业务逻辑：
            1. 从请求中提取并验证 Token
            2. 检查 Token 是否在白名单中
            3. 从 Token 中解析用户 ID
            4. 校验系列标题和描述的长度
            5. **检查系列标题是否已存在（同一用户下不能重复）**
            6. 创建系列对象并设置用户 ID、时间戳等信息
            7. 插入数据库并返回结果

            ## 注意事项：
            - 系列标题**必填**，长度不超过 16 个字符
            - 系列描述**可选**，长度不超过 24 个字符
            - **同一用户下系列标题不能重复**
            - 系统自动记录创建者 ID 和创建时间
            - 返回的 Series 对象包含自动生成的 series_id
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
            Series series = seriesService.addSeries(userId, seriesTitle, aboutText);

            if (series != null) {
                log.info("系列新增成功，系列 ID: {}, 用户 ID: {}", series.getSeries_id(), userId);
                return ResponsePojo.success(true, "系列新增成功");
            } else {
                log.error("系列新增失败，用户 ID: {}", userId);
                return ResponsePojo.error(false, "系列新增失败");
            }
        } catch (IllegalArgumentException e) {
            log.warn("系列新增参数错误，用户 ID: {}, 错误: {}", userId, e.getMessage());
            return ResponsePojo.error(false, e.getMessage());
        }
    }

    /**
     * 查询用户的所有作品系列
     *
     * @param userId 用户 ID
     * @return 作品系列列表
     * @author PlayerEG
     */
    @GetMapping("/list")
    @PublicAccess("查询用户作品系列，无需认证")
    @Operation(
        summary = "查询用户作品系列接口",
        description = """
            # 查询用户作品系列（无需登录认证）

            ## 特性
            - 公开接口（无需 Token 认证）
            - 自动过滤逻辑删除数据
            - 按创建时间倒序排列

            ## 参数说明：
            - userId: 用户 ID，Integer 类型，必填

            ## 返回说明：
            - **查询成功**：返回 **{"data": [Series 列表]}** ，包含用户的所有作品系列
            - **用户 ID 无效**：返回 **{"data": null}** 和"用户 ID 无效"提示
            - **查询失败**：返回 **{"data": null}** 和"查询失败"提示

            ## 业务逻辑：
            1. 校验用户 ID 参数有效性
            2. 查询用户的所有作品系列（自动排除逻辑删除的数据）
            3. 按创建时间倒序返回结果
            4. 返回系列列表

            ## 注意事项：
            - **此接口为公开接口，无需登录即可访问**
            - 自动过滤已逻辑删除的数据（is_delete=0）
            - 返回结果按创建时间倒序排列（最新的在前）
            - 如果用户没有作品系列，返回空列表 []
            """
    )
    public ResponsePojo<List<Series>> getSeriesList(
        @Parameter(description = "用户 ID", required = true, example = "1") @RequestParam Integer userId
    ) {
        log.debug("查询用户作品系列 - 用户 ID: {}", userId);

        // 参数校验
        if (userId == null || userId <= 0) {
            log.warn("用户 ID 无效: {}", userId);
            return ResponsePojo.error(null, "用户 ID 无效");
        }

        // 调用服务层查询
        List<Series> seriesList = seriesService.getSeriesByUserId(userId);

        if (seriesList != null) {
            log.info("查询成功 - 用户 ID: {}, 系列数量: {}", userId, seriesList.size());
            return ResponsePojo.success(seriesList, "查询成功");
        } else {
            log.error("查询失败 - 用户 ID: {}", userId);
            return ResponsePojo.error(null, "查询失败");
        }
    }

    /**
     * 删除作品系列（支持保留或删除系列内作品）
     *
     * @param request     HTTP 请求对象，用于从 Header 或 URL 参数中获取 Token
     * @param seriesId    系列 ID
     * @param deleteWorks 是否删除系列内的作品（true=删除作品，false=保留作品但移除系列关联）
     * @return 删除结果
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
     * @return 修改结果
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

            ## 参数说明：
            - Authorization: Header 中的 Token，格式为 `Bearer <token>`，或通过 URL 参数 `?token=<token>` 传递
            - seriesId: **系列 ID**，Integer 类型，必填
            - seriesTitle: **系列标题**，String 类型，可选，最多 16 个中文字符
            - aboutText: **系列描述**，String 类型，可选，最多 24 个中文字符

            ## 返回说明：
            - **修改成功**：返回 **{"data": true}** 和“系列信息更新成功”提示
            - **Token 不存在**：返回 **{"data": null}** 和“Token 不存在”提示
            - **Token 已失效**：返回 **{"data": null}** 和“Token 已失效”提示
            - **系列 ID 无效**：返回 **{"data": false}** 和“系列 ID 无效”提示
            - **无修改内容**：返回 **{"data": false}** 和“无修改内容”提示
            - **系列不存在**：返回 **{"data": false}** 和“系列不存在或已删除”提示
            - **无权修改**：返回 **{"data": false}** 和“无权修改该系列”提示
            - **标题过长**：返回 **{"data": false}** 和“系列标题长度不能超过 16 个字符”提示
            - **描述过长**：返回 **{"data": false}** 和“系列描述长度不能超过 24 个字符”提示
            - **标题重复**：返回 **{"data": false}** 和“系列标题已存在，请使用其他标题”提示

            ## 业务逻辑：
            1. 从请求头或 URL 参数中提取 Token（支持 Bearer 前缀）
            2. 验证 Token 是否在白名单中
            3. 从 Token 中解析用户 ID
            4. 校验系列 ID 参数有效性
            5. 检查是否至少有一个参数不为空
            6. 查询系列信息并验证所有权（只能修改自己的系列）
            7. 如果提供了新标题，验证长度并检查是否与当前标题不同且未被其他系列使用
            8. 如果提供了新描述，验证长度并检查是否与当前描述不同
            9. 执行动态更新（只更新非空且发生变化的字段）
            10. 自动更新 update_time 和 update_user
            11. 返回修改结果

            ## 注意事项：
            - **需要携带有效的 Token 才能修改系列**
            - Token 必须在白名单中（未过期、未登出）
            - **用户只能修改自己的系列**，无法修改他人的系列
            - 所有参数都是可选的，可以只修改部分字段
            - **如果所有参数都为空或与当前值相同，将返回“无修改内容”**
            - 系列标题限制：**最多 16 个字符**
            - 系列描述限制：**最多 24 个字符**
            - **同一用户下系列标题不能重复**（修改时也会检查）
            - 采用动态更新机制，只提供要修改的字段即可
            - 修改成功后，update_time 和 update_user 会自动更新
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
            Boolean result = seriesService.updateSeriesInfo(seriesId, userId, seriesTitle, aboutText);

            if (result) {
                log.info("系列信息更新成功，用户 ID: {}, 用户名: {}, 系列 ID: {}", userId, username, seriesId);
                return ResponsePojo.success(true, "系列信息更新成功");
            } else {
                log.error("系列信息更新失败，用户 ID: {}, 用户名: {}, 系列 ID: {}", userId, username, seriesId);
                return ResponsePojo.error(false, "系列信息更新失败");
            }
        } catch (IllegalArgumentException e) {
            log.warn("系列信息更新参数错误，用户 ID: {}, 错误: {}", userId, e.getMessage());
            return ResponsePojo.error(false, e.getMessage());
        } catch (SecurityException e) {
            log.warn("系列信息更新权限错误，用户 ID: {}, 错误: {}", userId, e.getMessage());
            return ResponsePojo.error(false, e.getMessage());
        }
    }
}
