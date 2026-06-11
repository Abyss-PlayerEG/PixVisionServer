package top.playereg.pix_vision.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import top.playereg.pix_vision.pojo.ResponsePojo;
import top.playereg.pix_vision.pojo.VO.ConversationVO;
import top.playereg.pix_vision.pojo.VO.MessageVO;
import top.playereg.pix_vision.pojo.entity.Message;
import top.playereg.pix_vision.service.MessageService;
import top.playereg.pix_vision.service.TokenWhitelistService;
import top.playereg.pix_vision.util.JWTUtils;
import top.playereg.pix_vision.util.PageUtils;
import top.playereg.pix_vision.util.PixVisionLogger;

import java.util.List;
import java.util.Map;

/**
 * 消息控制器 - 提供站内消息相关的接口
 *
 * @author PlayerEG
 * @since V4.0
 */
@RestController
@RequestMapping("/api/message")
@RequiredArgsConstructor
@Tag(name = "用户接口 - 消息", description = "提供站内消息、私信、系统通知相关功能")
public class MessageController {

    private static final PixVisionLogger log = PixVisionLogger.create(MessageController.class);

    private final MessageService messageService;
    private final TokenWhitelistService tokenWhitelistService;

    /**
     * 获取未读消息数量
     *
     * @param request HTTP 请求对象
     * @return 未读消息数量统计
     */
    @Operation(
        summary = "获取未读消息数量",
        description = """
            # 获取未读消息数量（需要登录认证）

            ## 特性
            - 返回未读消息的分类统计
            - 包含总数、私信数、系统通知数

            ## 返回说明：
            - **total**: 总未读数
            - **private**: 未读私信数
            - **system**: 未读系统通知数
            """
    )
    @GetMapping("/unread-count")
    public ResponsePojo<Map<String, Integer>> getUnreadCount(
        @Parameter(description = "HTTP 请求对象", required = true) HttpServletRequest request
    ) {
        // 提取 Token
        String token = JWTUtils.extractTokenWithLog(request, "获取未读消息数量接口");
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

        Map<String, Integer> unreadCount = messageService.getUnreadCount(userId);
        return ResponsePojo.success(unreadCount, "查询成功");
    }

    /**
     * 分页查询会话列表
     *
     * @param request HTTP 请求对象
     * @param current 当前页码（从 1 开始）
     * @param size    每页大小（范围 1-500）
     * @return 会话列表
     */
    @Operation(
        summary = "分页查询会话列表",
        description = """
            # 分页查询会话列表（需要登录认证）

            ## 特性
            - 查询用户的私信会话列表
            - 按最后消息时间倒序排列
            - 包含对方用户信息和未读数量

            ## 参数说明：
            - current: 当前页码，从 1 开始
            - size: 每页大小，范围 1-500

            ## 返回说明：
            - **other_user_id**: 对方用户ID
            - **other_username**: 对方用户名
            - **other_nickname**: 对方昵称
            - **other_avatar_url**: 对方头像
            - **last_message**: 最后一条消息内容
            - **last_message_time**: 最后消息时间
            - **unread_count**: 未读消息数量
            """
    )
    @GetMapping("/conversations/{current}/{size}")
    public ResponsePojo<IPage<ConversationVO>> getConversationList(
        @Parameter(description = "HTTP 请求对象", required = true) HttpServletRequest request,
        @Parameter(description = "当前页码（从 1 开始）", required = true, example = "1") @PathVariable Long current,
        @Parameter(description = "每页大小（范围 1-500）", required = true, example = "10") @PathVariable Long size
    ) {
        // 提取 Token
        String token = JWTUtils.extractTokenWithLog(request, "查询会话列表接口");
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

        // 分页参数校验
        ResponsePojo<?> validateResult = PageUtils.validatePageParams(current, size);
        if (validateResult != null) {
            return (ResponsePojo<IPage<ConversationVO>>) (ResponsePojo<?>) validateResult;
        }
        Page<Message> page = new Page<>(PageUtils.getValidCurrent(current), PageUtils.getValidSize(size));
        IPage<ConversationVO> result = messageService.getConversationList(page, userId);
        return ResponsePojo.success(result, "查询成功");
    }

