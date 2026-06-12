package top.playereg.pix_vision.service.Impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import top.playereg.pix_vision.service.VerificationCodeServices;
import top.playereg.pix_vision.util.PixVisionLogger;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@Service
@SuppressWarnings("all")
public class VerificationCodeServicesImpl implements VerificationCodeServices {
    private static final PixVisionLogger log = PixVisionLogger.create(VerificationCodeServicesImpl.class);
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 验证码生成
     *
     * @return 验证码
     * @implNote 生成仅包含数字和大写字母的验证码
     * @author blue_sky_ks
     */
    public String verificationCode() {
        // 验证码长度
        final int generateVerificationCodeLength = 6;
        // 验证码元数据
        final String[] metaCode = {
            "1", "2", "3", "4", "5", "6", "7", "8", "9",
            "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N",
            "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z"
        };

        Random random = new Random();
        StringBuilder verificationCode = new StringBuilder();
        while (verificationCode.length() < generateVerificationCodeLength) {
            int i = random.nextInt(metaCode.length);
            verificationCode.append(metaCode[i]);
        }

        return verificationCode.toString();
    }

    /**
     * 设置验证码缓存
     *
     * @param email 邮箱
     * @param vCode 验证码
     * @implNote 将邮箱和验证码以键值对缓存到redis
     * @author PlayerEG, blue_sky_ks
     */
    public void setRedisVCode(String email, String vCode) {
        String hashVCode = SecureUtil.sha256(vCode);
        String hashEmail = SecureUtil.sha256(email);
        // String 存储
        String key = StrUtil.format("userEmailCode:{}", hashEmail); // 用户邮箱
        redisTemplate.opsForValue().set(
            key, // key
            hashVCode, // value
            5, // 过期时间
            TimeUnit.MINUTES // 时间单位
        );
    }

    /**
     * 删除验证码缓存
     *
     * @param email 邮箱
     * @implNote 删除邮箱对应的验证码缓存
     * @deprecated
     * @author PlayerEG
     */
    @Deprecated
    public void deleteRedisVCode(String email) {
        String key = StrUtil.format("userEmailCode:{}", email); // 用户邮箱
        redisTemplate.delete(key);
    }

    /**
     * 验证码验证
     *
     * @param email          邮箱
     * @param userInputVCode 用户输入的验证码
     * @return 验证结果 true:验证成功 false:验证失败
     * @implNote 验证用户输入的验证码是否与缓存中的验证码一致
     * @author PlayerEG
     */
    @Override
    public boolean verificationCodeVerify(String email, String userInputVCode) {
        // 验证状态
        boolean verificationStatus = false;
        String redisVCode = null;
        String hashUserInputVCode = SecureUtil.sha256(userInputVCode);
        String hashEmail = SecureUtil.sha256(email);

        // 获取缓存中的验证码
        try {
            redisVCode = (String) redisTemplate.opsForValue().get(
                StrUtil.format("userEmailCode:{}", hashEmail)
            );
        } catch (Exception e) {
            // 获取缓存中的验证码失败
            log.error("获取验证码缓存失败: {}", e.getMessage());
            return verificationStatus;
        }

        // 验证码不存在的情况
        if (redisVCode == null) {
            log.error("验证码不存在或已过期，邮箱: {}", email);
            return verificationStatus;
        }

        // 验证逻辑
        if (redisVCode.equals(hashUserInputVCode)) {
            // 验证成功
            verificationStatus = true;
            // 删除缓存中的验证码
            try {
                redisTemplate.delete(
                    StrUtil.format("userEmailCode:{}", hashEmail)
                );
            } catch (Exception e) {
                log.error("删除验证码缓存失败: {}", e.getMessage());
            }
        } else {
            // 验证失败
            log.info("验证码不匹配，邮箱: {}", email);
            verificationStatus = false;
        }

        return verificationStatus;
    }

    /**
     * 检查验证码是否存在
     *
     * @param email 邮箱
     * @return true:存在 false:不存在
     * @implNote 检查邮箱对应的验证码是否已存在于Redis中
     * @author PlayerEG
     */
    @Override
    public boolean hasRedisVCode(String email) {
        String hashEmail = SecureUtil.sha256(email);
        String key = StrUtil.format("userEmailCode:{}", hashEmail);

        try {
            Object vCode = redisTemplate.opsForValue().get(key);
            return vCode != null;
        } catch (Exception e) {
            log.error("检查验证码缓存失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 获取验证码剩余过期时间
     *
     * @param email 邮箱
     * @return 剩余时间（秒），如果验证码不存在则返回 null
     * @implNote 获取邮箱对应验证码的剩余过期时间
     * @author PlayerEG
     */
    @Override
    public Long getRedisVCodeRemainingTime(String email) {
        String hashEmail = SecureUtil.sha256(email);
        String key = StrUtil.format("userEmailCode:{}", hashEmail);

        try {
            Long remainingTime = redisTemplate.getExpire(key, TimeUnit.SECONDS);
            // Redis 返回 -1 表示永不过期，-2 表示键不存在
            if (remainingTime != null && remainingTime > 0) {
                return remainingTime;
            }
            return null;
        } catch (Exception e) {
            log.error("获取验证码剩余时间失败: {}", e.getMessage());
            return null;
        }
    }
}
