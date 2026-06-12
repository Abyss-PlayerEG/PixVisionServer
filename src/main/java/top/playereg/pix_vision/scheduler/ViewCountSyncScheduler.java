package top.playereg.pix_vision.scheduler;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import top.playereg.pix_vision.mapper.WorksMapper;
import top.playereg.pix_vision.util.PixVisionLogger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 浏览量数据定时同步调度器
 * <p>
 * 定期将 Redis 中的浏览量数据同步到数据库，确保数据持久化，
 * 避免 Redis 重启或过期导致的数据丢失。
 * </p>
 *
 * @author PlayerEG
 */
@Component
@RequiredArgsConstructor
public class ViewCountSyncScheduler {

    private static final PixVisionLogger log = PixVisionLogger.create(ViewCountSyncScheduler.class);
    private static final String VIEW_COUNT_KEY_PREFIX = "pix:work:view:";

    private final WorksMapper worksMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 每 5 分钟同步一次 Redis 浏览量到数据库
     * <p>
     * 使用 SCAN 命令遍历所有浏览量 key，避免 KEYS 命令阻塞 Redis。
     * 批量更新数据库时采用 CASE WHEN 语句，保证幂等性。
     * </p>
     */
    @Scheduled(fixedRate = 5 * 60 * 1000)
    public void syncViewCountsToDatabase() {
        log.info("开始同步浏览量数据到数据库...");
        long startTime = System.currentTimeMillis();

        try {
            // 1. 使用 SCAN 命令遍历所有浏览量 key（避免阻塞）
            ScanOptions options = ScanOptions.scanOptions()
                .match(VIEW_COUNT_KEY_PREFIX + "*")
                .count(100)
                .build();

            List<Map<String, Object>> updateList = new ArrayList<>();

            try (Cursor<String> cursor = redisTemplate.scan(options)) {
                while (cursor.hasNext()) {
                    String key = cursor.next();
                    // 提取 workId
                    String workIdStr = key.substring(VIEW_COUNT_KEY_PREFIX.length());
                    try {
                        Integer workId = Integer.parseInt(workIdStr);
                        Object countObj = redisTemplate.opsForValue().get(key);

                        if (countObj != null) {
                            // 安全地将 Number 类型转换为 Long
                            Long count = ((Number) countObj).longValue();

                            if (count > 0) {
                                Map<String, Object> record = new HashMap<>();
                                record.put("work_id", workId);
                                record.put("count", count);
                                updateList.add(record);
                            }
                        }
                    } catch (NumberFormatException e) {
                        log.warn("无效的 workId 格式: {}", workIdStr);
                    }
                }
            }

            // 2. 批量更新数据库
            if (!updateList.isEmpty()) {
                worksMapper.batchUpdateViewCounts(updateList);
                long endTime = System.currentTimeMillis();
                log.info("浏览量同步完成 - 作品数: {}, 耗时: {} ms",
                    updateList.size(), (endTime - startTime));
            } else {
                log.info("无浏览量数据需要同步");
            }

        } catch (Exception e) {
            log.error("浏览量同步失败: {}", e.getMessage(), e);
        }
    }
}
