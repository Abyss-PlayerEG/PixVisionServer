package top.playereg.pix_vision.service.Impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import top.playereg.pix_vision.config.PythonServerConfig;
import top.playereg.pix_vision.pojo.dto.ContentAuditResult;
import top.playereg.pix_vision.pojo.external.PythonApiResponse;
import top.playereg.pix_vision.service.ContentAuditService;
import top.playereg.pix_vision.util.PixVisionLogger;

import java.util.HashMap;
import java.util.Map;

/**
 * AI 文案审核服务实现类
 * <p>
 * 调用 Python AI 审核 API 对评论文本进行内容安全审核
 * </p>
 *
 * @author PlayerEG
 */
@Service
public class ContentAuditServiceImpl implements ContentAuditService {

    private static final PixVisionLogger log = PixVisionLogger.create(ContentAuditServiceImpl.class);
    /**
     * 审核结果状态常量
     */
    private static final String STATUS_NORMAL = "normal";
    private static final String STATUS_VIOLATION = "violation";
    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private PythonServerConfig pythonServerConfig;

    /**
     * 对评论文本进行 AI 审核
     *
     * @param text 待审核文本
     * @return 审核结果，调用失败时返回 {@code null}
     * @author PlayerEG
     */
    @Override
    public ContentAuditResult auditContent(String text) {
        try {
            // 构建请求 URL
            String url = pythonServerConfig.getContentAuditUrl();

            log.debug("调用 AI 审核接口: {}, 文本长度: {}", url, text.length());

            // 构建请求体
            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("text", text);

            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(requestBody, headers);

            // 发送 POST 请求
            PythonApiResponse<ContentAuditResult> apiResponse = restTemplate.exchange(
                url,
                HttpMethod.POST,
                requestEntity,
                new ParameterizedTypeReference<PythonApiResponse<ContentAuditResult>>() {}
            ).getBody();

            // 检查响应是否成功
            if (apiResponse == null || !apiResponse.isSuccess()) {
                String errorMsg = apiResponse != null ? apiResponse.getMessage() : "null";
                log.warn("AI 审核调用失败: {}, 降级为待审核", errorMsg);
                return null;
            }

            ContentAuditResult result = apiResponse.getData();
            if (result == null) {
                log.warn("AI 审核结果为空, 降级为待审核");
                return null;
            }

            log.info("AI 审核完成 - 状态: {}, 原因: {}, 命中敏感词: {}",
                result.getStatus(), result.getReason(), result.getInsult_words());

            return result;

        } catch (Exception e) {
            log.error("调用 AI 审核 API 异常, 降级为待审核 - 错误: {}", e.getMessage(), e);
            return null;
        }
    }
}
