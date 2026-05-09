package top.playereg.pix_vision.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import top.playereg.pix_vision.pojo.ResponsePojo;
import top.playereg.pix_vision.pojo.Works;
import top.playereg.pix_vision.service.TokenWhitelistService;
import top.playereg.pix_vision.service.WorkService;
import top.playereg.pix_vision.util.JWTUtils;
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
@Tag(name = "用户访问历史管理接口")
public class HistoryController {
    private static final PixVisionLogger log = PixVisionLogger.create(HistoryController.class);

    private final WorkService workService;
    private final TokenWhitelistService tokenWhitelistService;

    /**
     * 查看个人访问历史记录（分页）
     *
     * @param request HTTP 请求对象，用于从 Header 或 URL 参数中获取 Token
     * @param current 当前页码（从 1 开始）
     * @param size    每页大小
     * @return 个人访问历史记录分页列表
     * @author PlayerEG
     */
    @Operation(
        summary = "查看个人访问历史记录",
        description = """
            # 查看个人访问历史记录（需要登录认证 + 分页）

            ## 特性
            - 需要 Token 认证
            - MyBatis-Plus 分页支持
            - 返回当前用户访问过的作品列表
            - 仅返回未删除的作品

            ## 参数说明：
            - Authorization: Header 中的 Token，格式为 `Bearer <token>`，或通过 URL 参数 `?token=<token>` 传递
            - current: **当前页码**，Long 类型，必填，**从 1 开始**，默认为 1
            - size: **每页大小**，Long 类型，必填，范围 1-100，默认为 10

            ## 返回说明：
            - **查询成功**：返回 **{"data": {IPage<Works>对象}}** ，包含作品列表和分页信息
            - **Token 不存在或失效**：返回 **{"data": null}** 和错误提示
            - **无历史记录**：返回 **{"data": null}** 和空列表

            ## 业务逻辑：
            1. 校验页码和每页大小参数
            2. 从请求头或 URL 参数中提取 Token
            3. 验证 Token 是否在白名单中
            4. 从 Token 中解析用户 ID
            5. 构建分页对象并调用 Service 层查询该用户的访问历史记录
            6. 返回关联的作品详细信息分页列表

            ## 注意事项：
            - **必须携带有效的 Token**
            - 只有已登录用户才能查看自己的历史记录
            - 如果作品已被删除，则不会出现在历史记录列表中
            - 每页大小限制：**1-100**
            """
    )
    @GetMapping("/{current}/{size}")
    public ResponsePojo<IPage<Works>> getUserHistory(
        @Parameter(description = "HTTP 请求对象，用于从 Header 或 URL 参数中获取 Token", required = true) HttpServletRequest request,
        @Parameter(description = "当前页码，从 1 开始", required = true, example = "1") @PathVariable Long current,
        @Parameter(description = "每页大小，范围 1-100", required = true, example = "10") @PathVariable Long size
    ) {
        // 参数校验
        if (current == null || current < 1) {
            return ResponsePojo.error(null, "页码必须大于 0");
        }
        if (size == null || size < 1 || size > 100) {
            return ResponsePojo.error(null, "每页大小必须在 1-100 之间");
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
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<Works> page = new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(current, size);

        // 调用服务层查询历史记录
        IPage<Works> historyPage = workService.getUserHistory(page, userId);

        log.info("查询个人历史记录成功，用户 ID: {}, 记录数: {}", userId, historyPage.getTotal());
        return ResponsePojo.success(historyPage, "查询成功");
    }

    /**
     * 批量删除访问历史记录
     *
     * @param request HTTP 请求对象，用于从 Header 或 URL 参数中获取 Token
     * @param workIds 要删除的作品 ID 列表
     * @return 删除结果
     * @author PlayerEG
     */
    @Operation(
        summary = "批量删除访问历史记录",
        description = """
            # 批量删除访问历史记录（需要登录认证）

            ## 特性
            - 需要 Token 认证
            - 支持单条/批量删除
            - SQL 层面权限验证（只能删除自己的历史记录）
            - 逻辑删除（数据不真正从数据库移除）

            ## 参数说明：
            - Authorization: Header 中的 Token，格式为 `Bearer <token>`，或通过 URL 参数 `?token=<token>` 传递
            - workIds: 要删除的作品 ID 列表，整形数组类型，必填
              * 单条删除：传入 [1]
              * 批量删除：传入 [1, 2, 3]

            ## 返回说明：
            - **删除成功**：返回 **{"data": true}** 和“历史记录删除成功”提示
            - **Token 不存在**：返回 **{"data": null}** 和“Token 不存在”提示
            - **Token 已失效**：返回 **{"data": null}** 和“Token 已失效”提示
            - **作品 ID 列表为空**：返回 **{"data": false}** 和“作品 ID 列表不能为空”提示
            - **删除失败**：返回 **{"data": false}** 和“历史记录删除失败”提示

            ## 业务逻辑：
            1. 从请求头或 URL 参数中提取 Token
            2. 验证 Token 是否在白名单中
            3. 从 Token 中解析用户 ID
            4. 校验作品 ID 列表参数有效性
            5. 执行批量逻辑删除（SQL 层面验证 user_id，确保只能删除自己的历史记录）
            6. 返回删除结果

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
        @Parameter(description = "要删除的作品 ID 列表（支持单条或多条）", required = true, example = "1,2,3") @RequestParam List<Integer> workIds
    ) {
        log.debug("删除历史记录 - 作品 ID: {}", workIds);

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
        int workCount = workIds != null ? workIds.size() : 0;
        log.info("开始删除历史记录，用户 ID: {}, 用户名: {}, 作品 ID 数量: {}", userId, username, workCount);

        // 校验作品 ID 列表参数
        if (workIds == null || workIds.isEmpty()) {
            log.warn("作品 ID 列表为空，用户 ID: {}", userId);
            return ResponsePojo.error(false, "作品 ID 列表不能为空");
        }

        // 调用服务层批量删除历史记录
        Boolean result = workService.batchDeleteHistory(workIds, userId);

        if (result) {
            String successMsg = workCount == 1 ? "历史记录删除成功" : "批量删除历史记录成功";
            log.info("{}，用户 ID: {}, 用户名: {}, 删除数量: {}", successMsg, userId, username, workCount);
            return ResponsePojo.success(true, successMsg);
        } else {
            log.warn("历史记录删除失败，用户 ID: {}, 用户名: {}", userId, username);
            return ResponsePojo.error(false, "历史记录删除失败");
        }
    }
}
