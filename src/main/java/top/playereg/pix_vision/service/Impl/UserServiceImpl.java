package top.playereg.pix_vision.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.playereg.pix_vision.mapper.UserDataMapper;
import top.playereg.pix_vision.mapper.UserMapper;
import top.playereg.pix_vision.pojo.userPojo.User;
import top.playereg.pix_vision.pojo.userPojo.UserData;
import top.playereg.pix_vision.service.UserService;
import top.playereg.pix_vision.service.VerificationCodeServices;
import top.playereg.pix_vision.util.StrSwitchUtils;

import java.util.List;

@Service
public class UserServiceImpl implements UserService {
    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private UserDataMapper userDataMapper;
    @Autowired
    private VerificationCodeServices verificationCodeServices;

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
        return userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username)) > 0;
    }

    /**
     * 检查邮箱是否存在
     */
    private boolean isEmailExists(String email) {
        return userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getEmail, email)) > 0;
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

    /**
     * 分页查询用户信息
     *
     * @param page     分页参数
     * @param username 用户名（可选）
     * @param uuid     UUID（可选）
     * @param email    邮箱（可选）
     * @return 分页用户列表
     */
    @Override
    public IPage<User> selectPageUserInfo(
            IPage<?> page,
            String username,
            byte[] uuid,
            String email
    ) {
        log.info("分页查询用户信息");

        // 构建查询条件对象
        User queryUser = new User();
        if (username != null && !username.isEmpty()) {
            queryUser.setUsername(username);
            log.info("查询条件 - 用户名：{}", username);
        }
        if (uuid != null) {
            queryUser.setUser_uuid(uuid);
            log.info("查询条件 - UUID: {}", StrSwitchUtils.bytes2Uuid(uuid));
        }
        if (email != null && !email.isEmpty()) {
            queryUser.setEmail(email);
            log.info("查询条件 - 邮箱：{}", email);
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
}
