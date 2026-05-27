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
import top.playereg.pix_vision.pojo.ResponsePojo;
import top.playereg.pix_vision.pojo.Works;
import top.playereg.pix_vision.service.StarService;
import top.playereg.pix_vision.service.TokenWhitelistService;
import top.playereg.pix_vision.util.Annotation.PublicAccess;
import top.playereg.pix_vision.util.JWTUtils;
import top.playereg.pix_vision.util.PageUtils;
import top.playereg.pix_vision.util.PixVisionLogger;

/**
 * 收藏控制器 - 提供作品收藏相关的接口
 *
 * @author PlayerEG
 */
@RestController
@RequestMapping("/api/star")
@RequiredArgsConstructor
@Tag(name = "用户接口 - 收藏", description = "提供作品收藏、取消收藏及状态查询功能")
public class StarController {

    private static final PixVisionLogger log = PixVisionLogger.create(StarController.class);

    private final StarService starService;
    private final TokenWhitelistService tokenWhitelistService;

    /**
     * 切换收藏状态（收藏或取消收藏）
     *
     * @param request HTTP 请求对象
     * @param workId  作品 ID
     * @return 响应数据，包含当前是否处于收藏状态
     */
    @Operation(
        summary = "切换收藏状态",
        description = """
            # 切换收藏状态（需要登录认证）

            ## 特性
            - Token 认证（支持 Header 和 URL 参数两种方式）
            - 智能切换：如果未收藏则收藏，如果已收藏则取消
            - 自动同步：实时更新作品的总收藏数
            - 幂等性：重复调用不会产生脏数据

            ## 参数说明：
            - Authorization: Header 中的 Token，格式为 `Bearer <token>`，或通过 URL 参数 `?token=<token>` 传递
            - workId: **作品 ID**，Integer 类型，必填

            ## 返回说明：
            - **操作成功**：返回 **{"data": true/false}**，true 表示当前已收藏，false 表示当前未收藏
            - **Token 不存在或失效**：返回 **{"data": null}** 和错误提示
            - **作品不存在**：返回 **{"data": null}** 和错误提示

            ## 业务逻辑：
            1. 从请求头或 URL 参数中提取 Token 并验证
            2. 从 Token 中解析用户 ID
            3. 检查该用户对该作品的收藏状态
            4. 如果是收藏状态，执行逻辑删除并减少作品收藏数
            5. 如果是未收藏状态，插入或恢复记录并增加作品收藏数
            6. 返回最新的收藏状态

            ## 注意事项：
            - **必须携带有效的 Token**
            - 只有已登录用户才能进行收藏操作
            - 收藏采用逻辑删除方式，保留历史数据
            """
    )
    @PostMapping("/toggle/{workId}")
    public ResponsePojo<Boolean> toggleStar(
        @Parameter(description = "HTTP 请求对象", required = true) HttpServletRequest request,
        @Parameter(description = "作品 ID", required = true, example = "1") @PathVariable Integer workId
    ) {
        // 提取 Token
        String token = JWTUtils.extractTokenWithLog(request, "切换收藏状态接口");
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

        log.info("用户 {} 尝试切换作品 {} 的收藏状态", userId, workId);
        Boolean isStarred = starService.toggleStar(userId, workId);

        if (isStarred != null) {
            String msg = isStarred ? "收藏成功" : "已取消收藏";
            return ResponsePojo.success(isStarred, msg);
        } else {
            return ResponsePojo.error(null, "操作失败");
        }
    }

    /**
     * 查询收藏状态
     *
     * @param request HTTP 请求对象
     * @param workId  作品 ID
     * @return 响应数据，包含是否已收藏
     */
    @Operation(
        summary = "查询收藏状态",
        description = """
            # 查询收藏状态（需要登录认证）

            ## 特性
            - 快速查询当前用户对指定作品的收藏状态
            - 用于前端初始化页面时显示星标状态

            ## 参数说明：
            - Authorization: Header 中的 Token
            - workId: **作品 ID**，Integer 类型，必填

            ## 返回说明：
            - **查询成功**：返回 **{"data": true/false}**
            """
    )
    @GetMapping("/status/{workId}")
    public ResponsePojo<Boolean> getStarStatus(
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

        Boolean isStarred = starService.isStarred(userId, workId);
        return ResponsePojo.success(isStarred, "查询成功");
    }

    /**
     * 查询作品收藏总数
     *
     * @param workId 作品 ID
     * @return 响应数据，包含收藏总数
     */
    @Operation(
        summary = "查询作品收藏总数",
        description = """
            # 查询作品收藏总数（无需登录认证）

            ## 特性
            - 公开接口，查询某作品的总收藏数
            - 数据来源于作品表的冗余字段，性能极高

            ## 参数说明：
            - workId: **作品 ID**，Integer 类型，必填

            ## 返回说明：
            - **查询成功**：返回 **{"data": 收藏数}**
            """
    )
    @PublicAccess("查询作品收藏总数，无需认证")
    @GetMapping("/count/{workId}")
    public ResponsePojo<Integer> getStarCount(
        @Parameter(description = "作品 ID", required = true, example = "1") @PathVariable Integer workId
    ) {
        Integer count = starService.getStarCount(workId);
        return ResponsePojo.success(count, "查询成功");
    }

