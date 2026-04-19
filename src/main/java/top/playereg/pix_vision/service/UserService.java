package top.playereg.pix_vision.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import top.playereg.pix_vision.pojo.userPojo.User;

import java.util.List;

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

    /**
     * 新增用户拓展数据
     *
     * @param userId      用户 ID
     * @param dataName    数据名称（电话、邮箱、网站、微信等）
     * @param dataContent 数据内容（具体的电话号码、邮箱地址、网站 url 等）
     * @return 是否成功
     */
    Boolean addUserData(Integer userId, String dataName, String dataContent);

    /**
     * 查询用户所有拓展数据
     *
     * @param userId 用户 ID
     * @return 用户拓展数据列表，如果用户不存在则返回 null
     */
    java.util.List<top.playereg.pix_vision.pojo.userPojo.UserData> getUserDataList(Integer userId);

    /**
     * 批量删除用户拓展数据（支持单条和批量，只能删除自己的数据）
     *
     * @param dataIds 数据 ID 列表（单条删除时传入单个元素的列表）
     * @param userId  用户 ID（从 Token 中获取，用于权限验证）
     * @return 是否成功
     */
    Boolean batchDeleteUserData(List<Integer> dataIds, Integer userId);

    /**
     * 注销用户账户（逻辑删除）
     *
     * @param userId 用户 ID（从 Token 中获取）
     * @return 是否成功
     */
    Boolean deleteUserAccount(Integer userId);

    /**
     * 根据用户名或邮箱查询用户信息（智能识别）
     *
     * @param usernameOrEmail 用户名或邮箱地址
     * @return 用户对象，如果不存在返回 null
     */
    User selectUserByUsernameOrEmail(String usernameOrEmail);
}
