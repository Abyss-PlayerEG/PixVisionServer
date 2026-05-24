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
     * 根据 UUID 查询用户信息
     *
     * @param uuidBytes UUID 字节数组（16字节）
     * @return 用户对象
     */
    User selectAllUserByUuid(byte[] uuidBytes);

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
     * @param page      分页对象
     * @param keyword   关键词（可选，模糊查询用户名/邮箱/昵称）
     * @param uuidBytes UUID 字节数组（可选，精确查询）
     * @return 分页用户列表
     */
    IPage<User> selectPageUserInfo(IPage<User> page, String keyword, byte[] uuidBytes);

    /**
     * 按角色分页查询用户信息（支持单个或多个角色）
     *
     * @param page      分页对象
     * @param userRoles 用户角色列表（可选，支持多个角色，OR 关系）
     * @return 分页用户列表
     */
    IPage<User> selectPageUserInfoByRole(IPage<User> page, java.util.List<Integer> userRoles);

    /**
     * 管理员分页查询用户列表（支持多条件过滤）
     *
     * @param page     页码（从 1 开始）
     * @param size     每页大小
     * @param userRole 用户角色（可选，11-普通用户, 22-创作者, 55-审核员, 66-工单管理员, 77-系统管理员）
     * @param status   用户状态（可选，10-正常, 20-冻结, 30-封禁）
     * @param isDelete 是否已删除（可选）
     * @param nickname 昵称关键字（可选，模糊查询）
     * @param orderBy  排序方式：'oldest' - 最早注册，其他值或 null - 最新注册（默认）
     * @return 分页用户列表
     */
    IPage<User> getAdminUserPage(Integer page, Integer size, Integer userRole,
                                  Integer status, Boolean isDelete, String nickname, String orderBy);

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
     * @param adminId   执行操作的用户 ID（用户自己更新时传自身 ID，管理员更新时传管理员 ID）
     * @return 是否成功
     */
    Boolean updateUserAvatar(Integer userId, String avatarUrl, Integer adminId);

    /**
     * 更新用户昵称
     *
     * @param userId   用户 ID
     * @param nickname 新昵称
     * @param adminId  执行操作的用户 ID（用户自己更新时传自身 ID，管理员更新时传管理员 ID）
     * @return 是否成功
     */
    Boolean updateUserNickname(Integer userId, String nickname, Integer adminId);

    /**
     * 更新用户昵称（带 AI 审核）
     * <p>
     * 与 {@link #updateUserNickname} 不同，此方法会先调用 AI 审核服务检查新昵称。
     * 根据审核结果决定是否立即更新昵称，并将审核记录写入 tb_user_data_change_lock 表。
     * </p>
     *
     * <h3>审核结果处理</h3>
     * <ul>
     *   <li>normal（通过）：直接更新昵称，lock 记录 approval_status=10</li>
     *   <li>neutral / AI 不可用（待审核）：不更新昵称，lock 记录 approval_status=20</li>
     *   <li>violation（违规）：不更新昵称，lock 记录 approval_status=30</li>
     * </ul>
     *
     * <h3>使用场景</h3>
     * <ol>
     *   <li>用户自行修改昵称时调用此方法（经过 AI 审核）</li>
     *   <li>管理员直接修改昵称时调用 {@link #updateUserNickname}（跳过审核）</li>
     * </ol>
     *
     * @param userId   用户 ID
     * @param nickname 新昵称
     * @param adminId  执行操作的用户 ID（用户自己更新时传自身 ID）
     * @return 昵称修改结果，包含成功状态、审核状态和审核原因
     * @author PlayerEG
     * @see #updateUserNickname(Integer, String, Integer)
     */
    top.playereg.pix_vision.pojo.NicknameChangeResult updateNicknameWithAudit(Integer userId, String nickname, Integer adminId);

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
     * 检查用户是否已存在指定类型的拓展数据
     *
     * @param userId   用户 ID
     * @param dataName 数据类型名称
     * @return true-已存在，false-不存在
     */
    boolean isUserDataExists(Integer userId, String dataName);

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
     * 根据用户名精确查询用户信息
     *
     * @param username 用户名
     * @return 用户对象，如果不存在返回 null
     */
    User selectUserByUsername(String username);

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

    /**
     * 管理员批量重置用户密码（仅系统管理员可调用）
     * <p>
     * 为指定用户列表生成随机密码，更新数据库并强制下线所有设备。
     * 返回包含用户 ID、邮箱和明文密码的列表，用于后续邮件发送。
     * </p>
     *
     * @param userIds 目标用户 ID 列表
     * @return 重置结果列表，每个元素包含 user_id, email, username, plainPassword
     */
    java.util.List<java.util.Map<String, Object>> batchResetUserPasswords(java.util.List<Integer> userIds);
}
