package top.playereg.pix_vision.service.Impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import top.playereg.pix_vision.mapper.UserDataMapper;
import top.playereg.pix_vision.mapper.UserMapper;
import top.playereg.pix_vision.pojo.userPojo.User;
import top.playereg.pix_vision.pojo.userPojo.UserData;
import top.playereg.pix_vision.service.TokenWhitelistService;
import top.playereg.pix_vision.service.UserService;
import top.playereg.pix_vision.service.VerificationCodeServices;
import top.playereg.pix_vision.util.PixVisionLogger;
import top.playereg.pix_vision.util.StrSwitchUtils;

import java.util.List;

@Service
public class UserServiceImpl implements UserService {
    private static final PixVisionLogger log = PixVisionLogger.create(UserServiceImpl.class);

    /**
     * Redis Key 前缀：role:{user_id}
     */
    private static final String ROLE_CACHE_PREFIX = "role:";

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private UserDataMapper userDataMapper;
    @Autowired
    private VerificationCodeServices verificationCodeServices;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    @Autowired
    private TokenWhitelistService tokenWhitelistService;

    /**
     * 注册用户
     *
     * @param username 用户名
     * @param password 密码
     * @param nickname 昵称
     * @param email    邮箱
     * @return 插入结果
     */
    @Override
    public User registerUser(
            String username,
            String password,
            String nickname,
            String email
    ) {
        log.info("开始注册用户 - 用户名: {}, 邮箱: {}", username, email);

        if (isUsernameExists(username)) {
            log.warn("注册失败 - 用户名已存在: {}", username);
            return null;
        }
        if (isEmailExists(email)) {
            log.warn("注册失败 - 邮箱已存在: {}", email);
            return null;
        }

        User user = new User();

        // 设置用户信息
        user.setUsername(username);
        user.setPassword(password);  // 密码已在 Controller 层加密
        user.setNickname(nickname);
        user.setEmail(email);

        // 生成用户 UUID（16 字节二进制）
        user.setUser_uuid(StrSwitchUtils.uuid2Bytes(StrSwitchUtils.generateUUID()));

        // 默认随机头像（1.png-21.png）
        int randomAvatarNum = (int) (Math.random() * 21) + 1;  // 生成 1-21 的随机整数
        String randomAvatar = randomAvatarNum + ".png";
        user.setAvatar_url("default/" + randomAvatar);

        user.setStatus(10);
        user.setUser_role(11);  // 默认为普通用户
        user.setIs_delete(false);

        // 创建时间
        user.setCreate_time(new java.sql.Timestamp(System.currentTimeMillis()));
        user.setCreate_user(0);

        boolean success = userMapper.insertUser(user) > 0;
        if (success) {
            log.info("用户注册成功 - 用户名: {}, 用户 ID: {}", username, user.getUser_id());
        } else {
            log.error("用户注册失败 - 用户名: {}", username);
        }

        return success ? user : null;
    }
    /**
     * 检查用户名是否存在
     */
    private boolean isUsernameExists(String username) {
        return userMapper.countByUsername(username) > 0;
    }

    /**
     * 检查邮箱是否存在
     */
    private boolean isEmailExists(String email) {
        return userMapper.countByEmail(email) > 0;
    }
    @Override
    public User selectAllUserByUsername(String username) {
        return userMapper.selectAllUserInfoByUsername(username);
    }

    @Override
    public User selectAllUserByEmail(String email) {
        return userMapper.selectAllUserInfoByEmail(email);
    }

    @Override
    public User selectAllUserById(Integer userId) {
        return userMapper.selectAllUserInfoById(userId);
    }

    @Override
    public User selectUserRoleById(Integer userId) {
        return userMapper.selectUserRoleById(userId);
    }

