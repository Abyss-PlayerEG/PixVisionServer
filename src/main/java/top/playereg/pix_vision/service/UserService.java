package top.playereg.pix_vision.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import top.playereg.pix_vision.pojo.userPojo.User;
import top.playereg.pix_vision.pojo.userPojo.UserData;

import java.util.List;

public interface UserService {
    /**
     * 用户注册
     *
     * @param username 用户名
     * @param password 密码
     * @param nickname 昵称
     * @param email    邮箱
     * @return 用户对象
     */
    User registerUser(
        String username,
        String password,
        String nickname,
        String email
    );

    /**
     * 根据用户名查询用户信息
     *
     * @param username 用户名
     * @return 用户对象
     */
    User selectAllUserByUsername(String username);

    /**
     * 根据邮箱查询用户信息
     *
     * @param email 邮箱
     * @return 用户对象
     */
    User selectAllUserByEmail(String email);

    /**
     * 根据用户 ID 查询用户信息
     *
     * @param userId 用户 ID
     * @return 用户对象
     */
    User selectAllUserById(Integer userId);

    /**
     * 根据用户 ID 查询用户角色和用户名（用于权限验证）
     * <p>
     * 仅查询 user_id, username, user_role 三个字段，提升性能
     * </p>
     *
     * @param userId 用户 ID
     * @return 用户对象（仅包含 id, username, user_role）
     */
    User selectUserRoleById(Integer userId);

    /**
     * 分页查询用户信息（支持关键词统一查询）
     *
     * @param page         分页对象
     * @param keyword      关键词（可选，模糊查询用户名/邮箱/昵称）
     * @param uuidBytes    UUID 字节数组（可选，精确查询）
     * @return 分页用户列表
     */
    IPage<User> selectPageUserInfo(IPage<User> page, String keyword, byte[] uuidBytes);

    /**
     * 用户密码修改（通过邮箱）
     *
     * @param email       用户的邮箱
     * @param oldPassword 用户的旧密码
     * @param newPassword 用户的新密码
     * @return 影响行数
     */
    Integer changeUserLoginPasswordByEmail(String email, String oldPassword, String newPassword);

    /**
     * 忘记密码 - 重置密码（无需登录）
     *
     * @param usernameOrEmail 用户名或邮箱
     * @param newPassword     新密码
     * @param confirmPassword 确认新密码
     * @param vCode           邮箱验证码
     * @return 是否成功
     */
    Boolean resetPasswordByUsernameOrEmail(String usernameOrEmail, String newPassword, String confirmPassword, String vCode);

    /**
     * 更新用户头像
     *
     * @param userId    用户 ID
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
    java.util.List<UserData> getUserDataList(Integer userId);

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

    /**
     * 更新用户绑定邮箱（需要验证码验证）
     *
     * @param userId   用户 ID
     * @param newEmail 新邮箱
     * @param vCode    邮箱验证码
     * @return 是否成功
     */
    Boolean updateUserEmail(Integer userId, String newEmail, String vCode);

    /**
     * 清除用户角色缓存
     * <p>
     * 当用户角色发生变更时调用此方法，确保权限验证获取最新的角色信息
     * </p>
     *
     * @param userId 用户 ID
     */
    void clearUserRoleCache(Integer userId);

    /**
     * 清除所有用户角色缓存
     * <p>
     * 批量删除 Redis 中所有 role: 前缀的用户角色缓存
     * 适用于批量修改用户角色后的缓存清理
     * </p>
     *
     * @return 清除的缓存数量
     */
    int clearAllUserRoleCache();

    /**
     * 更新用户角色（仅系统管理员可调用）
     * <p>
     * 修改指定用户的角色，并清除该用户的角色缓存以确保权限验证获取最新信息
     * </p>
     *
     * @param targetUserId 目标用户 ID
     * @param newRole      新角色代码（11-普通用户, 22-创作者, 55-审核员, 66-工单管理员, 77-系统管理员）
     * @param adminId      执行操作的管理员 ID
     * @return 是否成功
     */
    Boolean updateUserRole(Integer targetUserId, Integer newRole, Integer adminId);

    /**
     * 更新用户状态（仅系统管理员可调用）
     * <p>
     * 修改指定用户的账户状态，支持的状态代码：
     * 10-正常, 20-冻结, 30-封禁
     * </p>
     *
     * @param targetUserId 目标用户 ID
     * @param newStatus    新状态代码（10-正常, 20-冻结, 30-封禁）
     * @param adminId      执行操作的管理员 ID
     * @return 是否成功
     */
    Boolean updateUserStatus(Integer targetUserId, Integer newStatus, Integer adminId);

    /**
     * 删除用户账户（仅系统管理员可调用）
     * <p>
     * 管理员可以删除指定用户的账户（逻辑删除），并清除该用户的所有 Token
     * </p>
     *
     * @param targetUserId 目标用户 ID
     * @param adminId      执行操作的管理员 ID
     * @return 是否成功
     */
    Boolean deleteUserAccountByAdmin(Integer targetUserId, Integer adminId);

    /**
     * 管理员创建新用户（仅系统管理员可调用）
     * <p>
     * 管理员可以直接创建新用户，无需验证码验证
     * </p>
     *
     * @param username 用户名
     * @param password 密码（明文，将在 Service 层加密）
     * @param nickname 昵称
     * @param email    邮箱
     * @param role     角色代码（可选，默认为 11-普通用户）
     * @param status   状态代码（可选，默认为 10-正常）
     * @param adminId  执行操作的管理员 ID
     * @return 创建的用户对象，失败返回 null
     */
    User createUserByAdmin(String username, String password, String nickname, String email,
                          Integer role, Integer status, Integer adminId);
}
