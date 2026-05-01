package top.playereg.pix_vision.util;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * PixVisionLogger 测试类
 *
 * @author PlayerEG
 */
@SpringBootTest
class PixVisionLoggerTest {

    private static final PixVisionLogger log = PixVisionLogger.create(PixVisionLoggerTest.class);

    @Test
    void testDebugLog() {
        log.debug("这是一条 DEBUG 级别日志");
        log.debug("带参数的 DEBUG 日志: {}, {}", "参数1", "参数2");
    }

    @Test
    void testInfoLog() {
        log.info("这是一条 INFO 级别日志");
        log.info("带参数的 INFO 日志: {}", "测试数据");
    }

    @Test
    void testWarnLog() {
        log.warn("这是一条 WARN 级别日志");
        log.warn("带参数的 WARN 日志: {}", "警告信息");
    }

    @Test
    void testErrorLog() {
        log.error("这是一条 ERROR 级别日志");
        log.error("带参数的 ERROR 日志: {}", "错误信息");
        
        // 测试带异常的日志
        try {
            throw new RuntimeException("测试异常");
        } catch (Exception e) {
            log.error("捕获到异常", e);
        }
    }

    @Test
    void testTraceLog() {
        log.trace("这是一条 TRACE 级别日志");
        log.trace("带参数的 TRACE 日志: {}", "跟踪信息");
    }

    @Test
    void testMixedLogs() {
        log.info("=== 开始混合日志测试 ===");
        
        log.debug("调试信息");
        log.info("一般信息");
        log.warn("警告信息");
        log.error("错误信息");
        
        log.info("=== 混合日志测试完成 ===");
    }
}