    /**
     * 分页查询用户信息（支持关键词统一查询）
     *
     * @param page      分页参数
     * @param keyword   关键词（可选，模糊查询用户名/邮箱/昵称）
     * @param uuidBytes UUID 字节数组（可选，精确查询）
     * @return 分页用户列表
     */
    @Override
    public IPage<User> selectPageUserInfo(
        IPage<User> page,
        String keyword,
        byte[] uuidBytes
    ) {
        log.info("分页查询用户信息");

        // 构建查询条件对象
        User queryUser = new User();

        if (uuidBytes != null) {
            // UUID 精确查询
            queryUser.setUser_uuid(uuidBytes);
            log.info("查询条件 - UUID: {}", StrSwitchUtils.bytes2Uuid(uuidBytes));
        }

        if (keyword != null && !keyword.isEmpty()) {
            // 关键词模糊查询（同时搜索用户名、邮箱、昵称）
            queryUser.setUsername(keyword);
            queryUser.setEmail(keyword);
            queryUser.setNickname(keyword);
            log.info("查询条件 - 关键词（用户名/邮箱/昵称）: {}", keyword);
        }

        log.info("执行分页查询");
        return userMapper.selectPageUserInfo(page, queryUser);
    }

    /**
     * 用户密码修改（通过邮箱）
     * @param email 用户的邮箱
     * @param oldPassword 用户的旧密码
     * @param newPassword 用户的新密码
     * @return 影响行数
     * */
    public Integer changeUserLoginPasswordByEmail(String email, String oldPassword, String newPassword ){
        return userMapper.changeUserPassword(email, oldPassword, newPassword);
    }

    /**
     * 忘记密码 - 重置密码（无需登录）
     * @param usernameOrEmail 用户名或邮箱
     * @param newPassword 新密码
     * @param confirmPassword 确认新密码
     * @param vCode 邮箱验证码
     * @return 是否成功
     */
    @Override
    public Boolean resetPasswordByUsernameOrEmail(String usernameOrEmail, String newPassword, String confirmPassword, String vCode) {
        log.info("忘记密码 - 开始重置密码，用户名或邮箱：{}", usernameOrEmail);

        // 查询用户信息
        User user;
        if (usernameOrEmail.contains("@")) {
            // 是邮箱
            user = userMapper.selectAllUserInfoByEmail(usernameOrEmail);
            log.info("通过邮箱查询用户：{}, 结果：{}", usernameOrEmail, user != null ? "找到" : "未找到");
        } else {
            // 是用户名
            user = userMapper.selectAllUserInfoByUsername(usernameOrEmail);
            log.info("通过用户名查询用户：{}, 结果：{}", usernameOrEmail, user != null ? "找到" : "未找到");
        }

        // 用户不存在
        if (user == null) {
            log.warn("忘记密码 - 用户不存在：{}", usernameOrEmail);
            return false;
        }

        String email = user.getEmail();
        log.info("忘记密码 - 找到用户，邮箱：{}", email);

        // 更新密码（传入 null 作为 oldPassword，不验证旧密码）
        int result = userMapper.changeUserPassword(email, null, newPassword);

        if (result > 0) {
            log.info("忘记密码 - 密码重置成功，邮箱：{}", email);
            return true;
        } else {
            log.error("忘记密码 - 密码重置失败，邮箱：{}", email);
            return false;
        }
    }

    /**
     * 更新用户头像
     * @param userId 用户 ID
     * @param avatarUrl 头像路径
     * @return 是否成功
     */
    @Override
    public Boolean updateUserAvatar(Integer userId, String avatarUrl) {
        log.info("更新用户头像，用户 ID: {}, 头像路径: {}", userId, avatarUrl);

        if (userId == null || userId <= 0) {
            log.error("用户 ID 无效: {}", userId);
            return false;
        }

        if (avatarUrl == null || avatarUrl.isEmpty()) {
            log.error("头像路径不能为空");
            return false;
        }

        int result = userMapper.updateUserAvatar(userId, avatarUrl);

        if (result > 0) {
            log.info("用户头像更新成功，用户 ID: {}", userId);
            return true;
        } else {
            log.error("用户头像更新失败，用户 ID: {}", userId);
            return false;
        }
    }

