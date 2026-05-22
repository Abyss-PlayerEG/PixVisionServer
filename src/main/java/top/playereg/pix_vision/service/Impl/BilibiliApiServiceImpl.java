package top.playereg.pix_vision.service.Impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import top.playereg.pix_vision.config.PythonServerConfig;
import top.playereg.pix_vision.pojo.BilibiliAccountCheckResult;
import top.playereg.pix_vision.pojo.PythonApiResponse;
import top.playereg.pix_vision.service.BilibiliApiService;
import top.playereg.pix_vision.util.PixVisionLogger;

/**
 * B站 API 服务实现类
 * <p>
 * 调用像素视觉 Python 辅助服务检测 B站账号是否存在
 * </p>
 *
 * @author PlayerEG
 */
@Service
@Slf4j
public class BilibiliApiServiceImpl implements BilibiliApiService {
    private static final PixVisionLogger logger = PixVisionLogger.create(BilibiliApiServiceImpl.class);

    private final RestTemplate restTemplate;

    private final PythonServerConfig pythonServerConfig;

    public BilibiliApiServiceImpl(RestTemplate restTemplate, PythonServerConfig pythonServerConfig) {
        this.restTemplate = restTemplate;
        this.pythonServerConfig = pythonServerConfig;
    }

    /**
     * 检测 B站账号是否存在
     *
     * @param userId B站用户 ID（mid）
     * @return true-账号存在，false-账号不存在
     * @throws RuntimeException 当 API 调用失败时抛出异常
     */
    @Override
    public Boolean checkAccountExists(String userId) {
        try {
            // 构建请求 URL
            String url = pythonServerConfig.getBilibiliAccountCheckUrl(userId);

            logger.info("调用 B站账号检测接口: {}", url);

            // 发送 GET 请求（使用 exchange 方法支持泛型）
            PythonApiResponse<BilibiliAccountCheckResult> apiResponse = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<PythonApiResponse<BilibiliAccountCheckResult>>() {}
            ).getBody();

            // 检查响应是否成功
            if (apiResponse == null || !apiResponse.isSuccess()) {
                String errorMsg = apiResponse != null ? apiResponse.getMessage() : "null";
                logger.error("B站账号检测失败: {}", errorMsg);
                throw new RuntimeException("B站账号检测失败: " + errorMsg);
            }

            // 提取检测结果
            BilibiliAccountCheckResult result = apiResponse.getData();
            if (result == null) {
                logger.error("B站账号检测结果为空");
                throw new RuntimeException("B站账号检测结果为空");
            }

            logger.info("B站账号检测结果: userId={}, exists={}", result.getUserId(), result.getExists());

            return result.getExists();

        } catch (RuntimeException e) {
            // 重新抛出业务异常
            throw e;
        } catch (Exception e) {
            logger.error("调用 B站 API 异常: userId={}, error={}", userId, e.getMessage(), e);
            throw new RuntimeException("调用 B站 API 失败: " + e.getMessage(), e);
        }
    }
}
