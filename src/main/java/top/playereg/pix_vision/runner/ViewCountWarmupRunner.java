package top.playereg.pix_vision.runner;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import top.playereg.pix_vision.mapper.WorksMapper;
import top.playereg.pix_vision.util.PixVisionLogger;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 浏览量数据预热启动器
 * <p>
 * 在系统启动时，从数据库统计历史浏览记录并加载到 Redis 中，
 * 同时同步更新 works 表中的 view_count 字段，确保缓存与持久层数据一致。
 * </p>
 *
 * @author PlayerEG
 */
@Component
@RequiredArgsConstructor
public class ViewCountWarmupRunner implements ApplicationRunner {

    private static final PixVisionLogger log = PixVisionLogger.create(ViewCountWarmupRunner.class);
    private static final String VIEW_COUNT_KEY_PREFIX = "pix:work:view:";
    private static final long CACHE_TTL_MINUTES = 5;

    private final WorksMapper worksMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("开始执行浏览量数据预热...");
        long startTime = System.currentTimeMillis();

        try {
            // 1. 从数据库统计所有作品的总浏览记录数（登录用户 + 游客）
            List<Map<String, Object>> viewCounts = worksMapper.selectAllWorkTotalViewCounts();
            if (viewCounts == null || viewCounts.isEmpty()) {
                log.info("数据库中暂无历史浏览记录，预热结束。");
                return;
            }

            int count = 0;
            // 用于批量更新数据库的集合
            java.util.List<java.util.Map<String, Object>> updateList = new java.util.ArrayList<>();

            for (Map<String, Object> record : viewCounts) {
                Integer workId = (Integer) record.get("work_id");
                Long viewCount = ((Number) record.get("count")).longValue();

                if (workId != null) {
                    String key = VIEW_COUNT_KEY_PREFIX + workId;
                    // 1. 存入 Redis，设置 5 分钟过期时间
                    redisTemplate.opsForValue().set(key, viewCount, CACHE_TTL_MINUTES, TimeUnit.MINUTES);

                    // 2. 准备数据库更新数据
                    java.util.Map<String, Object> dbRecord = new java.util.HashMap<>();
                    dbRecord.put("work_id", workId);
                    dbRecord.put("count", viewCount);
                    updateList.add(dbRecord);

                    count++;
                }
            }

            // 3. 批量同步更新 tb_works 表的 view_count 字段
            if (!updateList.isEmpty()) {
                worksMapper.batchUpdateViewCounts(updateList);
                log.info("已同步更新 {} 个作品的数据库浏览量字段", updateList.size());
            }

            long endTime = System.currentTimeMillis();
            log.info("浏览量数据预热完成！共处理 {} 个作品，耗时: {} ms", count, (endTime - startTime));
        } catch (Exception e) {
            log.error("浏览量数据预热失败: {}", e.getMessage(), e);
        }
    }
}
