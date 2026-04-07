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
     * - token:whitelist:{token} -> {userId}:{username} Token 对应的用户信息
     */
    private static final String TOKEN_WHITELIST_PREFIX = "token:whitelist:";
    
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    
    @Override
    public void addToWhitelist(String token, Integer userId, String username, long expireTime) {
        try {
            // 将 Token 加入白名单，存储用户信息便于日志和审计
            String tokenKey = TOKEN_WHITELIST_PREFIX + token;
            String value = userId + ":" + username;
            redisTemplate.opsForValue().set(tokenKey, value, expireTime, TimeUnit.MILLISECONDS);
            
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
            // 从白名单中删除 Token
            String tokenKey = TOKEN_WHITELIST_PREFIX + token;
            redisTemplate.delete(tokenKey);
            
            log.info("Token 已从白名单移除");
        } catch (Exception e) {
            log.error("从白名单移除 Token 失败：{}", e.getMessage());
        }
    }
    
    @Override
    public int removeAllUserTokens(Integer userId, String username) {
        try {
            // 构建用户标识字符串
            String userIdentifier = userId + ":" + username;
            
            // 获取所有白名单 Token
            java.util.Set<String> keys = redisTemplate.keys(TOKEN_WHITELIST_PREFIX + "*");
            
            if (keys == null || keys.isEmpty()) {
                log.info("未找到任何 Token，用户 ID: {}, 用户名：{}", userId, username);
                return 0;
            }
            
            int removedCount = 0;
            for (String key : keys) {
                String value = redisTemplate.opsForValue().get(key);
                // 如果该 Token 属于当前用户，则删除
                if (userIdentifier.equals(value)) {
                    redisTemplate.delete(key);
                    removedCount++;
                    log.debug("已移除用户 Token: {}", key.replace(TOKEN_WHITELIST_PREFIX, ""));
                }
            }
            
            log.info("已移除用户所有 Token，用户 ID: {}, 用户名：{}, 移除数量：{}", userId, username, removedCount);
            return removedCount;
        } catch (Exception e) {
            log.error("移除用户所有 Token 失败，用户 ID: {}, 用户名：{}, 错误：{}", userId, username, e.getMessage());
            return 0;
        }
    }
}
