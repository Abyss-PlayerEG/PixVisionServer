package top.playereg.pix_vision.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import top.playereg.pix_vision.pojo.ResponsePojo;
import top.playereg.pix_vision.pojo.Works;
import top.playereg.pix_vision.service.TokenWhitelistService;
import top.playereg.pix_vision.service.WorkService;
import top.playereg.pix_vision.util.Annotation.PublicAccess;
import top.playereg.pix_vision.util.Annotation.RequireRole;
import top.playereg.pix_vision.util.JWTUtils;

import java.util.List;

/**
 * 作品控制器 - 提供作品相关的接口
 *
 * @author PlayerEG
 */
@RestController
@RequestMapping("/api/work")
@RequiredArgsConstructor
@Tag(name = "作品管理相关接口")
public class WorkController {
    private static final Logger log = LoggerFactory.getLogger(WorkController.class);

    private final WorkService workService;
    private final TokenWhitelistService tokenWhitelistService;

    /**
     * 删除作品（支持单条和批量删除，需要登录）
     *
     * @param request HTTP 请求对象，用于从 Header 或 URL 参数中获取 Token
     * @param workIds 要删除的作品 ID 列表（支持单个或多个）
     * @return 删除结果
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
    @RequireRole({22, 77})
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
     * 获取首页作品列表（分页）
     *
     * @param current 页码，从 1 开始
     * @param size    每页大小，范围 1-100
     * @return 分页作品列表
     * @author PlayerEG
     */
    @Operation(
        summary = "获取首页作品列表",
        description = """
            # 获取首页作品列表（无需登录认证）

            ## 特性
            - 公开接口（无需认证）
            - 分页查询
            - 仅返回未删除的作品
            - 按创建时间倒序排列（最新作品优先）
            - 返回完整作品实体信息

            ## 参数说明：
            - current: **页码**，长整数类型，必填，从 1 开始
            - size: **每页大小**，长整数类型，必填，范围 1-100

            ## 返回说明：
            - **查询成功**：返回 **{"data": IPage<Works>}** 和提示信息
              - records: 作品列表（包含完整 Works 实体字段）
              - total: 总记录数
              - current: 当前页码
              - size: 每页大小
              - pages: 总页数
            - **参数错误**：返回 **{"data": null}** 和错误提示
            - **无数据**：返回 **{"data": null}** 和“查询失败，返回结果为空”提示

            ## 业务逻辑：
            1. 校验分页参数（current >= 1, size 在 1-100 范围内）
            2. 构建 MyBatis-Plus 分页对象
            3. 查询未删除的作品（is_delete = false）
            4. 按创建时间倒序排列（create_time DESC）
            5. 返回分页结果集（IPage<Works>）

            ## 注意事项：
            - 该接口**无需认证**，任何人都可以访问
            - 仅返回**未删除**的作品（is_delete = false）
            - 作品按**创建时间倒序**排列，最新作品在前
            - 图片 URL 为文件名，完整访问路径为：`/api/image/works?filePath={img_url}`
            - 每页大小限制：**1-100**，超出范围会返回错误
            - 返回完整的 Works 实体，包含所有字段
            - 使用 **RESTful 风格**路径参数，格式：`/homepage/{current}/{size}`

            ## 使用示例：
            ```
            # 获取第 1 页，每页 10 条
            GET /api/work/homepage/1/10

            # 获取第 2 页，每页 20 条
            GET /api/work/homepage/2/20

            # 获取第 1 页，每页 50 条
            GET /api/work/homepage/1/50
            ```
            """
    )
    @PublicAccess("获取首页作品列表，无需认证")
    @GetMapping("/homepage/{current}/{size}")
    public ResponsePojo<IPage<Works>> getHomepageWorks(
        @Parameter(description = "页码，从 1 开始", required = true, example = "1")
        @PathVariable Long current,
        @Parameter(description = "每页大小，范围 1-100", required = true, example = "10")
        @PathVariable Long size
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

        // 调用服务层查询首页作品列表
        IPage<Works> result = workService.selectHomepageWorks(page);

        // 返回结果为空，则返回错误信息
        if (result == null || result.getRecords().isEmpty()) {
            log.warn("分页查询返回结果为空 - 页码：{}, 每页：{}", current, size);
            return ResponsePojo.error(null, "查询失败，返回结果为空");
        }

        log.info("分页查询成功 - 页码：{}, 每页：{}, 总数：{}, 返回：{}",
            current, size, result.getTotal(), result.getRecords().size());

        return ResponsePojo.success(result, "查询成功");
    }

