package top.playereg.pix_vision.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import top.playereg.pix_vision.handler.NotificationWebSocketHandler;
import top.playereg.pix_vision.handler.WebSocketAuthInterceptor;
import top.playereg.pix_vision.util.PixVisionLogger;

/**
 * WebSocket 配置类
 * <p>
 * 配置 WebSocket 端点、认证拦截器和跨域策略。
 * </p>
 *
 * <h3>配置说明</h3>
 * <ul>
 *   <li>WebSocket 端点：/api/ws/notification</li>
 *   <li>认证方式：URL 参数传递 token</li>
 *   <li>跨域策略：允许所有来源</li>
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
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private static final PixVisionLogger log = PixVisionLogger.create(WebSocketConfig.class);

    @Autowired
    private NotificationWebSocketHandler notificationHandler;

    @Autowired
    private WebSocketAuthInterceptor authInterceptor;

    /**
     * 从配置文件读取允许的跨域源地址
     */
    @Value("${cors.allowed-origin}")
    private String[] allowedOrigin;

    /**
     * 注册 WebSocket 处理器
     *
     * @param registry WebSocket 处理器注册表
     */
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        log.info("WebSocket 配置：允许的跨域源地址数量 {}", allowedOrigin.length);
        registry.addHandler(notificationHandler, "/api/ws/notification")
                .addInterceptors(authInterceptor)
                .setAllowedOriginPatterns(allowedOrigin);
    }
}