    /**
     * 更新用户昵称
     *
     * @param userId   用户 ID
     * @param nickname 新昵称
     * @return 是否成功
     */
    @Override
    public Boolean updateUserNickname(Integer userId, String nickname) {
        log.info("更新用户昵称，用户 ID: {}, 新昵称: {}", userId, nickname);

        if (userId == null || userId <= 0) {
            log.error("用户 ID 无效: {}", userId);
            return false;
        }

        if (nickname == null || nickname.isEmpty()) {
            log.error("昵称不能为空");
            return false;
        }

        // 验证昵称长度（1-20 个字符）
        if (nickname.length() < 1 || nickname.length() > 20) {
            log.error("昵称长度必须在 1-20 个字符之间，当前长度: {}", nickname.length());
            return false;
        }

        int result = userMapper.updateUserNickname(userId, nickname);

        if (result > 0) {
            log.info("用户昵称更新成功，用户 ID: {}, 新昵称: {}", userId, nickname);
            return true;
        } else {
            log.error("用户昵称更新失败，用户 ID: {}", userId);
            return false;
        }
    }

    /**
     * 新增用户拓展数据
     *
     * @param userId      用户 ID
     * @param dataName    数据名称（电话、邮箱、网站、微信等）
     * @param dataContent 数据内容（具体的电话号码、邮箱地址、网站 url 等）
     * @return 是否成功
     */
    @Override
    public Boolean addUserData(Integer userId, String dataName, String dataContent) {
        log.debug("新增用户拓展数据 - 用户 ID: {}, 数据名称: {}", userId, dataName);

        // 参数校验
        if (userId == null || userId <= 0) {
            log.warn("用户 ID 无效: {}", userId);
            return false;
        }

        if (dataName == null || dataName.isEmpty()) {
            log.warn("数据名称不能为空");
            return false;
        }

        if (dataContent == null || dataContent.isEmpty()) {
            log.warn("数据内容不能为空");
            return false;
        }

        // 验证数据名称长度（不超过 26 个字符）
        if (dataName.length() > 26) {
            log.warn("数据名称过长 - 长度: {} (最大 26)", dataName.length());
            return false;
        }

        // 验证数据内容长度（不超过 96 个字符）
        if (dataContent.length() > 96) {
            log.warn("数据内容过长 - 长度: {} (最大 96)", dataContent.length());
            return false;
        }

        // 检查用户是否存在
        User user = userMapper.selectAllUserInfoById(userId);
        if (user == null) {
            log.warn("用户不存在 - 用户 ID: {}", userId);
            return false;
        }

        // 创建用户拓展数据对象
        UserData userData = new UserData();
        userData.setUser_id(userId);
        userData.setUser_data_name(dataName);
        userData.setUser_data(dataContent);
        userData.setIs_delete(false);
        userData.setCreate_time(new java.sql.Timestamp(System.currentTimeMillis()));
        userData.setCreate_user(0); // 系统创建为 0

        // 插入数据
        int result = userDataMapper.insertUserData(userData);

        if (result > 0) {
            log.info("用户拓展数据添加成功 - 用户 ID: {}, 数据名称: {}", userId, dataName);
            return true;
        } else {
            log.error("用户拓展数据添加失败 - 用户 ID: {}, 数据名称: {}", userId, dataName);
            return false;
        }
    }

    /**
     * 查询用户所有拓展数据
     *
     * @param userId 用户 ID
     * @return 用户拓展数据列表，如果用户不存在则返回 null
     */
    @Override
    public List<UserData> getUserDataList(Integer userId) {
        log.debug("查询用户拓展数据 - 用户 ID: {}", userId);

        // 参数校验
        if (userId == null || userId <= 0) {
            log.warn("用户 ID 无效: {}", userId);
            return null;
        }

        // 先检查用户是否存在
        User user = userMapper.selectAllUserInfoById(userId);
        if (user == null) {
            log.warn("用户不存在 - 用户 ID: {}", userId);
            return null;
        }

        // 查询用户的所有拓展数据（排除逻辑删除）
        java.util.List<UserData> userDataList = userDataMapper.selectUserDataByUserId(userId);

        if (userDataList == null) {
            log.info("用户没有拓展数据，用户 ID: {}", userId);
            return new java.util.ArrayList<>();
        }

        log.info("查询到用户拓展数据，用户 ID: {}, 数据条数: {}", userId, userDataList.size());
        return userDataList;
    }