    /**
     * 分页查询用户收藏过的作品列表
     *
     * @param userId  用户 ID
     * @param current 当前页码（从 1 开始）
     * @param size    每页大小（范围 1-500）
     * @param orderBy 排序方式，默认 "newest"（最新收藏优先），传 "oldest" 按最早收藏优先
     * @return 响应数据，包含分页的作品列表及封面缩略图
     * @author PlayerEG
     */
    @Operation(
        summary = "分页查询用户收藏作品",
        description = """
            # 分页查询用户收藏作品（无需登录认证）
    
            ## 特性
            - 公开接口（无需 Token 认证）
            - MyBatis-Plus 分页支持
            - 只返回审核通过的作品（approval_status = 10）
            - 支持按收藏时间正序或倒序排列
            - 返回作品封面缩略图（thumb_url）
    
            ## 参数说明：
            - userId: **用户 ID**，Integer 类型，路径变量，必填
            - current: **当前页码**，Long 类型，路径变量，必填，从 1 开始
            - size: **每页大小**，Long 类型，路径变量，必填，范围 1-500
            - orderBy: **排序方式**，String 类型，查询参数，可选，默认 "newest"（最新收藏优先），传 "oldest" 按最早收藏优先
    
            ## 返回说明：
            - **查询成功**：返回 `{"data": {IPage<Works>对象}}`，包含分页信息和作品列表，每个作品含封面缩略图（thumb_url）
            - **用户 ID 无效**：返回 `{"data": null}` 和 "用户 ID 无效" 提示
            - **分页参数错误**：返回 `{"data": null}` 和 "页码或每页大小错误" 提示
    
            ## 业务逻辑：
            1. 校验用户 ID 有效性（非空且大于 0）
            2. 校验页码和每页大小参数（current >= 1，1 <= size <= 500）
            3. 构建 MyBatis-Plus 分页对象
            4. 关联查询 tb_star 和 tb_works 表
            5. 过滤条件：用户收藏且未删除（s.is_delete = 0）、作品未删除（w.is_delete = 0）、审核通过（w.approval_status = 10）
            6. 根据 orderBy 参数排序：默认按收藏时间倒序（s.time DESC），传 "oldest" 按收藏时间正序（s.time ASC）
            7. 返回分页结果集，每个作品包含原图（img_url）和封面缩略图（thumb_url）
    
            ## 注意事项：
            - **此接口为公开接口，无需登录即可访问**
            - 如果用户没有收藏过任何作品，返回空的 IPage 对象（total = 0，records = []）
            - **只返回审核通过的作品**，待审核和未过审的作品不可见
            - 建议合理设置每页数量（size），避免一次性加载过多数据
            - 作品原图访问路径：`/api/image/works?filePath={img_url}`
            - 封面缩略图访问路径：`/api/image/works?filePath={thumb_url}`
            - 使用 **RESTful 风格**路径参数，格式：`/user-starred/{userId}/{current}/{size}`
            """
    )
    @PublicAccess("分页查询用户收藏作品，无需认证")
    @GetMapping("/user-starred/{userId}/{current}/{size}")
    public ResponsePojo<IPage<Works>> getUserStarredWorks(
        @Parameter(description = "用户 ID", required = true, example = "1") @PathVariable Integer userId,
        @Parameter(description = "当前页码，从 1 开始", required = true, example = "1") @PathVariable Long current,
        @Parameter(description = "每页大小，范围 1-500", required = true, example = "10") @PathVariable Long size,
        @Schema(description = "排序方式：'oldest' - 按最早收藏，其他值或 null - 按最新收藏（默认）", allowableValues = {"newest", "oldest"}, example = "newest") @RequestParam(required = false, defaultValue = "newest") String orderBy
    ) {
        log.debug("分页查询用户收藏作品 - 用户 ID: {}, 页码: {}, 每页大小: {}, 排序: {}", userId, current, size, orderBy);

        // 参数校验
        if (userId == null || userId <= 0) {
            log.warn("用户 ID 无效: {}", userId);
            return ResponsePojo.error(null, "用户 ID 无效");
        }

        ResponsePojo<?> error = PageUtils.validatePageParams(current, size);
        if (error != null) {
            return (ResponsePojo<IPage<Works>>) (ResponsePojo<?>) error;
        }

        try {
            // 构建分页对象
            Page<Works> page = new Page<>(current, size);

            // 调用服务层查询
            IPage<Works> result = starService.getUserStarredWorks(page, userId, orderBy);

            if (result != null && result.getRecords() != null) {
                log.info("查询用户收藏作品成功，用户 ID: {}, 总数: {}, 当前页: {}",
                    userId, result.getTotal(), result.getCurrent());
                return ResponsePojo.success(result, "查询成功");
            } else {
                log.warn("查询用户收藏作品返回结果为空，用户 ID: {}", userId);
                return ResponsePojo.error(null, "查询失败，返回结果为空");
            }
        } catch (Exception e) {
            log.error("查询用户收藏作品异常，用户 ID: {}, 错误: {}", userId, e.getMessage(), e);
            return ResponsePojo.error(null, "查询失败：" + e.getMessage());
        }
    }
}
