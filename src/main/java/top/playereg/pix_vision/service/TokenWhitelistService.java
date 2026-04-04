package top.playereg.pix_vision.service;

/**
 * Token 白名单服务
 * 
 * @author PlayerEG
 */
public interface TokenWhitelistService {
    
    /**
     * 将 Token 加入白名单（允许访问）
     * 
     * @param token JWT Token
     * @param userId 用户 ID
     * @param username 用户名
     * @param expireTime 过期时间（毫秒）
     */
    void addToWhitelist(String token, Integer userId, String username, long expireTime);
    
    /**
     * 检查 Token 是否在白名单中
     * 
     * @param token JWT Token
     * @return true-在白名单中（允许访问），false-不在白名单中（拒绝访问）
     */
    boolean isInWhitelist(String token);
    
    /**
     * 从白名单中移除 Token（禁止访问）
     * 
     * @param token JWT Token
     */
    void removeFromWhitelist(String token);
}
