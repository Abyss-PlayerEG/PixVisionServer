package top.playereg.pix_vision.manager;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.PingMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import top.playereg.pix_vision.util.PixVisionLogger;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket 会话管理器
 * <p>
 * 管理所有 WebSocket 连接会话，提供消息发送功能。
 * </p>
 *
 * <h3>功能说明</h3>
 * <ul>
 *   <li>管理用户 WebSocket 会话</li>
 *   <li>发送消息给指定用户</li>
 *   <li>广播消息给所有在线用户</li>
 *   <li>获取在线用户数量</li>
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
public class WebSocketSessionManager {

    private static final PixVisionLogger log = PixVisionLogger.create(WebSocketSessionManager.class);

    /**
     * 用户会话映射表：userId -> WebSocketSession
     */
    private final ConcurrentHashMap<Integer, WebSocketSession> sessions = new ConcurrentHashMap<>();

    /**
     * 添加用户会话
     *
     * @param userId  用户ID
     * @param session WebSocket 会话
     */
    public void addSession(Integer userId, WebSocketSession session) {
        sessions.put(userId, session);
        log.debug("添加 WebSocket 会话，用户 ID: {}，当前在线：{}", userId, sessions.size());
    }

    /**
     * 移除用户会话
     *
     * @param userId 用户ID
     */
    public void removeSession(Integer userId) {
        sessions.remove(userId);
        log.debug("移除 WebSocket 会话，用户 ID: {}，当前在线：{}", userId, sessions.size());
    }

    /**
     * 获取用户会话
     *
     * @param userId 用户ID
     * @return WebSocket 会话，如果用户不在线则返回 null
     */
    public WebSocketSession getSession(Integer userId) {
        return sessions.get(userId);
    }

    /**
     * 检查用户是否在线
     *
     * @param userId 用户ID
     * @return true-在线，false-离线
     */
    public boolean isOnline(Integer userId) {
        WebSocketSession session = sessions.get(userId);
        return session != null && session.isOpen();
    }

    /**
     * 发送消息给指定用户
     *
     * @param userId  用户ID
     * @param message 消息内容
     */
    public void sendMessage(Integer userId, String message) {
        WebSocketSession session = sessions.get(userId);
        if (session != null && session.isOpen()) {
            try {
                session.sendMessage(new TextMessage(message));
                log.debug("发送消息给用户 {}，消息长度：{}", userId, message.length());
            } catch (IOException e) {
                log.error("发送消息失败，用户 ID: {}，错误：{}", userId, e.getMessage());
                // 移除无效会话
                removeSession(userId);
            }
        } else {
            log.debug("用户 {} 不在线，消息已持久化，等待用户上线查询", userId);
        }
    }

    /**
     * 广播消息给所有在线用户
     *
     * @param message 消息内容
     */
    public void broadcast(String message) {
        sessions.forEach((userId, session) -> {
            if (session.isOpen()) {
                try {
                    session.sendMessage(new TextMessage(message));
                } catch (IOException e) {
                    log.error("广播消息失败，用户 ID: {}，错误：{}", userId, e.getMessage());
                    removeSession(userId);
                }
            }
        });
    }

    /**
     * 获取在线用户数量
     *
     * @return 在线用户数量
     */
    public int getOnlineCount() {
        return sessions.size();
    }

    /**
     * 心跳检测：定时清理僵尸连接
     * <p>
     * 每 30 秒执行一次，向所有连接发送 PING 消息。
     * 如果发送失败，说明连接已断开，自动移除该会话。
     * </p>
     */
    @Scheduled(fixedRate = 30000)
    public void heartbeatCheck() {
        if (sessions.isEmpty()) {
            return;
        }

        List<Integer> deadSessions = new ArrayList<>();
        PingMessage pingMessage = new PingMessage(ByteBuffer.wrap(new byte[0]));

        sessions.forEach((userId, session) -> {
            try {
                if (session.isOpen()) {
                    session.sendMessage(pingMessage);
                } else {
                    deadSessions.add(userId);
                }
            } catch (IOException e) {
                log.debug("心跳检测发现僵尸连接，用户 ID: {}，错误：{}", userId, e.getMessage());
                deadSessions.add(userId);
            }
        });

        // 批量移除僵尸连接
        if (!deadSessions.isEmpty()) {
            deadSessions.forEach(this::removeSession);
            log.info("心跳检测完成，清理僵尸连接 {} 个，当前在线：{}", deadSessions.size(), sessions.size());
        } else {
            log.debug("心跳检测完成，当前在线：{}", sessions.size());
        }
    }
}
