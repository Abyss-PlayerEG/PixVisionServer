package top.playereg.pix_vision.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import top.playereg.pix_vision.pojo.History;
import top.playereg.pix_vision.pojo.ResponsePojo;
import top.playereg.pix_vision.service.TokenWhitelistService;
import top.playereg.pix_vision.service.WorkService;
import top.playereg.pix_vision.util.JWTUtils;
import top.playereg.pix_vision.util.PageUtils;
import top.playereg.pix_vision.util.PixVisionLogger;

import java.util.List;

/**
 * 历史记录控制器 - 提供用户访问历史相关的接口
 *
 * @author PlayerEG
 */
@RestController
@RequestMapping("/api/history")
@RequiredArgsConstructor
@Tag(name = "用户接口 - 历史记录", description = "提供用户访问历史记录的查询和删除功能")
public class HistoryController {
    private static final PixVisionLogger log = PixVisionLogger.create(HistoryController.class);

    private final WorkService workService;
    private final TokenWhitelistService tokenWhitelistService;

    /**
     * 查看个人访问历史记录（分页）
     *
     * @param request HTTP 请求对象，用于从 Header 或 URL 参数中获取 Token
     * @param current 当前页码（从 1 开始）
     * @param size    每页大小（范围 1-500）
     * @return 响应数据，包含个人访问历史记录分页列表
     * @author PlayerEG
     */
    @Operation(
        summary = "查看个人访问历史记录",
        description = """
            # 查看个人访问历史记录（需要登录认证 + 分页）

            ## 特性
            - 需要 Token 认证
            - MyBatis-Plus 分页支持
            - 返回当前用户访问过的作品列表（每个作品只返回一条最新记录）
            - 仅返回未删除的作品
            - 按访问时间倒序排列（最新的在前）

            ## 参数说明：
            - Authorization: Header 中的 Token，格式为 `Bearer <token>`，或通过 URL 参数 `?token=<token>` 传递
            - current: **当前页码**，Long 类型，必填，**从 1 开始**，默认为 1
            - size: **每页大小**，Long 类型，必填，范围 1-500，默认为 10

            ## 返回说明：
            - **查询成功**：返回 **{"data": {IPage<History>对象}}** ，包含作品列表和分页信息
            - **Token 不存在或失效**：返回 **{"data": null}** 和错误提示
            - **无历史记录**：返回 **{"data": null}** 和空列表

            ## 业务逻辑：
            1. 校验页码和每页大小参数
            2. 从请求头或 URL 参数中提取 Token
            3. 验证 Token 是否在白名单中
            4. 从 Token 中解析用户 ID
            5. 构建分页对象并调用 Service 层查询该用户的访问历史记录
            6. 对同一作品的多条记录进行去重，只保留访问时间最新的那条
            7. 按访问时间倒序返回结果
            8. 返回关联的作品详细信息分页列表

            ## 注意事项：
            - **必须携带有效的 Token**
            - 只有已登录用户才能查看自己的历史记录
            - 如果作品已被删除，则不会出现在历史记录列表中
            - **每个作品只返回一条记录**（最近一次访问的记录）
            - 如果多次访问同一作品，只显示最新的那次访问记录
            - 每页大小限制：**1-500**
            """
    )
    @GetMapping("/{current}/{size}")
    public ResponsePojo<IPage<History>> getUserHistory(
        @Parameter(description = "HTTP 请求对象，用于从 Header 或 URL 参数中获取 Token", required = true) HttpServletRequest request,
        @Parameter(description = "当前页码，从 1 开始", required = true, example = "1") @PathVariable Long current,
        @Parameter(description = "每页大小，范围 1-500", required = true, example = "10") @PathVariable Long size
    ) {
        // 参数校验
        ResponsePojo<?> error = PageUtils.validatePageParams(current, size);
        if (error != null) {
            return (ResponsePojo<IPage<History>>) (ResponsePojo<?>) error;
        }

        // 提取 Token
        String token = JWTUtils.extractTokenWithLog(request, "查看个人历史记录接口");

        if (token == null || token.isEmpty()) {
            log.error("查看历史记录失败 - Token 不存在");
            return ResponsePojo.error(null, "Token 不存在，请在 Header 中添加 Authorization: Bearer <token>");
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
        log.info("开始查询个人历史记录，用户 ID: {}, 用户名: {}, 页码: {}, 每页: {}", userId, username, current, size);

        // 构建分页对象
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<top.playereg.pix_vision.pojo.History> page = new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(current, size);

        // 调用服务层查询历史记录
        IPage<History> historyPage = workService.getUserHistory(page, userId);

        log.info("查询个人历史记录成功，用户 ID: {}, 记录数: {}", userId, historyPage.getTotal());
        return ResponsePojo.success(historyPage, "查询成功");
    }

    /**
     * 批量删除访问历史记录
     *
     * @param request HTTP 请求对象，用于从 Header 或 URL 参数中获取 Token
     * @param workIds 要删除的作品 ID 列表
     * @return 响应数据，表示历史记录是否删除成功
     * @author PlayerEG
     */
    @Operation(
        summary = "批量删除访问历史记录",
        description = """
            # 批量删除访问历史记录（需要登录认证）

            ## 特性
            - 需要 Token 认证
            - 支持单条/批量删除指定作品
            - 支持一键清空所有历史记录
            - SQL 层面权限验证（只能删除自己的历史记录）
            - 逻辑删除（数据不真正从数据库移除）

            ## 参数说明：
            - Authorization: Header 中的 Token，格式为 `Bearer <token>`，或通过 URL 参数 `?token=<token>` 传递
            - workIds: 要删除的作品 ID 列表，整形数组类型。当 clearAll 为 false 或 null 时必填
              * 单条删除：传入 [1]
              * 批量删除：传入 [1, 2, 3]
            - clearAll: 是否清空所有历史记录，布尔值，可选。默认为 false
              * true: 清空当前用户的所有历史记录（此时 workIds 可为空）
              * false/null: 执行基于 workIds 的批量删除

            ## 返回说明：
            - **删除成功**：返回 **{"data": true}** 和“历史记录删除成功”提示
            - **清空成功**：返回 **{"data": true}** 和“历史记录已清空”提示
            - **Token 不存在**：返回 **{"data": null}** 和“Token 不存在”提示
            - **Token 已失效**：返回 **{"data": null}** 和“Token 已失效”提示
            - **参数错误**：返回 **{"data": false}** 和相应的错误提示
            - **删除失败**：返回 **{"data": false}** 和“历史记录删除失败”提示

            ## 业务逻辑：
            1. 从请求头或 URL 参数中提取 Token
            2. 验证 Token 是否在白名单中
            3. 从 Token 中解析用户 ID
            4. 判断 clearAll 参数：
               - 若为 true，则清空该用户所有历史记录
               - 若为 false 或 null，则校验 workIds 并执行批量逻辑删除
            5. 返回操作结果

            ## 注意事项：
            - **需要携带有效的 Token 才能删除历史记录**
            - Token 必须在白名单中（未过期、未登出）
            - **用户只能删除自己的历史记录**
            - 采用逻辑删除方式，数据不会真正从数据库中移除
            - 建议单次批量删除不超过 100 个记录
            """
    )
    @PostMapping("/delete")
    public ResponsePojo<Boolean> deleteHistory(
        @Parameter(description = "HTTP 请求对象，用于从 Header 或 URL 参数中获取 Token", required = true) HttpServletRequest request,
        @Parameter(description = "要删除的作品 ID 列表（支持单条或多条）", required = false, example = "1,2,3") @RequestParam(required = false) List<Integer> workIds,
        @Schema(description = "是否清空所有历史记录", allowableValues = {"true", "false"}, defaultValue = "true") @RequestParam(required = false) Boolean clearAll
    ) {
        log.debug("删除历史记录 - 作品 ID: {}, 清空全部: {}", workIds, clearAll);

        // 提取 Token
        String token = JWTUtils.extractTokenWithLog(request, "删除历史记录接口");

        if (token == null || token.isEmpty()) {
            log.error("删除历史记录失败 - Token 不存在");
            return ResponsePojo.error(null, "Token 不存在，请在 Header 中添加 Authorization: Bearer <token>");
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
        log.info("开始删除历史记录，用户 ID: {}, 用户名: {}, 清空全部: {}", userId, username, clearAll);

        Boolean result;
        // 如果 clearAll 为 true，则清空该用户所有历史记录
        if (Boolean.TRUE.equals(clearAll)) {
            result = workService.clearUserHistory(userId);
            if (result) {
                log.info("清空历史记录成功，用户 ID: {}, 用户名: {}", userId, username);
                return ResponsePojo.success(true, "历史记录已清空");
            } else {
                log.warn("清空历史记录失败或无记录可删，用户 ID: {}, 用户名: {}", userId, username);
                return ResponsePojo.error(false, "清空历史记录失败");
            }
        } else {
            // 否则执行批量删除指定作品
            if (workIds == null || workIds.isEmpty()) {
                log.warn("作品 ID 列表为空且未选择清空，用户 ID: {}", userId);
                return ResponsePojo.error(false, "作品 ID 列表不能为空");
            }
            result = workService.batchDeleteHistory(workIds, userId);
            if (result) {
                int workCount = workIds.size();
                String successMsg = workCount == 1 ? "历史记录删除成功" : "批量删除历史记录成功";
                log.info("{}，用户 ID: {}, 用户名: {}, 删除数量: {}", successMsg, userId, username, workCount);
                return ResponsePojo.success(true, successMsg);
            } else {
                log.warn("历史记录删除失败，用户 ID: {}, 用户名: {}", userId, username);
                return ResponsePojo.error(false, "历史记录删除失败");
            }
        }
    }
}