    /**
     * 批量删除用户拓展数据（支持单条和批量，只能删除自己的数据）
     *
     * @param dataIds 数据 ID 列表（单条删除时传入单个元素的列表）
     * @param userId  用户 ID（从 Token 中获取，用于权限验证）
     * @return 是否成功
     */
    @Override
    public Boolean batchDeleteUserData(List<Integer> dataIds, Integer userId) {
        log.info("批量删除用户拓展数据，数据 ID 数量: {}, 用户 ID: {}", dataIds != null ? dataIds.size() : 0, userId);

        // 参数校验
        if (dataIds == null || dataIds.isEmpty()) {
            log.error("数据 ID 列表为空");
            return false;
        }

        if (userId == null || userId <= 0) {
            log.error("用户 ID 无效: {}", userId);
            return false;
        }

        // 检查用户是否存在
        User user = userMapper.selectAllUserInfoById(userId);
        if (user == null) {
            log.warn("用户不存在，用户 ID: {}", userId);
            return false;
        }

        // 执行批量逻辑删除（SQL 中已包含 user_id 验证，确保只能删除自己的数据）
        int result = userDataMapper.batchDeleteUserDataByIds(dataIds, userId);

        if (result > 0) {
            log.info("批量删除用户拓展数据成功，删除数量: {}, 用户 ID: {}", result, userId);
            return true;
        } else {
            log.warn("批量删除用户拓展数据失败，可能原因：数据不存在、不属于当前用户、或已被删除，用户 ID: {}", userId);
            return false;
        }
    }

    /**
     * 注销用户账户（逻辑删除）
     *
     * @param userId 用户 ID
     * @return 是否成功
     */
    @Override
    public Boolean deleteUserAccount(Integer userId) {
        log.info("开始注销用户账户，用户 ID: {}", userId);

        // 参数校验
        if (userId == null || userId <= 0) {
            log.error("用户 ID 无效: {}", userId);
            return false;
        }

        // 检查用户是否存在
        User user = userMapper.selectAllUserInfoById(userId);
        if (user == null) {
            log.warn("用户不存在，用户 ID: {}", userId);
            return false;
        }

        // 执行逻辑删除
        int result = userMapper.deleteUserAccount(userId);

        if (result > 0) {
            log.info("用户账户注销成功，用户 ID: {}, 用户名: {}", userId, user.getUsername());
            return true;
        } else {
            log.warn("用户账户注销失败，用户 ID: {}", userId);
            return false;
        }
    }

    /**
     * 根据用户名或邮箱查询用户信息（智能识别）
     *
     * @param usernameOrEmail 用户名或邮箱地址
     * @return 用户对象，如果不存在返回 null
     */
    @Override
    public User selectUserByUsernameOrEmail(String usernameOrEmail) {
        if (usernameOrEmail == null || usernameOrEmail.isEmpty()) {
            log.warn("查询用户信息失败：参数为空");
            return null;
        }

        User user;
        // 判断是邮箱还是用户名
        if (usernameOrEmail.contains("@")) {
            // 是邮箱格式
            log.info("通过邮箱查询用户：{}", usernameOrEmail);
            user = userMapper.selectAllUserInfoByEmail(usernameOrEmail);
        } else {
            // 是用户名格式
            log.info("通过用户名查询用户：{}", usernameOrEmail);
            user = userMapper.selectAllUserInfoByUsername(usernameOrEmail);
        }

        if (user != null) {
            log.info("查询到用户 - 用户名：{}, 邮箱：{}", user.getUsername(), user.getEmail());
        } else {
            log.warn("用户不存在：{}", usernameOrEmail);
        }

        return user;
    }

