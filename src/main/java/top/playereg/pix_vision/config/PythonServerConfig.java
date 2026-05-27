package top.playereg.pix_vision.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Python 辅助服务连接配置类
 * <p>
 * 统一管理所有 Python 外部服务的 URL 配置，包括 AI 文案审核服务和 B站账号检测服务。
 * 各服务实现类通过注入此类获取对应的 API 地址，避免在代码中硬编码或重复使用 {@code @Value} 注解。
 * </p>
 *
 * <h3>配置格式（application.yml）</h3>
 * <pre>{@code
 * python-server:
 *   base-url: http://localhost:8000/api/v1
 *   content:
 *     audit-path: /content/audit
 *   bilibili:
 *     account-check-path: /accounts/bilibili
 * }</pre>
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * @Autowired
 * private PythonServerConfig pythonServerConfig;
 *
 * // 获取 AI 审核接口完整 URL
 * String auditUrl = pythonServerConfig.getContentAuditUrl();
 *
 * // 获取 B站账号检测接口完整 URL
 * String checkUrl = pythonServerConfig.getBilibiliAccountCheckUrl("520500365");
 * }</pre>
 *
 * @author PlayerEG
 * @see top.playereg.pix_vision.service.Impl.ContentAuditServiceImpl
 * @see top.playereg.pix_vision.service.Impl.BilibiliApiServiceImpl
 */
@Data
@Component
@ConfigurationProperties(prefix = "python-server")
public class PythonServerConfig {

    /**
     * Python 服务基础 URL，默认 http://localhost:8000/api/v1
     */
    private String baseUrl = "http://localhost:8000/api/v1";

    /**
     * AI 文案审核服务配置
     */
    private Content content = new Content();

    /**
     * B站账号检测服务配置
     */
    private Bilibili bilibili = new Bilibili();

    /**
     * 获取 AI 文案审核接口完整 URL
     *
     * @return 完整的审核接口地址
     */
    public String getContentAuditUrl() {
        return baseUrl + content.getAuditPath();
    }

    /**
     * 获取 B站账号检测接口完整 URL（带用户 ID 路径参数）
     *
     * @param userId B站用户 ID（mid）
     * @return 完整的账号检测接口地址
     */
    public String getBilibiliAccountCheckUrl(String userId) {
        return baseUrl + bilibili.getAccountCheckPath() + "/" + userId;
    }

    /**
     * 获取 B站用户信息接口完整 URL（带用户 ID 路径参数）
     *
     * @param userId B站用户 ID（mid）
     * @return 完整的用户信息接口地址
     */
    public String getBilibiliUserInfoUrl(String userId) {
        return baseUrl + bilibili.getAccountCheckPath() + "/" + userId + "/info";
    }

    /**
     * AI 文案审核服务子配置
     */
    @Data
    public static class Content {
        /**
         * 审核接口路径，默认 /content/audit
         */
        private String auditPath = "/content/audit";
    }

    /**
     * B站账号检测服务子配置
     */
    @Data
    public static class Bilibili {
        /**
         * 账号检测接口路径，默认 /accounts/bilibili
         */
        private String accountCheckPath = "/accounts/bilibili";
    }
}
