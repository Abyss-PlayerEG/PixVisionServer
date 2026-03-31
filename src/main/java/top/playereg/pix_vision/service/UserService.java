package top.playereg.pix_vision.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import top.playereg.pix_vision.pojo.userPojo.User;

public interface UserService {
    User registerUser(
            String username,
            String password,
            String nickname,
            String email
    );
    User selectAllUserByUsername(String username);
    User selectAllUserByEmail(String email);
    
    /**
     * 分页查询用户信息
     * @param page 分页参数
     * @param username 用户名（可选）
     * @param uuid UUID（可选）
     * @param email 邮箱（可选）
     * @return 分页用户列表
     */
    IPage<User> selectPageUserInfo(
            IPage<?> page,
            String username,
            byte[] uuid,
            String email
    );
}
