package top.playereg.pix_vision.service.Impl;

import cn.hutool.core.util.StrUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import top.playereg.pix_vision.service.VerificationCodeServices;

import javax.annotation.Resource;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Service
@SuppressWarnings("all")
public class VerificationCodeServicesImpl implements VerificationCodeServices {
    private static final Logger log = LoggerFactory.getLogger(VerificationCodeServicesImpl.class);
    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 验证码生成
     *
     * @return 验证码
     * @author blue_sky_ks
     */
    public String verificationCode() {
        // 验证码长度
        final int generateVerificationCodeLength = 6;
        // 验证码元数据
        final String[] metaCode = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9",
                "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O",
                "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z"};

        Random random = new Random();
        StringBuilder verificationCode = new StringBuilder();
        while (verificationCode.length()<generateVerificationCodeLength){
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
     * @author blue_sky_ks
     */
    public void setRedisVCode(String email, String vCode )  {
        // String 存储
        String key = StrUtil.format( "userEmailCode:{}", email ); // 用户邮箱
        redisTemplate.opsForValue().set(
                key, // key
                vCode, // value
                5, // 过期时间
                TimeUnit.MINUTES // 时间单位
        );
    }

    /**
     * 删除验证码缓存
     *
     * @param email 邮箱
     * @author PlayerEG
     */
    public void deleteRedisVCode( String email ) {
        String key = StrUtil.format( "userEmailCode:{}", email ); // 用户邮箱
        redisTemplate.delete( key );
    }

    /**
     * 验证码验证
     *
     * @param email 邮箱
     * @param userInputVCode 用户输入的验证码
     * @author PlayerEG
     * @return 验证结果 true:验证成功 false:验证失败
     */
    @Override
    public boolean verificationCodeVerify(String email, String userInputVCode) {
        // 验证状态
        boolean verificationStatus = false;
        String redisVCode = null;
        
        // 获取缓存中的验证码
        try {
            redisVCode = (String) redisTemplate.opsForValue().get(
                    StrUtil.format("userEmailCode:{}", email)
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
        if (redisVCode.equals(userInputVCode)) {
            // 验证成功
            verificationStatus = true;
            // 删除缓存中的验证码
            try {
                redisTemplate.delete(
                        StrUtil.format("userEmailCode:{}", email)
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
}
