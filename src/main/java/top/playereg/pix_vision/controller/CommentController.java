package top.playereg.pix_vision.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import top.playereg.pix_vision.pojo.ResponsePojo;
import top.playereg.pix_vision.pojo.commentsPojo.CommentAddResult;
import top.playereg.pix_vision.pojo.commentsPojo.VO.PrimaryComment;
import top.playereg.pix_vision.service.CommentService;
import top.playereg.pix_vision.service.TokenWhitelistService;
import top.playereg.pix_vision.util.Annotation.PublicAccess;
import top.playereg.pix_vision.util.JWTUtils;
import top.playereg.pix_vision.util.PixVisionLogger;

import java.util.List;

/**
 * 评论控制器 - 提供评论相关的接口
 *
 * @author PlayerEG
 */
@RestController
@RequestMapping("/api/comment")
@RequiredArgsConstructor
@Tag(name = "用户接口 - 评论", description = "提供评论新增、查询等评论相关功能")
public class CommentController {

    private static final PixVisionLogger log = PixVisionLogger.create(CommentController.class);

    private final CommentService commentService;
    private final TokenWhitelistService tokenWhitelistService;

    /**
     * 新增评论（需要登录认证）
     *
     * @param request         HTTP 请求对象，用于从 Header 或 URL 参数中获取 Token
     * @param workId          作品 ID（必填）
     * @param parentCommentId 父评论 ID（可选，二级评论时必填）
     * @param commentFloor    评论层级（必填，1 - 作品评论、2 - 二级评论）
     * @param commentText     评论内容（必填，限制长度 125 个汉字）
     * @return 响应数据，表示评论是否新增成功
     * @author PlayerEG
     */
    @Operation(
        summary = "新增评论接口",
        description = """
            # 新增评论（需要登录认证）

            ## 特性
            - Token 认证（支持 Header 和 URL 参数两种方式）
            - 支持一级评论（对作品评论）和二级评论（回复其他评论）
            - 评论内容长度限制（最多 125 个汉字）
            - 严格的参数校验和逻辑验证

            ## 参数说明：
            - Authorization: Header 中的 Token，格式为 `Bearer <token>`，或通过 URL 参数 `?token=<token>` 传递
            - workId: **作品 ID**，Integer 类型，必填
            - parentCommentId: **父评论 ID**，Integer 类型，可选（二级评论时必填）
            - commentFloor: **评论层级**，Integer 类型，必填（1 - 作品评论、2 - 二级评论）
            - commentText: **评论内容**，String 类型，必填，最多 125 个汉字

            ## 返回说明：
            - **审核通过**：返回 **{"data": true}** 和"评论新增成功"提示
            - **AI 审核不通过（违规）**：返回 **{"data": false}** 和"违规内容：{原因}"提示
            - **AI 审核存疑（待审核）**：返回 **{"data": true}** 和"评论成功，等待人工审核"提示
            - **Token 不存在**：返回 **{"data": null}** 和"Token 不存在"提示
            - **Token 已失效**：返回 **{"data": null}** 和"Token 已失效"提示
            - **参数缺失**：返回 **{"data": false}** 和"缺少必要参数"提示
            - **评论层级无效**：返回 **{"data": false}** 和"评论层级无效，必须为 1 或 2"提示
            - **评论内容过长**：返回 **{"data": false}** 和"评论内容不能超过 125 个汉字"提示
            - **二级评论缺少父评论ID**：返回 **{"data": false}** 和"二级评论必须提供父评论ID"提示
            - **一级评论不应有父评论ID**：返回 **{"data": false}** 和"一级评论不应提供父评论ID"提示
            - **父评论不存在**：返回 **{"data": false}** 和"父评论不存在"提示
            - **父评论不属于当前作品**：返回 **{"data": false}** 和"父评论不属于当前作品"提示
            - **新增失败**：返回 **{"data": false}** 和"评论新增失败"提示

            ## 业务逻辑：
            1. 从请求头或 URL 参数中提取 Token（支持 Bearer 前缀）
            2. 验证 Token 是否在白名单中
            3. 从 Token 中解析用户 ID
            4. 校验所有必要参数（workId, commentFloor, commentText）
            5. 验证评论层级（必须为 1 或 2）
            6. 验证评论内容长度（不超过 125 个汉字）
            7. 如果评论层级为 2，验证 parentCommentId 不为空
            8. 如果评论层级为 1，验证 parentCommentId 为空
            9. 如果是二级评论，验证父评论是否存在且属于同一作品
            10. 调用 AI 审核服务对评论内容进行安全审核
            11. 根据审核结果设置审核状态（通过-10/待审核-20/违规-30）
            12. 创建评论对象并插入数据库
            13. 根据审核状态返回差异化的响应消息

            ## 注意事项：
            - **需要携带有效的 Token 才能新增评论**
            - Token 必须在白名单中（未过期、未登出）
            - 所有用户角色（11, 22, 55, 66, 77）都可以发表评论
            - 评论内容限制：**最多 125 个汉字**
            - 评论层级只能为 1（对作品评论）或 2（回复其他评论）
            - 二级评论必须提供父评论 ID
            - 一级评论不应提供父评论 ID
            - 父评论必须存在且属于同一作品
            - 评论会自动关联到当前登录用户
            - 评论新增后会自动调用 AI 审核服务进行内容安全审核
            - AI 审核不通过（违规）时直接拦截，data 返回 false
            - AI 审核存疑时标记为待审核，评论需要人工审核后才会公开显示
            - AI 审核服务不可用时自动降级为待审核状态
            """
    )
    @PostMapping("/add")
    public ResponsePojo<Boolean> addComment(
        @Parameter(description = "HTTP 请求对象，用于从 Header 或 URL 参数中获取 Token", required = true) HttpServletRequest request,
        @Parameter(description = "作品 ID", required = true, example = "1") @RequestParam Integer workId,
        @Parameter(description = "父评论 ID（二级评论时必填）", required = false, example = "5") @RequestParam(required = false) Integer parentCommentId,
        @Schema(description = "评论层级（1 - 作品评论、2 - 二级评论）", allowableValues = {"1", "2"}, example = "1") @RequestParam Integer commentFloor,
        @Parameter(description = "评论内容（最多 125 个汉字）", required = true, example = "这是一条测试评论") @RequestParam String commentText
    ) {
        log.debug("新增评论 - 作品 ID: {}, 评论层级: {}", workId, commentFloor);

        // 提取 Token
        String token = JWTUtils.extractTokenWithLog(request, "新增评论接口");

        if (token == null || token.isEmpty()) {
            log.error("新增评论失败 - Token 不存在");
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
        log.info("开始新增评论，用户 ID: {}, 用户名: {}, 作品 ID: {}", userId, username, workId);

        // 参数校验
        if (workId == null || commentFloor == null || commentText == null) {
            log.warn("新增评论失败 - 缺少必要参数，用户 ID: {}", userId);
            return ResponsePojo.error(false, "缺少必要参数");
        }

        // 调用服务层新增评论
        CommentAddResult result = commentService.addComment(userId, workId, parentCommentId, commentFloor, commentText);

        if (result.getSuccess() == null || !result.getSuccess()) {
            log.warn("评论新增失败，用户 ID: {}, 用户名: {}, 作品 ID: {}", userId, username, workId);
            return ResponsePojo.error(false, "评论新增失败");
        }

        Integer approvalStatus = result.getApprovalStatus();
        String auditReason = result.getAuditReason();

        // 根据审核状态返回差异化响应
        if (approvalStatus != null && approvalStatus == 30) {
            // 违规内容
            String reason = auditReason != null ? auditReason : "未知原因";
            log.warn("评论审核不通过（违规），用户 ID: {}, 原因: {}", userId, reason);
            return ResponsePojo.error(false, "违规内容：" + reason);
        }

        if (approvalStatus != null && approvalStatus == 20) {
            // 待审核
            log.info("评论新增成功，待人工审核，用户 ID: {}, 作品 ID: {}", userId, workId);
            return ResponsePojo.success(true, "评论成功，等待人工审核");
        }

        // 审核通过（10）或其他
        log.info("评论新增成功，用户 ID: {}, 用户名: {}, 作品 ID: {}", userId, username, workId);
        return ResponsePojo.success(true, "发布成功");
    }

    /**
     * 根据作品 ID 查询评论列表（公开接口）
     *
     * @param workId  作品 ID
     * @param orderBy 排序方式：'oldest' - 按最早发布，其他值或 null - 按最新发布（默认）
     * @return 响应数据，包含一级评论列表（每个一级评论包含二级评论列表）
     * @author PlayerEG
     */
    @Operation(
        summary = "查询作品评论列表",
        description = """
            # 查询作品评论列表（无需登录认证）

            ## 特性
            - 公开接口（无需认证）
            - 根据作品 ID 查询所有评论
            - 包含用户昵称和头像信息
            - 自动构建嵌套回复结构（一级评论包含子评论列表）
            - 支持一级评论按时间排序（最新/最早）
            - 二级评论始终按最早发布排列

            ## 参数说明：
            - workId: **作品 ID**，Integer 类型，必填
            - orderBy: **排序方式**，String 类型，可选
              - 'oldest': 一级评论按最早发布排列
              - 其他值或 null: 一级评论按最新发布排列（默认）

            ## 返回说明：
            - **查询成功**：返回 **{"data": [PrimaryComment列表]}** 和“查询成功”提示
            - **作品 ID 无效**：返回 **{"data": null}** 和“作品 ID 无效”提示
            - **无评论**：返回 **{"data": []}** 和“该作品暂无评论”提示
            - **查询失败**：返回 **{"data": null}** 和“查询失败”提示

            ## 业务逻辑：
            1. 校验作品 ID 参数有效性
            2. 查询该作品的所有评论（排除已删除的评论）
            3. 批量查询所有评论用户的昵称和头像信息
            4. 将评论转换为响应对象，包含用户信息
            5. 构建两级结构：一级评论的 children 字段包含其所有二级评论（SecondaryComment）
            6. 一级评论按指定顺序排列，二级评论始终按最早发布排列
            7. 返回评论列表

            ## 注意事项：
            - 这是一个**公开接口**，无需 Token 认证
            - 只返回**未删除**的评论（is_delete=0）
            - 一级评论支持两种排序：最新发布（默认）或最早发布
            - 二级评论始终按最早发布排列（comment_id ASC）
            - 如果作品没有评论，返回空数组
            - 每个评论对象包含用户昵称（nickname）和头像路径（user_avatar）
            - 一级评论的 children 字段包含其所有二级评论（SecondaryComment类型）
            - 二级评论不再有 children 字段，结构清晰扁平化
            """
    )
    @PublicAccess("评论获取接口，无需认证")
    @GetMapping("/list/{workId}")
    public ResponsePojo<List<PrimaryComment>> getCommentsByWorkId(
        @Parameter(description = "作品 ID", required = true, example = "1") @PathVariable Integer workId,
        @Schema(description = "排序方式：'oldest' - 按最早发布，其他值或 null - 按最新发布（默认）", allowableValues = {"newest", "oldest"}, example = "newest") @RequestParam(required = false, defaultValue = "newest") String orderBy
    ) {
        // 参数校验
        if (workId == null || workId <= 0) {
            log.warn("作品 ID 无效: {}", workId);
            return ResponsePojo.error(null, "作品 ID 无效");
        }

        // 调用服务层查询评论列表（包含用户信息和两级结构）
        List<PrimaryComment> comments = commentService.getCommentsWithUserInfoByWorkId(workId, orderBy);

        if (comments == null) {
            log.warn("查询评论失败，作品 ID: {}", workId);
            return ResponsePojo.error(null, "查询失败");
        }

        if (comments.isEmpty()) {
            log.info("该作品暂无评论，作品 ID: {}", workId);
            return ResponsePojo.success(comments, "该作品暂无评论");
        }

        log.info("查询作品评论成功，作品 ID: {}, 一级评论数量: {}", workId, comments.size());
        return ResponsePojo.success(comments, "查询成功");
    }

    /**
     * 删除评论（需要登录认证）
     *
     * @param request   HTTP 请求对象，用于从 Header 或 URL 参数中获取 Token
     * @param commentId 评论 ID（必填）
     * @return 响应数据，表示评论是否删除成功
     * @author PlayerEG
     */
    @Operation(
        summary = "删除评论接口",
        description = """
            # 删除评论（需要登录认证）

            ## 特性
            - Token 认证（支持 Header 和 URL 参数两种方式）
            - 权限控制：用户只能删除自己的评论
            - 级联删除：删除一级评论时，其下属的所有二级评论也会一并删除
            - 逻辑删除：数据库中仅更新 is_delete 字段

            ## 参数说明：
            - Authorization: Header 中的 Token，格式为 `Bearer <token>`，或通过 URL 参数 `?token=<token>` 传递
            - commentId: **评论 ID**，Integer 类型，必填

            ## 返回说明：
            - **删除成功**：返回 **{"data": true}** 和“评论删除成功”提示
            - **Token 不存在**：返回 **{"data": null}** 和“Token 不存在”提示
            - **Token 已失效**：返回 **{"data": null}** 和“Token 已失效”提示
            - **评论不存在**：返回 **{"data": false}** 和“评论不存在或已删除”提示
            - **无权操作**：返回 **{"data": false}** 和“无权删除他人评论”提示
            - **删除失败**：返回 **{"data": false}** 和“评论删除失败”提示

            ## 业务逻辑：
            1. 从请求头或 URL 参数中提取 Token
            2. 验证 Token 是否在白名单中
            3. 从 Token 中解析用户 ID
            4. 校验评论 ID 参数有效性
            5. 查询评论是否存在且未被删除
            6. 验证当前用户是否为评论所有者
            7. 如果是一级评论，查找其下属的所有二级评论 ID
            8. 执行批量逻辑删除（将 is_delete 设为 1）
            9. 返回删除结果

            ## 注意事项：
            - **需要携带有效的 Token 才能删除评论**
            - Token 必须在白名单中（未过期、未登出）
            - 用户**只能删除自己发布的评论**
            - 删除一级评论会**自动级联删除**其下的所有二级评论
            - 删除二级评论只影响当前评论
            - 该操作为逻辑删除，数据仍保留在数据库中
            """
    )
    @PostMapping("/delete")
    public ResponsePojo<Boolean> deleteComment(
        @Parameter(description = "HTTP 请求对象，用于从 Header 或 URL 参数中获取 Token", required = true) HttpServletRequest request,
        @Parameter(description = "评论 ID", required = true, example = "10") @RequestParam Integer commentId
    ) {
        log.debug("删除评论 - 评论 ID: {}", commentId);

        // 提取 Token
        String token = JWTUtils.extractTokenWithLog(request, "删除评论接口");

        if (token == null || token.isEmpty()) {
            log.error("删除评论失败 - Token 不存在");
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
        log.info("开始删除评论，用户 ID: {}, 用户名: {}, 评论 ID: {}", userId, username, commentId);

        // 参数校验
        if (commentId == null) {
            log.warn("删除评论失败 - 缺少评论 ID");
            return ResponsePojo.error(false, "缺少必要参数：评论 ID");
        }

        // 调用服务层删除评论
        Boolean result = commentService.deleteComment(userId, commentId);

        if (result != null && result) {
            log.info("评论删除成功，用户 ID: {}, 用户名: {}, 评论 ID: {}", userId, username, commentId);
            return ResponsePojo.success(true, "评论删除成功");
        } else {
            log.warn("评论删除失败，用户 ID: {}, 用户名: {}, 评论 ID: {}", userId, username, commentId);
            return ResponsePojo.error(false, "评论删除失败");
        }
    }


}
