package top.playereg.pix_vision.handler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import top.playereg.pix_vision.manager.WebSocketSessionManager;
import top.playereg.pix_vision.util.PixVisionLogger;

/**
 * WebSocket 消息处理器
 * <p>
 * 处理 WebSocket 连接的建立、关闭和消息收发。
 * </p>
 *
 * <h3>功能说明</h3>
 * <ul>
 *   <li>连接建立时注册会话到 SessionManager</li>
 *   <li>连接关闭时从 SessionManager 移除会话</li>
 *   <li>处理心跳消息（PING/PONG）</li>
 *   <li>处理传输错误</li>
 * </ul>
 *
 * <h3>使用场景</h3>
 * <ol>
 *   <li>站内消息实时推送</li>
 *   <li>私信通知</li>
 *   <li>系统公告推送</li>
 * </ol>
 *
 * @author PlayerEG
 * @since V4.0
 */
@Component
public class NotificationWebSocketHandler extends TextWebSocketHandler {

    private static final PixVisionLogger log = PixVisionLogger.create(NotificationWebSocketHandler.class);

    @Autowired
    private WebSocketSessionManager sessionManager;

    /**
     * WebSocket 连接建立成功
     *
     * @param session WebSocket 会话
     * @throws Exception 可能的异常
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Integer userId = (Integer) session.getAttributes().get("userId");
        String username = (String) session.getAttributes().get("username");

        if (userId != null) {
            sessionManager.addSession(userId, session);
            log.info("WebSocket 连接建立，用户：{} (ID: {})", username, userId);
        } else {
            log.warn("WebSocket 连接建立，但未获取到用户信息");
        }
    }

    /**
     * WebSocket 连接关闭
     *
     * @param session WebSocket 会话
     * @param status  关闭状态
     * @throws Exception 可能的异常
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        Integer userId = (Integer) session.getAttributes().get("userId");
        String username = (String) session.getAttributes().get("username");

        if (userId != null) {
            sessionManager.removeSession(userId);
            log.info("WebSocket 连接关闭，用户：{} (ID: {})，状态：{}", username, userId, status);
        }
    }

    /**
     * 处理接收到的文本消息
     *
     * @param session WebSocket 会话
     * @param message 文本消息
     * @throws Exception 可能的异常
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();

        // 处理心跳消息
        if ("PING".equals(payload)) {
            session.sendMessage(new TextMessage("PONG"));
            return;
        }

        // 其他消息暂时忽略
        log.debug("收到 WebSocket 消息：{}", payload);
    }

    /**
     * 处理传输错误
     *
     * @param session   WebSocket 会话
     * @param exception 异常
     * @throws Exception 可能的异常
     */
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        Integer userId = (Integer) session.getAttributes().get("userId");
        String username = (String) session.getAttributes().get("username");

        log.error("WebSocket 传输错误，用户：{} (ID: {})，错误：{}", username, userId, exception.getMessage());

        if (userId != null) {
            sessionManager.removeSession(userId);
        }
    }
}
