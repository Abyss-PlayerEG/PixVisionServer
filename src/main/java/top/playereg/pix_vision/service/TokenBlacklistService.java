package top.playereg.pix_vision.service;

/**
 * Token 黑名单服务
 * 
 * @author PlayerEG
 */
public interface TokenBlacklistService {
    
    /**
     * 将 Token 加入黑名单
     * 
     * @param token JWT Token
     * @param expireTime 过期时间（毫秒）
     */
    void addToBlacklist(String token, long expireTime);
    
    /**
     * 检查 Token 是否在黑名单中
     * 
     * @param token JWT Token
     * @return true-在黑名单中，false-不在黑名单中
     */
    boolean isInBlacklist(String token);
    
    /**
     * 从黑名单中移除 Token
     * 
     * @param token JWT Token
     */
    void removeFromBlacklist(String token);
}