    /**
     * 修改作品信息（支持部分字段修改）
     *
     * @param request    HTTP 请求对象，用于从 Header 或 URL 参数中获取 Token
     * @param workId     作品 ID
     * @param workTitle  作品标题（可选，最多 16 个中文字符）
     * @param file       新的图片文件（可选，MultipartFile 类型）
     * @param isOriginal 是否原创（可选）
     * @param outUrl     外部转载链接（可选）
     * @return 修改结果
     * @author PlayerEG
     */
    @Operation(
        summary = "修改作品信息接口",
        description = """
            # 修改作品信息（需要登录认证）

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
            5. 检查是否所有修改参数都为空，如果是则返回“无修改内容”
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
            9. **处理原创/转载逻辑**：
               - **设置为原创（isOriginal=true）**：
                 * 不能填写 outUrl，否则报错
                 * 自动将 outUrl 清空为空字符串 ""（即使原来有转载链接）
               - **设置为转载（isOriginal=false）**：
                 * 必须提供有效的 outUrl
                 * 验证 URL 格式
               - **不修改原创状态（isOriginal=null）**：
                 * 可以单独修改 outUrl
                 * 如果提供了 outUrl，验证 URL 格式
            10. 执行动态更新（只更新非空字段）
            11. 返回修改结果

            ## 注意事项：
            - **需要携带有效的 Token 才能修改作品**
            - Token 必须在白名单中（未过期、未登出）
            - **用户只能修改自己的作品**，无法修改他人的作品
            - 所有参数都是可选的，可以只修改部分字段
            - **如果所有参数都为空或空字符串，将返回“无修改内容”**
            - 作品标题限制：**最多 16 个中文字符**（48 字节）
            - 图片文件仅支持：**JPG、JPEG、PNG** 格式
            - 图片文件大小限制：**最大 32MB**
            - **原创与转载链接约束**：
              * 原创作品（isOriginal=true）：不能填写 outUrl，系统会自动清空原有的转载链接
              * 转载作品（isOriginal=false）：必须提供有效的 outUrl
            - 采用动态更新机制，只提供要修改的字段即可
            - 上传新图片后，旧图片会被自动删除（重命名为 .del）
            - 修改成功后，update_time 和 update_user 会自动更新
            - isOriginal 参数使用下拉框选择，避免输入错误

            ## 使用示例：
            ```
            # 示例1：只修改标题（不上传图片）
            POST /api/work/update
            Content-Type: multipart/form-data
            Authorization: Bearer <token>
            
            workId: 1
            workTitle: 春日樱花
            
            # 示例2：上传新图片并修改标题
            POST /api/work/update
            Content-Type: multipart/form-data
            Authorization: Bearer <token>
            
            workId: 1
            file: [binary image data]
            workTitle: 新标题
            
            # 示例3：将转载作品改为原创（自动清空 outUrl）
            POST /api/work/update
            Content-Type: multipart/form-data
            Authorization: Bearer <token>
            
            workId: 1
            isOriginal: true
            # outUrl 不填或填空，系统会自动清空原有的转载链接
            
            # 示例4：将原创作品改为转载
            POST /api/work/update
            Content-Type: multipart/form-data
            Authorization: Bearer <token>
            
            workId: 1
            isOriginal: false
            outUrl: https://example.com/original
            
            # 示例5：只修改外部链接（不改原创状态）
            POST /api/work/update
            Content-Type: multipart/form-data
            Authorization: Bearer <token>
            
            workId: 1
            outUrl: https://new-example.com
            ```
            """
    )
    @RequireRole({22, 77})
    @PostMapping("/update")
    public ResponsePojo<Boolean> updateWork(
        @Parameter(description = "HTTP 请求对象，用于从 Header 或 URL 参数中获取 Token", required = true) HttpServletRequest request,
        @Parameter(description = "作品 ID", required = true, example = "1") @RequestParam Integer workId,
        @Parameter(description = "作品标题（可选，最多 16 个中文字符）", required = false, example = "我的新作品") @RequestParam(required = false) String workTitle,
        @Parameter(description = "新的图片文件（可选，仅支持 JPG/JPEG/PNG，最大 32MB）", required = false) @RequestParam(required = false) MultipartFile file,
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
            Boolean result = workService.updateWork(workId, userId, workTitle, file, isOriginal, outUrl);

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
