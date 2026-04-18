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
import top.playereg.pix_vision.util.StrSwitchUtils;

@Service
public class UserServiceImpl implements UserService {
    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private UserDataMapper userDataMapper;

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
        log.info("注册用户");
        if (isUsernameExists(username)) {
            log.error("用户名已存在");
            return null;
        }
        if (isEmailExists(email)) {
            log.error("邮箱已存在");
            return null;
        }
        User user = new User();

        // 设置用户信息
        user.setUsername(username);
        log.info("用户名：{}", user.getUsername());
        user.setPassword(password);
        log.info("密码：{}", user.getPassword());
        user.setNickname(nickname);
        log.info("昵称：{}", user.getNickname());
        user.setEmail(email);
        log.info("邮箱：{}", user.getEmail());

        // 生成用户 UUID（16 字节二进制）
        user.setUser_uuid(StrSwitchUtils.uuid2Bytes(StrSwitchUtils.generateUUID()));
        log.info("用户 UUID (hex): {}", StrSwitchUtils.bytes2Uuid(user.getUser_uuid()));

        // 默认随机头像（1.png-21.png）
        int randomAvatarNum = (int) (Math.random() * 21) + 1;  // 生成 1-21 的随机整数
        String randomAvatar = randomAvatarNum + ".png";
        user.setAvatar_url("default/" + randomAvatar);
        log.info("用户头像：{}", user.getAvatar_url());

        user.setStatus(10);
        log.info("用户状态：{}", user.getStatus());

        // 设置用户角色（默认为普通用户 11）
        user.setUser_role(11);
        log.info("用户角色：{}", user.getUser_role());

        user.setIs_delete(false);
        log.info("用户删除状态：{}", user.getIs_delete());

        // 创建时间
        user.setCreate_time(new java.sql.Timestamp(System.currentTimeMillis()));
        // 创建用户
        user.setCreate_user(0);

        return userMapper.insertUser(user) > 0 ? user : null;
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
        log.info("新增用户拓展数据，用户 ID: {}, 数据名称: {}, 数据内容: {}", userId, dataName, dataContent);

        // 参数校验
        if (userId == null || userId <= 0) {
            log.error("用户 ID 无效: {}", userId);
            return false;
        }

        if (dataName == null || dataName.isEmpty()) {
            log.error("数据名称不能为空");
            return false;
        }

        if (dataContent == null || dataContent.isEmpty()) {
            log.error("数据内容不能为空");
            return false;
        }

        // 验证数据名称长度（不超过 26 个字符）
        if (dataName.length() > 26) {
            log.error("数据名称长度不能超过 26 个字符，当前长度: {}", dataName.length());
            return false;
        }

        // 验证数据内容长度（不超过 96 个字符）
        if (dataContent.length() > 96) {
            log.error("数据内容长度不能超过 96 个字符，当前长度: {}", dataContent.length());
            return false;
        }

        // 检查用户是否存在
        User user = userMapper.selectAllUserInfoById(userId);
        if (user == null) {
            log.error("用户不存在，用户 ID: {}", userId);
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
            log.info("用户拓展数据添加成功，用户 ID: {}, 数据名称: {}", userId, dataName);
            return true;
        } else {
            log.error("用户拓展数据添加失败，用户 ID: {}, 数据名称: {}", userId, dataName);
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
    public java.util.List<UserData> getUserDataList(Integer userId) {
        log.info("查询用户拓展数据，用户 ID: {}", userId);

        // 参数校验
        if (userId == null || userId <= 0) {
            log.error("用户 ID 无效: {}", userId);
            return null;
        }

        // 先检查用户是否存在
        User user = userMapper.selectAllUserInfoById(userId);
        if (user == null) {
            log.warn("用户不存在，用户 ID: {}", userId);
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
     * 删除用户拓展数据（只能删除自己的数据）
     *
     * @param dataId 数据 ID
     * @param userId 用户 ID（从 Token 中获取，用于权限验证）
     * @return 是否成功
     */
    @Override
    public Boolean deleteUserData(Integer dataId, Integer userId) {
        log.info("删除用户拓展数据，数据 ID: {}, 用户 ID: {}", dataId, userId);

        // 参数校验
        if (dataId == null || dataId <= 0) {
            log.error("数据 ID 无效: {}", dataId);
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

        // 执行逻辑删除（SQL 中已包含 user_id 验证，确保只能删除自己的数据）
        int result = userDataMapper.deleteUserDataById(dataId, userId);

        if (result > 0) {
            log.info("用户拓展数据删除成功，数据 ID: {}, 用户 ID: {}", dataId, userId);
            return true;
        } else {
            log.warn("用户拓展数据删除失败，可能原因：数据不存在、不属于当前用户、或已被删除，数据 ID: {}, 用户 ID: {}", dataId, userId);
            return false;
        }
    }
}