    /**
     * 更新用户绑定邮箱（需要验证码验证）
     *
     * @param userId   用户 ID
     * @param newEmail 新邮箱
     * @param vCode    邮箱验证码
     * @return 是否成功
     */
    @Override
    public Boolean updateUserEmail(Integer userId, String newEmail, String vCode) {
        log.info("开始更新用户邮箱，用户 ID: {}, 新邮箱: {}", userId, newEmail);

        // 参数校验
        if (userId == null || userId <= 0) {
            log.error("用户 ID 无效: {}", userId);
            return false;
        }

        if (newEmail == null || newEmail.isEmpty()) {
            log.error("新邮箱不能为空");
            return false;
        }

        if (vCode == null || vCode.isEmpty()) {
            log.error("验证码不能为空");
            return false;
        }

        // 查询当前用户信息
        User currentUser = userMapper.selectAllUserInfoById(userId);
        if (currentUser == null) {
            log.warn("用户不存在 - 用户 ID: {}", userId);
            return false;
        }

        String oldEmail = currentUser.getEmail();
        log.info("当前用户邮箱: {}, 准备更新为: {}", oldEmail, newEmail);

        // 检查新邮箱是否已被其他用户使用
        User existingUser = userMapper.selectAllUserInfoByEmail(newEmail);
        if (existingUser != null && !existingUser.getUser_id().equals(userId)) {
            log.warn("新邮箱已被其他用户使用: {}", newEmail);
            return false;
        }

        // 验证邮箱验证码（使用新邮箱作为 key）
        boolean isVerified = verificationCodeServices.verificationCodeVerify(newEmail, vCode);
        if (!isVerified) {
            log.warn("邮箱验证码验证失败 - 新邮箱: {}", newEmail);
            return false;
        }

        // 更新邮箱
        int result = userMapper.updateUserEmail(userId, newEmail);

        if (result > 0) {
            log.info("用户邮箱更新成功 - 用户 ID: {}, 旧邮箱: {}, 新邮箱: {}", userId, oldEmail, newEmail);
            return true;
        } else {
            log.error("用户邮箱更新失败 - 用户 ID: {}", userId);
            return false;
        }
    }

    /**
     * 清除用户角色缓存
     * <p>
     * 当用户角色发生变更时调用此方法，确保权限验证获取最新的角色信息
     * </p>
     *
     * @param userId 用户 ID
     */
    @Override
    public void clearUserRoleCache(Integer userId) {
        try {
            String key = ROLE_CACHE_PREFIX + userId;
            Boolean deleted = redisTemplate.delete(key);
            if (deleted != null && deleted) {
                log.info("用户角色缓存已清除，用户 ID: {}", userId);
            } else {
                log.debug("用户角色缓存不存在或已过期，用户 ID: {}", userId);
            }
        } catch (Exception e) {
            log.error("清除用户角色缓存失败，用户 ID: {}, 错误: {}", userId, e.getMessage());
        }
    }

