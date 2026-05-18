package top.playereg.pix_vision.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * HTTP 客户端配置类
 * <p>
 * 配置 RestTemplate 用于调用外部 API（如 Python 辅助服务）
 * </p>
 *
 * @author PlayerEG
 */
@Configuration
public class HttpClientConfig {

    /**
     * 创建 RestTemplate Bean
     * <p>
     * 配置合理的超时时间：
     * - 连接超时：5秒
     * - 读取超时：15秒（考虑限流重试可能需要更长时间）
     * </p>
     *
     * @return 配置好的 RestTemplate 实例
     */
    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);  // 连接超时 5 秒
        factory.setReadTimeout(15000);    // 读取超时 15 秒
        return new RestTemplate(factory);
    }
}
