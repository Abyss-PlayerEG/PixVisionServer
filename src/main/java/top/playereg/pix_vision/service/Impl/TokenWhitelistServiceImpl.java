package top.playereg.pix_vision.service.Impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import top.playereg.pix_vision.service.TokenWhitelistService;
import top.playereg.pix_vision.util.JWTUtils;

import java.util.Map;
import java.util.Set;
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
     * - user:tokens:{userId} -> Set<String> 存储用户的所有 Token
     * - token:whitelist:{token} -> {userId}:{username} Token 对应的用户信息
     */
    private static final String USER_TOKENS_PREFIX = "user:tokens:";
    private static final String TOKEN_WHITELIST_PREFIX = "token:whitelist:";
    
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    
    @Override
    public void addToWhitelist(String token, Integer userId, String username, long expireTime) {
        try {
            // 1. 将 Token 存入用户的 Token 集合
            String userTokensKey = USER_TOKENS_PREFIX + userId;
            redisTemplate.opsForSet().add(userTokensKey, token);
            redisTemplate.expire(userTokensKey, expireTime, TimeUnit.MILLISECONDS);
            
            // 2. 将 Token 加入白名单，存储用户信息便于日志和审计
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
            // 1. 从白名单中删除 Token
            String tokenKey = TOKEN_WHITELIST_PREFIX + token;
            redisTemplate.delete(tokenKey);
            
            // 2. 从用户的 Token 集合中移除
            Map<String, Object> payload = JWTUtils.getTokenPayload(token);
            if (payload != null && payload.containsKey("userId")) {
                Object userIdObj = payload.get("userId");
                if (userIdObj instanceof Number) {
                    Integer userId = ((Number) userIdObj).intValue();
                    String userTokensKey = USER_TOKENS_PREFIX + userId;
                    redisTemplate.opsForSet().remove(userTokensKey, token);
                }
            }
            
            log.info("Token 已从白名单移除");
        } catch (Exception e) {
            log.error("从白名单移除 Token 失败：{}", e.getMessage());
        }
    }
    
    @Override
    public void removeAllByUserId(Integer userId) {
        try {
            // 获取用户的所有 Token
            String userTokensKey = USER_TOKENS_PREFIX + userId;
            Set<String> tokens = redisTemplate.opsForSet().members(userTokensKey);
            
            if (tokens != null && !tokens.isEmpty()) {
                // 批量删除所有 Token
                for (String token : tokens) {
                    String tokenKey = TOKEN_WHITELIST_PREFIX + token;
                    redisTemplate.delete(tokenKey);
                }
                
                // 删除用户的 Token 集合
                redisTemplate.delete(userTokensKey);
                
                log.info("用户 ID: {} 的所有 Token 已从白名单移除，共 {} 个", userId, tokens.size());
            }
        } catch (Exception e) {
            log.error("移除用户 ID: {} 的所有 Token 失败：{}", e.getMessage(), userId);
        }
    }
}
