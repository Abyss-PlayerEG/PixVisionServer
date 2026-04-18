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
     * 根据用户 ID 查询用户信息
     * @param userId 用户 ID
     * @return 用户对象
     */
    User selectAllUserById(Integer userId);

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

    /**
     * 用户密码修改（通过邮箱）
     * @param email 用户的邮箱
     * @param oldPassword 用户的旧密码
     * @param newPassword 用户的新密码
     * @return 影响行数
     * */
    Integer changeUserLoginPasswordByEmail(String email, String oldPassword, String newPassword );

    /**
     * 忘记密码 - 重置密码（无需登录）
     * @param usernameOrEmail 用户名或邮箱
     * @param newPassword 新密码
     * @param confirmPassword 确认新密码
     * @param vCode 邮箱验证码
     * @return 是否成功
     */
    Boolean resetPasswordByUsernameOrEmail(String usernameOrEmail, String newPassword, String confirmPassword, String vCode);

    /**
     * 更新用户头像
     * @param userId 用户 ID
     * @param avatarUrl 头像路径
     * @return 是否成功
     */
    Boolean updateUserAvatar(Integer userId, String avatarUrl);

    /**
     * 更新用户昵称
     *
     * @param userId   用户 ID
     * @param nickname 新昵称
     * @return 是否成功
     */
    Boolean updateUserNickname(Integer userId, String nickname);
}
