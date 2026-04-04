package top.playereg.pix_vision.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.playereg.pix_vision.mapper.UserMapper;
import top.playereg.pix_vision.pojo.userPojo.User;
import top.playereg.pix_vision.service.UserService;
import top.playereg.pix_vision.util.StrSwitchUtils;

@Service
public class UserServiceImpl implements UserService {
    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);
    @Autowired
    private UserMapper userMapper;

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
        user.setAvatar_url("/default/" + randomAvatar);
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
    public Integer changeUserPasswordByEmail( String email, String oldPassword, String newPassword ){
        return userMapper.changeUserPassword(email, oldPassword, newPassword);
    }
}