    /**
     * 清除所有用户角色缓存
     * <p>
     * 批量删除 Redis 中所有 role: 前缀的用户角色缓存
     * 使用 SCAN 命令避免阻塞 Redis
     * </p>
     *
     * @return 清除的缓存数量
     */
    @Override
    public int clearAllUserRoleCache() {
        int clearedCount = 0;
        try {
            // 使用 SCAN 命令遍历所有 role: 前缀的键
            // SCAN 是非阻塞的，比 KEYS 更安全
            String pattern = ROLE_CACHE_PREFIX + "*";

            // 使用 scan 方法，每次扫描 100 个键
            org.springframework.data.redis.core.Cursor<byte[]> cursor = redisTemplate.executeWithStickyConnection(
                connection -> {
                    org.springframework.data.redis.core.ScanOptions scanOptions =
                        org.springframework.data.redis.core.ScanOptions.scanOptions()
                            .match(pattern)
                            .count(100)
                            .build();
                    return connection.keyCommands().scan(scanOptions);
                }
            );

            if (cursor != null) {
                java.util.List<String> keysToDelete = new java.util.ArrayList<>();

                // 收集所有匹配的键
                while (cursor.hasNext()) {
                    byte[] keyBytes = cursor.next();
                    String key = new String(keyBytes);
                    keysToDelete.add(key);
                }

                // 关闭 cursor
                cursor.close();

                // 批量删除
                if (!keysToDelete.isEmpty()) {
                    Long deletedCount = redisTemplate.delete(keysToDelete);
                    clearedCount = deletedCount != null ? deletedCount.intValue() : 0;
                    log.info("批量清除用户角色缓存成功 - 清除数量: {}", clearedCount);
                } else {
                    log.info("未找到需要清除的角色缓存");
                }
            }
        } catch (Exception e) {
            log.error("清除所有用户角色缓存失败 - 错误: {}", e.getMessage(), e);
        }

        return clearedCount;
    }

    /**
     * 更新用户角色（仅系统管理员可调用）
     *
     * @param targetUserId 目标用户 ID
     * @param newRole      新角色代码
     * @param adminId      执行操作的管理员 ID
     * @return 是否成功
     */
    @Override
    public Boolean updateUserRole(Integer targetUserId, Integer newRole, Integer adminId) {
        log.info("开始更新用户角色 - 目标用户 ID: {}, 新角色: {}, 管理员 ID: {}", targetUserId, newRole, adminId);

        // 参数校验
        if (targetUserId == null || targetUserId <= 0) {
            log.error("目标用户 ID 无效: {}", targetUserId);
            return false;
        }

        if (newRole == null) {
            log.error("新角色不能为空");
            return false;
        }

        // 验证角色代码是否合法
        List<Integer> validRoles = List.of(11, 22, 55, 66, 77);
        if (!validRoles.contains(newRole)) {
            log.error("无效的角色代码: {}，允许的角色代码: {}", newRole, validRoles);
            return false;
        }

        // 检查目标用户是否存在
        User targetUser = userMapper.selectAllUserInfoById(targetUserId);
        if (targetUser == null) {
            log.warn("目标用户不存在 - 用户 ID: {}", targetUserId);
            return false;
        }

        // 检查管理员是否存在
        User adminUser = userMapper.selectAllUserInfoById(adminId);
        if (adminUser == null) {
            log.error("管理员不存在 - 管理员 ID: {}", adminId);
            return false;
        }

        // 防止修改自己的角色（可选的安全措施）
        if (targetUserId.equals(adminId)) {
            log.warn("管理员不能修改自己的角色 - 用户 ID: {}", adminId);
            return false;
        }

        Integer oldRole = targetUser.getUser_role();
        log.info("当前用户角色: {}, 准备更新为: {}", oldRole, newRole);

        // 执行角色更新
        int result = userMapper.updateUserRole(targetUserId, newRole, adminId);

        if (result > 0) {
            log.info("用户角色更新成功 - 用户 ID: {}, 旧角色: {}, 新角色: {}, 管理员: {}",
                    targetUserId, oldRole, newRole, adminId);

            // 清除该用户的角色缓存，确保下次请求时获取最新的角色信息
            clearUserRoleCache(targetUserId);

            return true;
        } else {
            log.error("用户角色更新失败 - 用户 ID: {}", targetUserId);
            return false;
        }
    }

