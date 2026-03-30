package top.playereg.pix_vision.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import top.playereg.pix_vision.service.TokenWhitelistService;

/**
 * Token 白名单管理工具类
 * 
 * 用于在修改密码、封禁用户、删除用户等场景下清除用户的 Token
 * 
 * @author PlayerEG
 */
@Component
public class TokenWhitelistManager {
    
    private static final Logger log = LoggerFactory.getLogger(TokenWhitelistManager.class);
    
    @Autowired
    private TokenWhitelistService tokenWhitelistService;
    
    /**
     * 清除指定用户的所有 Token（从白名单中移除）
     * 适用于以下场景：
     * - 修改密码
     * - 账户被封禁
     * - 账户被删除
     * - 强制下线
     * 
     * @param userId 用户 ID
     */
    public void clearUserTokens(Integer userId) {
        if (userId == null) {
            log.warn("用户 ID 为空，无法清除 Token");
            return;
        }
        
        log.info("开始清除用户 ID: {} 的所有 Token", userId);
        tokenWhitelistService.removeAllByUserId(userId);
        log.info("用户 ID: {} 的所有 Token 已清除", userId);
    }
}
