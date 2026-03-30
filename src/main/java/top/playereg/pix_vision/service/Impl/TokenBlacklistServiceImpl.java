package top.playereg.pix_vision.service.Impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import top.playereg.pix_vision.service.TokenBlacklistService;

import java.util.concurrent.TimeUnit;

/**
 * Token 黑名单服务实现类
 * 
 * @author PlayerEG
 */
@Service
public class TokenBlacklistServiceImpl implements TokenBlacklistService {
    
    private static final Logger log = LoggerFactory.getLogger(TokenBlacklistServiceImpl.class);
    
    private static final String BLACKLIST_PREFIX = "token:blacklist:";
    
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    
    @Override
    public void addToBlacklist(String token, long expireTime) {
        try {
            String key = BLACKLIST_PREFIX + token;
            redisTemplate.opsForValue().set(key, "logout", expireTime, TimeUnit.MILLISECONDS);
            log.info("Token 已加入黑名单，过期时间：{}ms", expireTime);
        } catch (Exception e) {
            log.error("将 Token 加入黑名单失败：{}", e.getMessage());
        }
    }
    
    @Override
    public boolean isInBlacklist(String token) {
        try {
            String key = BLACKLIST_PREFIX + token;
            Boolean hasKey = redisTemplate.hasKey(key);
            return hasKey != null && hasKey;
        } catch (Exception e) {
            log.error("检查 Token 是否在黑名单中失败：{}", e.getMessage());
            return false;
        }
    }
    
    @Override
    public void removeFromBlacklist(String token) {
        try {
            String key = BLACKLIST_PREFIX + token;
            redisTemplate.delete(key);
            log.info("Token 已从黑名单移除");
        } catch (Exception e) {
            log.error("从黑名单移除 Token 失败：{}", e.getMessage());
        }
    }
}
