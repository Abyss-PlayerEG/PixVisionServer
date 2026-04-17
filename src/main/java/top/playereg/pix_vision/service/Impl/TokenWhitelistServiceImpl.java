package top.playereg.pix_vision.service.Impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import top.playereg.pix_vision.service.TokenWhitelistService;

import java.util.concurrent.TimeUnit;

/**
 * Token 白名单服务实现类
 *
 * @author PlayerEG
 */
@Service
public class TokenWhitelistServiceImpl implements TokenWhitelistService {

    private static final Logger log = LoggerFactory.getLogger(TokenWhitelistServiceImpl.class);

    /**
     * Redis Key 命名规则：
     * - token:whitelist:{token} -> {userId}:{username} Token 白名单（String 类型）
     * - token:index:{userId} -> Set<{token1>, <token2>, ...> Token 索引集合（Set 类型，用于快速批量删除）
     */
    private static final String TOKEN_WHITELIST_PREFIX = "token:whitelist:";
    private static final String TOKEN_INDEX_PREFIX = "token:index:";

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Override
    public void addToWhitelist(String token, Integer userId, String username, long expireTime) {
        try {
            // 1. 将 Token 加入白名单，存储用户信息便于日志和审计
            String tokenKey = TOKEN_WHITELIST_PREFIX + token;
            String value = userId + ":" + username;
            redisTemplate.opsForValue().set(tokenKey, value, expireTime, TimeUnit.MILLISECONDS);

            // 2. 将 Token 添加到用户的 Token 索引集合中（Set 结构）
            String indexKey = TOKEN_INDEX_PREFIX + userId;
            redisTemplate.opsForSet().add(indexKey, token);
            // 设置相同的过期时间，保持数据一致性
            redisTemplate.expire(indexKey, expireTime, TimeUnit.MILLISECONDS);

            log.info("Token 已加入白名单，用户 ID: {}, 用户名：{}, 过期时间：{}ms", userId, username, expireTime);
        } catch (Exception e) {
            log.error("将 Token 加入白名单失败：{}", e.getMessage());
        }
    }

    @Override
    public boolean isInWhitelist(String token) {
        try {
            String key = TOKEN_WHITELIST_PREFIX + token;
            Boolean hasKey = redisTemplate.hasKey(key);
            return hasKey != null && hasKey;
        } catch (Exception e) {
            log.error("检查 Token 是否在白名单中失败：{}", e.getMessage());
            return false;
        }
    }

    @Override
    public void removeFromWhitelist(String token) {
        try {
            // 1. 从白名单中删除 Token
            String tokenKey = TOKEN_WHITELIST_PREFIX + token;

            // 2. 获取 Token 对应的用户信息，以便从索引中移除
            String userInfo = redisTemplate.opsForValue().get(tokenKey);
            if (userInfo != null && userInfo.contains(":")) {
                String userId = userInfo.split(":")[0];

                // 3. 从用户的 Token 索引集合中移除该 Token
                String indexKey = TOKEN_INDEX_PREFIX + userId;
                redisTemplate.opsForSet().remove(indexKey, token);

                log.debug("已从用户 {} 的 Token 索引中移除", userId);
            }

            // 4. 删除 Token 白名单记录
            redisTemplate.delete(tokenKey);

            log.info("Token 已从白名单移除");
        } catch (Exception e) {
            log.error("从白名单移除 Token 失败：{}", e.getMessage());
        }
    }

    @Override
    public int removeAllUserTokens(Integer userId, String username) {
        try {
            // 1. 从用户 Token 索引集合中获取该用户的所有 Token（O(1) 复杂度）
            String indexKey = TOKEN_INDEX_PREFIX + userId;
            java.util.Set<String> tokens = redisTemplate.opsForSet().members(indexKey);

            if (tokens == null || tokens.isEmpty()) {
                log.info("未找到任何 Token，用户 ID: {}, 用户名：{}", userId, username);
                return 0;
            }

            // 2. 批量删除白名单中的 Token
            int removedCount = 0;
            for (String token : tokens) {
                String tokenKey = TOKEN_WHITELIST_PREFIX + token;
                redisTemplate.delete(tokenKey);
                removedCount++;
                log.debug("已移除用户 Token: {}", token);
            }

            // 3. 删除用户 Token 索引集合
            redisTemplate.delete(indexKey);

            log.info("已移除用户所有 Token，用户 ID: {}, 用户名：{}, 移除数量：{}", userId, username, removedCount);
            return removedCount;
        } catch (Exception e) {
            log.error("移除用户所有 Token 失败，用户 ID: {}, 用户名：{}, 错误：{}", userId, username, e.getMessage());
            return 0;
        }
    }
}
