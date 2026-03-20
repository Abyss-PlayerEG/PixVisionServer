package top.playereg.pix_vision.service;


public interface VerificationCodeServices {
    // 验证码接口
    public String verificationCode();

    // 设置验证码缓存
    public void setRedisVCode( String email, String vCode );

    // 释放验证码缓存
    public void deleteRedisVCode( String email );

    // 验证码验证
    public boolean verificationCodeVerify( String email, String userInputVCode );
}
