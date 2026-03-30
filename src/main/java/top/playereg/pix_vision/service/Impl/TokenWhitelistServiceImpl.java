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
    public void removeAllByUserId(Integer userId) {
        log.warn("当前数据结构不支持按用户 ID 批量移除 Token，该方法不执行任何操作");
        // 注意：由于只存储了 token:whitelist:{token}，无法通过 userId 反查所有 token
        // 如需此功能，需要改为双层结构或使用其他方案（如使用 jti 作为索引）
    }
}