    /**
     * 分页查询聊天记录
     *
     * @param request     HTTP 请求对象
     * @param otherUserId 对方用户ID
     * @param current     当前页码（从 1 开始）
     * @param size        每页大小（范围 1-500）
     * @return 聊天记录列表
     */
    @Operation(
        summary = "分页查询聊天记录",
        description = """
            # 分页查询聊天记录（需要登录认证）

            ## 特性
            - 查询与指定用户的私信聊天记录
            - 按时间倒序排列
            - 包含发送者信息

            ## 参数说明：
            - otherUserId: 对方用户ID
            - current: 当前页码，从 1 开始
            - size: 每页大小，范围 1-500
            """
    )
    @GetMapping("/chat/{otherUserId}/{current}/{size}")
    public ResponsePojo<IPage<MessageVO>> getChatHistory(
        @Parameter(description = "HTTP 请求对象", required = true) HttpServletRequest request,
        @Parameter(description = "对方用户ID", required = true, example = "1001") @PathVariable Integer otherUserId,
        @Parameter(description = "当前页码（从 1 开始）", required = true, example = "1") @PathVariable Long current,
        @Parameter(description = "每页大小（范围 1-500）", required = true, example = "20") @PathVariable Long size
    ) {
        // 提取 Token
        String token = JWTUtils.extractTokenWithLog(request, "查询聊天记录接口");
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

        // 分页参数校验
        ResponsePojo<?> validateResult = PageUtils.validatePageParams(current, size);
        if (validateResult != null) {
            return (ResponsePojo<IPage<MessageVO>>) (ResponsePojo<?>) validateResult;
        }
        Page<Message> page = new Page<>(PageUtils.getValidCurrent(current), PageUtils.getValidSize(size));
        IPage<MessageVO> result = messageService.getChatHistory(page, userId, otherUserId);
        return ResponsePojo.success(result, "查询成功");
    }

    /**
     * 分页查询系统通知
     *
     * @param request HTTP 请求对象
     * @param current 当前页码（从 1 开始）
     * @param size    每页大小（范围 1-500）
     * @param project 消息主题（可选）
     * @param isRead  已读状态（可选）
     * @return 系统通知列表
     */
    @Operation(
        summary = "分页查询系统通知",
        description = """
            # 分页查询系统通知（需要登录认证）

            ## 特性
            - 查询用户的系统通知列表
            - 支持按消息主题筛选
            - 支持按已读状态筛选
            - 按时间倒序排列

            ## 参数说明：
            - current: 当前页码，从 1 开始
            - size: 每页大小，范围 1-500
            - project: 消息主题（可选），如 work_audit、like 等
            - isRead: 已读状态（可选），true-已读、false-未读
            """
    )
    @GetMapping("/system/{current}/{size}")
    public ResponsePojo<IPage<MessageVO>> getSystemMessages(
        @Parameter(description = "HTTP 请求对象", required = true) HttpServletRequest request,
        @Parameter(description = "当前页码（从 1 开始）", required = true, example = "1") @PathVariable Long current,
        @Parameter(description = "每页大小（范围 1-500）", required = true, example = "10") @PathVariable Long size,
        @Parameter(description = "消息主题（可选）") @RequestParam(required = false) String project,
        @Parameter(description = "已读状态（可选）") @RequestParam(required = false) Boolean isRead
    ) {
        // 提取 Token
        String token = JWTUtils.extractTokenWithLog(request, "查询系统通知接口");
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

        // 分页参数校验
        ResponsePojo<?> validateResult = PageUtils.validatePageParams(current, size);
        if (validateResult != null) {
            return (ResponsePojo<IPage<MessageVO>>) (ResponsePojo<?>) validateResult;
        }
        Page<Message> page = new Page<>(PageUtils.getValidCurrent(current), PageUtils.getValidSize(size));
        IPage<MessageVO> result = messageService.getSystemMessages(page, userId, project, isRead);
        return ResponsePojo.success(result, "查询成功");
    }

    /**
     * 发送私信
     *
     * @param request   HTTP 请求对象
     * @param toUserId  接收者用户ID
     * @param content   消息内容
     * @return 是否成功
     */
    @Operation(
        summary = "发送私信",
        description = """
            # 发送私信（需要登录认证）

            ## 特性
            - 发送私信给指定用户
            - 消息会实时推送给在线用户
            - 离线用户上线后可查询

            ## 参数说明：
            - toUserId: 接收者用户ID
            - content: 消息内容
            """
    )
    @PostMapping("/send")
    public ResponsePojo<Boolean> sendPrivateMessage(
        @Parameter(description = "HTTP 请求对象", required = true) HttpServletRequest request,
        @Parameter(description = "接收者用户ID", required = true, example = "1001") @RequestParam Integer toUserId,
        @Parameter(description = "消息内容", required = true) @RequestParam String content
    ) {
        // 提取 Token
        String token = JWTUtils.extractTokenWithLog(request, "发送私信接口");
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

        // 参数校验
        if (toUserId.equals(userId)) {
            return ResponsePojo.error(false, "不能给自己发送私信");
        }

        if (content == null || content.trim().isEmpty()) {
            return ResponsePojo.error(false, "消息内容不能为空");
        }

        log.info("用户 {} 向用户 {} 发送私信", userId, toUserId);
        messageService.sendPrivateMessage(userId, toUserId, content.trim());
        return ResponsePojo.success(true, "发送成功");
    }

