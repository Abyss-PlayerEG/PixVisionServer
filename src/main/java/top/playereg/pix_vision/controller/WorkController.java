package top.playereg.pix_vision.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
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
@Tag(name = "作品接口", description = "提供作品查询、展示等接口")
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
            # 删除作品（需要登录认证）

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
    @DeleteMapping("/delete")
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
}