    /**
     * 更新用户状态（仅系统管理员可调用）
     *
     * @param targetUserId 目标用户 ID
     * @param newStatus    新状态代码
     * @param adminId      执行操作的管理员 ID
     * @return 是否成功
     */
    @Override
    public Boolean updateUserStatus(Integer targetUserId, Integer newStatus, Integer adminId) {
        log.info("开始更新用户状态 - 目标用户 ID: {}, 新状态: {}, 管理员 ID: {}", targetUserId, newStatus, adminId);

        // 参数校验
        if (targetUserId == null || targetUserId <= 0) {
            log.error("目标用户 ID 无效: {}", targetUserId);
            return false;
        }

        if (newStatus == null) {
            log.error("新状态不能为空");
            return false;
        }

        // 验证状态代码是否合法
        List<Integer> validStatuses = List.of(10, 20, 30);
        if (!validStatuses.contains(newStatus)) {
            log.error("无效的状态代码: {}，允许的状态代码: {}", newStatus, validStatuses);
            return false;
        }

        // 检查目标用户是否存在
        User targetUser = userMapper.selectAllUserInfoById(targetUserId);
        if (targetUser == null) {
            log.warn("目标用户不存在 - 用户 ID: {}", targetUserId);
            return false;
        }

        // 检查管理员是否存在
        User adminUser = userMapper.selectAllUserInfoById(adminId);
        if (adminUser == null) {
            log.error("管理员不存在 - 管理员 ID: {}", adminId);
            return false;
        }

        // 防止修改自己的状态（可选的安全措施）
        if (targetUserId.equals(adminId)) {
            log.warn("管理员不能修改自己的状态 - 用户 ID: {}", adminId);
            return false;
        }

        Integer oldStatus = targetUser.getStatus();
        log.info("当前用户状态: {}, 准备更新为: {}", oldStatus, newStatus);

        // 执行状态更新
        int result = userMapper.updateUserStatus(targetUserId, newStatus, adminId);

        if (result > 0) {
            log.info("用户状态更新成功 - 用户 ID: {}, 旧状态: {}, 新状态: {}, 管理员: {}",
                    targetUserId, oldStatus, newStatus, adminId);
            return true;
        } else {
            log.error("用户状态更新失败 - 用户 ID: {}", targetUserId);
            return false;
        }
    }

    /**
     * 删除用户账户（仅系统管理员可调用）
     *
     * @param targetUserId 目标用户 ID
     * @param adminId      执行操作的管理员 ID
     * @return 是否成功
     */
    @Override
    public Boolean deleteUserAccountByAdmin(Integer targetUserId, Integer adminId) {
        log.info("管理员开始删除用户账户 - 目标用户 ID: {}, 管理员 ID: {}", targetUserId, adminId);

        // 参数校验
        if (targetUserId == null || targetUserId <= 0) {
            log.error("目标用户 ID 无效: {}", targetUserId);
            return false;
        }

        if (adminId == null || adminId <= 0) {
            log.error("管理员 ID 无效: {}", adminId);
            return false;
        }

        // 检查目标用户是否存在
        User targetUser = userMapper.selectAllUserInfoById(targetUserId);
        if (targetUser == null) {
            log.warn("目标用户不存在 - 用户 ID: {}", targetUserId);
            return false;
        }

        // 检查管理员是否存在
        User adminUser = userMapper.selectAllUserInfoById(adminId);
        if (adminUser == null) {
            log.error("管理员不存在 - 管理员 ID: {}", adminId);
            return false;
        }

        // 防止删除自己（安全措施）
        if (targetUserId.equals(adminId)) {
            log.warn("管理员不能删除自己的账户 - 用户 ID: {}", adminId);
            return false;
        }

        log.info("准备删除用户 - 用户名: {}, 邮箱: {}", targetUser.getUsername(), targetUser.getEmail());

        // 执行逻辑删除
        int result = userMapper.deleteUserAccount(targetUserId);

        if (result > 0) {
            log.info("用户账户删除成功 - 用户 ID: {}, 用户名: {}, 管理员: {}",
                    targetUserId, targetUser.getUsername(), adminId);

            // 清除该用户的所有 Token，强制所有设备下线
            int removedTokens = tokenWhitelistService.removeAllUserTokens(
                    targetUserId, targetUser.getUsername());
            log.info("已清除用户 Token - 用户 ID: {}, 清除数量: {}", targetUserId, removedTokens);

            // 清除用户角色缓存
            clearUserRoleCache(targetUserId);

            return true;
        } else {
            log.error("用户账户删除失败 - 用户 ID: {}", targetUserId);
            return false;
        }
    }