    /**
     * 标记会话已读
     *
     * @param request     HTTP 请求对象
     * @param otherUserId 对方用户ID
     * @return 是否成功
     */
    @Operation(
        summary = "标记会话已读",
        description = """
            # 标记会话已读（需要登录认证）

            ## 特性
            - 标记与指定用户的所有未读私信为已读
            - 用于打开会话时自动标记

            ## 参数说明：
            - otherUserId: 对方用户ID
            """
    )
    @PostMapping("/read/conversation/{otherUserId}")
    public ResponsePojo<Boolean> markConversationAsRead(
        @Parameter(description = "HTTP 请求对象", required = true) HttpServletRequest request,
        @Parameter(description = "对方用户ID", required = true, example = "1001") @PathVariable Integer otherUserId
    ) {
        // 提取 Token
        String token = JWTUtils.extractTokenWithLog(request, "标记会话已读接口");
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

        boolean success = messageService.markConversationAsRead(userId, otherUserId);
        return success ? ResponsePojo.success(true, "标记成功") : ResponsePojo.error(false, "标记失败");
    }

    /**
     * 全部标记已读
     *
     * @param request     HTTP 请求对象
     * @param messageType 消息类型（可选，为空则标记所有类型）
     * @return 是否成功
     */
    @Operation(
        summary = "全部标记已读",
        description = """
            # 全部标记已读（需要登录认证）

            ## 特性
            - 标记所有未读消息为已读
            - 支持按消息类型筛选标记

            ## 参数说明：
            - messageType: 消息类型（可选），system-系统通知、private-私信
            """
    )
    @PostMapping("/read-all")
    public ResponsePojo<Boolean> markAllAsRead(
        @Parameter(description = "HTTP 请求对象", required = true) HttpServletRequest request,
        @Parameter(description = "消息类型（可选）") @RequestParam(required = false) String messageType
    ) {
        // 提取 Token
        String token = JWTUtils.extractTokenWithLog(request, "全部标记已读接口");
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

        boolean success = messageService.markAllAsRead(userId, messageType);
        return success ? ResponsePojo.success(true, "标记成功") : ResponsePojo.error(false, "标记失败");
    }

    /**
     * 删除消息
     *
     * @param request   HTTP 请求对象
     * @param messageId 消息ID
     * @return 是否成功
     */
    @Operation(
        summary = "删除消息",
        description = """
            # 删除消息（需要登录认证）

            ## 特性
            - 软删除消息（仅对自己可见）
            - 只能删除自己收到的消息

            ## 参数说明：
            - messageId: 消息ID
            """
    )
    @PostMapping("/delete/{messageId}")
    public ResponsePojo<Boolean> deleteMessage(
        @Parameter(description = "HTTP 请求对象", required = true) HttpServletRequest request,
        @Parameter(description = "消息ID", required = true, example = "1") @PathVariable Integer messageId
    ) {
        // 提取 Token
        String token = JWTUtils.extractTokenWithLog(request, "删除消息接口");
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

        boolean success = messageService.deleteMessage(userId, messageId);
        return success ? ResponsePojo.success(true, "删除成功") : ResponsePojo.error(false, "删除失败");
    }

    /**
     * 批量删除消息
     *
     * @param request    HTTP 请求对象
     * @param messageIds 消息ID列表
     * @return 是否成功
     */
    @Operation(
        summary = "批量删除消息",
        description = """
            # 批量删除消息（需要登录认证）

            ## 特性
            - 批量软删除消息（仅对自己可见）
            - 只能删除自己收到的消息

            ## 参数说明：
            - messageIds: 消息ID列表
            """
    )
    @PostMapping("/batch-delete")
    public ResponsePojo<Boolean> batchDeleteMessages(
        @Parameter(description = "HTTP 请求对象", required = true) HttpServletRequest request,
        @Parameter(description = "消息ID列表", required = true) @RequestBody List<Integer> messageIds
    ) {
        // 提取 Token
        String token = JWTUtils.extractTokenWithLog(request, "批量删除消息接口");
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

        if (messageIds == null || messageIds.isEmpty()) {
            return ResponsePojo.error(false, "消息ID列表不能为空");
        }

        boolean success = messageService.batchDeleteMessages(userId, messageIds);
        return success ? ResponsePojo.success(true, "批量删除成功") : ResponsePojo.error(false, "批量删除失败");
    }
}
