package top.playereg.pix_vision.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import top.playereg.pix_vision.pojo.ResponsePojo;
import top.playereg.pix_vision.service.LikeService;
import top.playereg.pix_vision.service.TokenWhitelistService;
import top.playereg.pix_vision.util.Annotation.PublicAccess;
import top.playereg.pix_vision.util.JWTUtils;
import top.playereg.pix_vision.util.PixVisionLogger;

/**
 * 点赞控制器 - 提供作品点赞相关的接口
 *
 * @author PlayerEG
 */
@RestController
@RequestMapping("/api/like")
@RequiredArgsConstructor
@Tag(name = "用户接口 - 点赞", description = "提供作品点赞、取消点赞及状态查询功能")
public class LikeController {

    private static final PixVisionLogger log = PixVisionLogger.create(LikeController.class);

    private final LikeService likeService;
    private final TokenWhitelistService tokenWhitelistService;

    /**
     * 切换点赞状态（点赞或取消点赞）
     *
     * @param request HTTP 请求对象
     * @param workId  作品 ID
     * @return 响应数据，包含当前是否处于点赞状态
     */
    @Operation(
        summary = "切换点赞状态",
        description = """
            # 切换点赞状态（需要登录认证）

            ## 特性
            - Token 认证（支持 Header 和 URL 参数两种方式）
            - 智能切换：如果未点赞则点赞，如果已点赞则取消
            - 自动同步：实时更新作品的总点赞数
            - 幂等性：重复调用不会产生脏数据

            ## 参数说明：
            - Authorization: Header 中的 Token，格式为 `Bearer <token>`，或通过 URL 参数 `?token=<token>` 传递
            - workId: **作品 ID**，Integer 类型，必填

            ## 返回说明：
            - **操作成功**：返回 **{"data": true/false}**，true 表示当前已点赞，false 表示当前未点赞
            - **Token 不存在或失效**：返回 **{"data": null}** 和错误提示
            - **作品不存在**：返回 **{"data": null}** 和错误提示

            ## 业务逻辑：
            1. 从请求头或 URL 参数中提取 Token 并验证
            2. 从 Token 中解析用户 ID
            3. 检查该用户对该作品的点赞状态
            4. 如果是点赞状态，执行逻辑删除并减少作品点赞数
            5. 如果是未点赞状态，插入或恢复记录并增加作品点赞数
            6. 返回最新的点赞状态

            ## 注意事项：
            - **必须携带有效的 Token**
            - 只有已登录用户才能进行点赞操作
            - 点赞采用逻辑删除方式，保留历史数据
            """
    )
    @PostMapping("/toggle/{workId}")
    public ResponsePojo<Boolean> toggleLike(
        @Parameter(description = "HTTP 请求对象", required = true) HttpServletRequest request,
        @Parameter(description = "作品 ID", required = true, example = "1") @PathVariable Integer workId
    ) {
        // 提取 Token
        String token = JWTUtils.extractTokenWithLog(request, "切换点赞状态接口");
        if (token == null || token.isEmpty()) {
            return ResponsePojo.error(null, "Token 不存在");
        }

        if (!tokenWhitelistService.isInWhitelist(token)) {
            return ResponsePojo.error(null, "Token 已失效");
        }

        Integer userId = JWTUtils.getUserIdFromToken(token);
        if (userId == null) {
            return ResponsePojo.error(null, "Token 无效");
        }

        log.info("用户 {} 尝试切换作品 {} 的点赞状态", userId, workId);
        Boolean isLiked = likeService.toggleLike(userId, workId);

        if (isLiked != null) {
            String msg = isLiked ? "点赞成功" : "已取消点赞";
            return ResponsePojo.success(isLiked, msg);
        } else {
            return ResponsePojo.error(null, "操作失败");
        }
    }

    /**
     * 查询点赞状态
     *
     * @param request HTTP 请求对象
     * @param workId  作品 ID
     * @return 响应数据，包含是否已点赞
     */
    @Operation(
        summary = "查询点赞状态",
        description = """
            # 查询点赞状态（需要登录认证）

            ## 特性
            - 快速查询当前用户对指定作品的点赞状态
            - 用于前端初始化页面时显示红心状态

            ## 参数说明：
            - Authorization: Header 中的 Token
            - workId: **作品 ID**，Integer 类型，必填

            ## 返回说明：
            - **查询成功**：返回 **{"data": true/false}**
            """
    )
    @GetMapping("/status/{workId}")
    public ResponsePojo<Boolean> getLikeStatus(
        @Parameter(description = "HTTP 请求对象", required = true) HttpServletRequest request,
        @Parameter(description = "作品 ID", required = true, example = "1") @PathVariable Integer workId
    ) {
        String token = JWTUtils.extractToken(request);
        if (token == null || !tokenWhitelistService.isInWhitelist(token)) {
            return ResponsePojo.error(null, "请先登录");
        }

        Integer userId = JWTUtils.getUserIdFromToken(token);
        if (userId == null) {
            return ResponsePojo.error(null, "Token 无效");
        }

        Boolean isLiked = likeService.isLiked(userId, workId);
        return ResponsePojo.success(isLiked, "查询成功");
    }

    /**
     * 查询作品点赞总数
     *
     * @param workId 作品 ID
     * @return 响应数据，包含点赞总数
     */
    @Operation(
        summary = "查询作品点赞总数",
        description = """
            # 查询作品点赞总数（无需登录认证）

            ## 特性
            - 公开接口，查询某作品的总点赞数
            - 数据来源于作品表的冗余字段，性能极高

            ## 参数说明：
            - workId: **作品 ID**，Integer 类型，必填

            ## 返回说明：
            - **查询成功**：返回 **{"data": 点赞数}**
            """
    )
    @PublicAccess("查询作品点赞总数，无需认证")
    @GetMapping("/count/{workId}")
    public ResponsePojo<Integer> getLikeCount(
        @Parameter(description = "作品 ID", required = true, example = "1") @PathVariable Integer workId
    ) {
        Integer count = likeService.getLikeCount(workId);
        return ResponsePojo.success(count, "查询成功");
    }
}