    /**
     * 管理员创建新用户（仅系统管理员可调用）
     *
     * @param username 用户名
     * @param password 密码（明文）
     * @param nickname 昵称
     * @param email    邮箱
     * @param role     角色代码（可选，默认为 11-普通用户）
     * @param status   状态代码（可选，默认为 10-正常）
     * @param adminId  执行操作的管理员 ID
     * @return 创建的用户对象，失败返回 null
     */
    @Override
    public User createUserByAdmin(String username, String password, String nickname, String email,
                                  Integer role, Integer status, Integer adminId) {
        log.info("管理员开始创建新用户 - 用户名: {}, 邮箱: {}, 管理员 ID: {}", username, email, adminId);

        // 参数校验
        if (username == null || username.isEmpty()) {
            log.error("用户名不能为空");
            return null;
        }

        if (password == null || password.isEmpty()) {
            log.error("密码不能为空");
            return null;
        }

        if (nickname == null || nickname.isEmpty()) {
            log.error("昵称不能为空");
            return null;
        }

        if (email == null || email.isEmpty()) {
            log.error("邮箱不能为空");
            return null;
        }

        if (adminId == null || adminId <= 0) {
            log.error("管理员 ID 无效: {}", adminId);
            return null;
        }

        // 检查用户名是否已存在
        if (isUsernameExists(username)) {
            log.warn("创建失败 - 用户名已存在: {}", username);
            return null;
        }

        // 检查邮箱是否已存在
        if (isEmailExists(email)) {
            log.warn("创建失败 - 邮箱已存在: {}", email);
            return null;
        }

        // 检查管理员是否存在
        User adminUser = userMapper.selectAllUserInfoById(adminId);
        if (adminUser == null) {
            log.error("管理员不存在 - 管理员 ID: {}", adminId);
            return null;
        }

        // 设置默认值
        if (role == null) {
            role = 11; // 默认为普通用户
        }
        if (status == null) {
            status = 10; // 默认为正常状态
        }

        // 验证角色代码是否合法
        List<Integer> validRoles = List.of(11, 22, 55, 66, 77);
        if (!validRoles.contains(role)) {
            log.error("无效的角色代码: {}，允许的角色代码: {}", role, validRoles);
            return null;
        }

        // 验证状态代码是否合法
        List<Integer> validStatuses = List.of(10, 20, 30);
        if (!validStatuses.contains(status)) {
            log.error("无效的状态代码: {}，允许的状态代码: {}", status, validStatuses);
            return null;
        }

        // 创建用户对象
        User user = new User();

        // 设置用户信息
        user.setUsername(username);
        // 密码加密（SHA-256）
        user.setPassword(StrSwitchUtils.PasswdToHash256(password));
        user.setNickname(nickname);
        user.setEmail(email);

        // 生成用户 UUID（16 字节二进制）
        user.setUser_uuid(StrSwitchUtils.uuid2Bytes(StrSwitchUtils.generateUUID()));

        // 默认随机头像（1.png-21.png）
        int randomAvatarNum = (int) (Math.random() * 21) + 1;
        String randomAvatar = randomAvatarNum + ".png";
        user.setAvatar_url("default/" + randomAvatar);

        // 设置角色和状态
        user.setUser_role(role);
        user.setStatus(status);
        user.setIs_delete(false);

        // 创建时间
        user.setCreate_time(new java.sql.Timestamp(System.currentTimeMillis()));
        user.setCreate_user(adminId); // 记录创建者为管理员

        // 插入数据库
        boolean success = userMapper.insertUser(user) > 0;
        if (success) {
            log.info("管理员创建用户成功 - 用户名: {}, 用户 ID: {}, 角色: {}, 状态: {}, 管理员: {}",
                    username, user.getUser_id(), role, status, adminId);
        } else {
            log.error("管理员创建用户失败 - 用户名: {}", username);
        }

        return success ? user : null;
    }
}
