package top.playereg.pix_vision.service.Impl;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.playereg.pix_vision.config.FilePathConfig;
import top.playereg.pix_vision.mapper.ContentAuditRecordMapper;
import top.playereg.pix_vision.mapper.UserDataChangeLockMapper;
import top.playereg.pix_vision.mapper.UserDataMapper;
import top.playereg.pix_vision.mapper.UserMapper;
import top.playereg.pix_vision.pojo.VO.UserDataChangeLockVO;
import top.playereg.pix_vision.pojo.admin.AdminBatchOperateWorkResult;
import top.playereg.pix_vision.pojo.dto.ContentAuditResult;
import top.playereg.pix_vision.pojo.entity.ContentAuditRecord;
import top.playereg.pix_vision.pojo.entity.user.User;
import top.playereg.pix_vision.pojo.entity.user.UserData;
import top.playereg.pix_vision.pojo.entity.user.UserDataChangeLock;
import top.playereg.pix_vision.pojo.external.NicknameChangeResult;
import top.playereg.pix_vision.service.ContentAuditService;
import top.playereg.pix_vision.service.TokenWhitelistService;
import top.playereg.pix_vision.service.UserService;
import top.playereg.pix_vision.service.VerificationCodeServices;
import top.playereg.pix_vision.util.JWTUtils;
import top.playereg.pix_vision.util.PixVisionLogger;
import top.playereg.pix_vision.util.StrSwitchUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
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
    @Autowired
    private ContentAuditService contentAuditService;
    @Autowired
    private UserDataChangeLockMapper userDataChangeLockMapper;

    @Autowired
    private ContentAuditRecordMapper contentAuditRecordMapper;

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

    /**
     * 从 HTTP 请求中提取 Token 并验证用户身份
     *
     * @param request   HTTP 请求对象
     * @param sceneName 场景名称
     * @return 用户对象，认证失败返回 null
     * @author PlayerEG
     */
    @Override
    public User extractUserFromToken(HttpServletRequest request, String sceneName) {
        // 提取 Token
        String token = JWTUtils.extractTokenWithLog(request, sceneName);
        if (token == null || token.isEmpty()) {
            log.error("{} - Token 不存在", sceneName);
            return null;
        }

        // 从 Token 中获取用户 ID
        Integer userId = JWTUtils.getUserIdFromToken(token);
        if (userId == null || userId <= 0) {
            log.error("{} - 无法从 Token 中获取用户 ID", sceneName);
            return null;
        }

        log.info("{} - 用户 ID: {}", sceneName, userId);

        // 根据用户 ID 查询用户信息
        User user = userMapper.selectAllUserInfoById(userId);
        if (user == null) {
            log.warn("{} - 用户不存在，用户 ID: {}", sceneName, userId);
            return null;
        }

        return user;
    }

    @Override
    public User selectAllUserByUuid(byte[] uuidBytes) {
        return userMapper.selectAllUserInfoByUuid(uuidBytes);
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
     * 按角色分页查询用户信息（支持单个或多个角色）
     *
     * @param page      分页参数
     * @param userRoles 用户角色列表（可选，支持多个角色，OR 关系）
     * @return 分页用户列表
     */
    @Override
    public IPage<User> selectPageUserInfoByRole(IPage<User> page, java.util.List<Integer> userRoles) {
        log.info("按角色分页查询用户信息 - 角色列表: {}", userRoles);

        // 如果未提供角色列表或列表为空，则查询所有用户
        if (userRoles == null || userRoles.isEmpty()) {
            log.info("未提供角色筛选条件，查询所有用户");
            return userMapper.selectAllUsers(page);
        }

        // 验证角色代码是否合法
        java.util.List<Integer> validRoles = java.util.List.of(11, 22, 55, 66, 77);
        for (Integer role : userRoles) {
            if (!validRoles.contains(role)) {
                log.warn("无效的角色代码: {}，允许的角色代码: {}", role, validRoles);
                return null;
            }
        }

        log.info("执行按角色分页查询 - 角色列表: {}", userRoles);
        return userMapper.selectUsersByRoles(page, userRoles);
    }

    /**
     * 管理员分页查询用户列表（支持多条件过滤）
     *
     * @param page     页码（从 1 开始）
     * @param size     每页大小
     * @param userRole 用户角色（可选）
     * @param status   用户状态（可选）
     * @param isDelete 是否已删除（可选）
     * @param nickname 昵称关键字（可选，模糊查询）
     * @param orderBy  排序方式：'oldest' - 最早注册，其他值或 null - 最新注册（默认）
     * @return 分页用户列表
     */
    @Override
    public IPage<User> getAdminUserPage(Integer page, Integer size, Integer userRole,
                                         Integer status, Boolean isDelete, String nickname, String orderBy) {
        log.info("管理员分页查询用户 - 页码: {}, 每页: {}, 角色: {}, 状态: {}, 是否删除: {}, 昵称: {}, 排序: {}",
            page, size, userRole, status, isDelete, nickname, orderBy);

        Page<User> pageObj = new Page<>(page, size);
        IPage<User> result = userMapper.adminSelectUsers(pageObj, userRole, status, isDelete, nickname, orderBy);

        log.info("管理员分页查询用户完成 - 总条数: {}, 当前页条数: {}",
            result.getTotal(), result.getRecords().size());
        return result;
    }

    /**
     * 用户密码修改（通过邮箱）
     *
     * @param email       用户的邮箱
     * @param oldPassword 用户的旧密码
     * @param newPassword 用户的新密码
     * @return 影响行数
     */
    public Integer changeUserLoginPasswordByEmail(String email, String oldPassword, String newPassword) {
        return userMapper.changeUserPassword(email, oldPassword, newPassword);
    }

    /**
     * 忘记密码 - 重置密码（无需登录）
     *
     * @param usernameOrEmail 用户名或邮箱
     * @param newPassword     新密码
     * @param confirmPassword 确认新密码
     * @param vCode           邮箱验证码
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
     *
     * @param userId    用户 ID
     * @param avatarUrl 头像路径
     * @return 是否成功
     */
    @Override
    public Boolean updateUserAvatar(Integer userId, String avatarUrl, Integer adminId) {
        log.info("更新用户头像，用户 ID: {}, 头像路径: {}, 操作者 ID: {}", userId, avatarUrl, adminId);

        if (userId == null || userId <= 0) {
            log.error("用户 ID 无效: {}", userId);
            return false;
        }

        if (avatarUrl == null || avatarUrl.isEmpty()) {
            log.error("头像路径不能为空");
            return false;
        }

        int result = userMapper.updateUserAvatar(userId, avatarUrl, adminId);

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
    public Boolean updateUserNickname(Integer userId, String nickname, Integer adminId) {
        log.info("更新用户昵称，用户 ID: {}, 新昵称: {}, 操作者 ID: {}", userId, nickname, adminId);

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

        int result = userMapper.updateUserNickname(userId, nickname, adminId);

        if (result > 0) {
            log.info("用户昵称更新成功，用户 ID: {}, 新昵称: {}", userId, nickname);
            return true;
        } else {
            log.error("用户昵称更新失败，用户 ID: {}", userId);
            return false;
        }
    }

    /**
     * 更新用户昵称（带 AI 审核）
     *
     * @param userId   用户 ID
     * @param nickname 新昵称
     * @param adminId  执行操作的用户 ID
     * @return 昵称修改结果
     * @author PlayerEG
     */
    @Override
    @org.springframework.transaction.annotation.Transactional
    public NicknameChangeResult updateNicknameWithAudit(Integer userId, String nickname, Integer adminId) {
        log.info("开始带AI审核的昵称修改 - 用户ID: {}, 新昵称: {}", userId, nickname);

        if (userId == null || userId <= 0) {
            log.error("用户 ID 无效: {}", userId);
            return new NicknameChangeResult(false, null, null);
        }

        if (nickname == null || nickname.isEmpty()) {
            log.error("昵称不能为空");
            return new NicknameChangeResult(false, null, null);
        }

        if (nickname.length() < 1 || nickname.length() > 20) {
            log.error("昵称长度必须在 1-20 个字符之间，当前长度: {}", nickname.length());
            return new NicknameChangeResult(false, null, null);
        }

        // 查询用户当前昵称作为 old_data
        User currentUser = userMapper.selectAllUserInfoById(userId);
        if (currentUser == null) {
            log.error("用户不存在 - 用户ID: {}", userId);
            return new NicknameChangeResult(false, null, null);
        }

        String oldNickname = currentUser.getNickname();

        // 新旧昵称相同，无需审核和更新
        if (nickname.equals(oldNickname)) {
            log.info("新旧昵称相同，跳过更新 - 用户ID: {}, 昵称: {}", userId, nickname);
            return new NicknameChangeResult(true, 10, null);
        }

        // 调用 AI 审核服务
        Integer approvalStatus = 20; // 默认待审核
        String auditReason = null;
        ContentAuditResult auditResult = contentAuditService.auditContent(nickname);

        if (auditResult != null) {
            auditReason = auditResult.getReason();
            switch (auditResult.getStatus()) {
                case "normal":
                    approvalStatus = 10;
                    break;
                case "neutral":
                    approvalStatus = 20;
                    break;
                case "violation":
                    approvalStatus = 30;
                    break;
                default:
                    log.warn("AI 审核返回未知状态: {}, 降级为待审核", auditResult.getStatus());
                    break;
            }
            log.info("AI 审核结果 - 状态: {}, 原因: {}, 命中敏感词: {}, 最终审核状态: {}",
                auditResult.getStatus(), auditResult.getReason(), auditResult.getInsult_words(), approvalStatus);
        } else {
            log.warn("AI 审核服务不可用，降级为待审核");
        }

        // 审核通过时直接更新昵称
        if (approvalStatus == 10) {
            int result = userMapper.updateUserNickname(userId, nickname, adminId);
            if (result <= 0) {
                log.error("昵称更新失败 - 用户ID: {}", userId);
                return new NicknameChangeResult(false, null, null);
            }
            log.info("昵称审核通过，已直接更新 - 用户ID: {}", userId);
        }

        // 待审核状态时，清除旧的同类型待审核记录
        if (approvalStatus == 20) {
            supersedePendingLocks(userId, 100);
        }

        // 插入锁定记录
        UserDataChangeLock lock = new UserDataChangeLock();
        lock.setUser_id(userId);
        lock.setType(100); // 昵称类型
        lock.setNickname(nickname);
        lock.setOld_data(oldNickname);
        lock.setApproval_status(approvalStatus);
        lock.setIs_delete(false);

        int insertResult = userDataChangeLockMapper.insertLock(lock);
        if (insertResult <= 0) {
            log.error("插入锁定记录失败 - 用户ID: {}", userId);
            return new NicknameChangeResult(false, null, null);
        }

        log.info("昵称修改锁定记录已插入 - 用户ID: {}, 审核状态: {}, lockId: {}",
            userId, approvalStatus, lock.getLock_id());

        // 将 AI 审核结果写入审核记录表
        if (auditResult != null) {
            ContentAuditRecord auditRecord = new ContentAuditRecord();
            auditRecord.setContent_type(400);
            auditRecord.setContent_id(lock.getLock_id());
            auditRecord.setApproval_status(approvalStatus);
            auditRecord.setAudit_reason(auditResult.getReason());
            auditRecord.setInsult_words(auditResult.getInsult_words() != null
                ? JSON.toJSONString(auditResult.getInsult_words()) : null);
            auditRecord.setOriginal_content(nickname);
            auditRecord.setCreate_time(new java.sql.Timestamp(System.currentTimeMillis()));
            contentAuditRecordMapper.insertRecord(auditRecord);
            log.info("昵称审核记录已保存 - lockId: {}, 审核状态: {}", lock.getLock_id(), approvalStatus);
        }

        return new NicknameChangeResult(true, approvalStatus, auditReason);
    }

    /**
     * 更新用户头像（带人工审核锁定）
     * <p>
     * 与 {@link #updateUserAvatar} 不同，此方法不直接更新用户头像，
     * 而是将变更记录写入 tb_user_data_change_lock 表，审核状态始终设为待审核（20），
     * 等待管理员审核通过后才更新用户头像。
     * </p>
     *
     * <h3>使用场景</h3>
     * <ol>
     *   <li>用户自行上传头像时调用此方法（经人工审核）</li>
     *   <li>管理员直接修改头像时调用 {@link #updateUserAvatar}（跳过审核）</li>
     * </ol>
     *
     * @param userId       用户 ID
     * @param newAvatarUrl 新头像路径
     * @param adminId      执行操作的用户 ID
     * @return 是否成功
     * @author PlayerEG
     * @see #updateUserAvatar(Integer, String, Integer)
     */
    @Override
    @Transactional
    public Boolean updateAvatarWithLock(Integer userId, String newAvatarUrl, Integer adminId) {
        log.info("开始头像修改（人工审核）- 用户ID: {}, 新头像: {}", userId, newAvatarUrl);

        if (userId == null || userId <= 0) {
            log.error("用户 ID 无效: {}", userId);
            return false;
        }

        if (newAvatarUrl == null || newAvatarUrl.isEmpty()) {
            log.error("头像路径不能为空");
            return false;
        }

        // 查询用户当前头像作为 old_data
        User currentUser = userMapper.selectAllUserInfoById(userId);
        if (currentUser == null) {
            log.error("用户不存在 - 用户ID: {}", userId);
            return false;
        }

        String oldAvatar = currentUser.getAvatar_url();

        // 新旧头像相同，无需审核
        if (newAvatarUrl.equals(oldAvatar)) {
            log.info("新旧头像相同，跳过更新 - 用户ID: {}, 头像: {}", userId, newAvatarUrl);
            return true;
        }

        // 清除旧的同类型待审核记录
        supersedePendingLocks(userId, 300);

        // 插入锁定记录，审核状态固定为待审核（20）
        UserDataChangeLock lock = new UserDataChangeLock();
        lock.setUser_id(userId);
        lock.setType(300); // 头像类型
        lock.setAvatar_url(newAvatarUrl);
        lock.setOld_data(oldAvatar);
        lock.setApproval_status(20); // 固定待审核，不走 AI 审核
        lock.setIs_delete(false);

        int insertResult = userDataChangeLockMapper.insertLock(lock);
        if (insertResult <= 0) {
            log.error("插入头像锁定记录失败 - 用户ID: {}", userId);
            return false;
        }

        log.info("头像修改锁定记录已插入 - 用户ID: {}, lockId: {}, 待人工审核",
            userId, lock.getLock_id());

        return true;
    }

    /**
     * 申请权限升级（需审核）
     * <p>
     * 用户申请升级到更高角色。该方法不直接更新用户角色，
     * 而是将变更记录写入 tb_user_data_change_lock 表（type=200），
     * 审核状态设为待审核（20），等待管理员审核通过后才更新。
     * </p>
     *
     * <h3>使用场景</h3>
     * <ol>
     *   <li>用户主动申请升级权限（如普通用户申请成为创作者）</li>
     *   <li>需要邮箱验证码验证身份后方可申请</li>
     * </ol>
     *
     * <h3>注意事项</h3>
     * <ul>
     *   <li>只能申请比当前角色更高的角色</li>
     *   <li>不能有同类型待审核的记录</li>
     *   <li>申请后需等待管理员审核</li>
     * </ul>
     *
     * @param userId     用户 ID
     * @param targetRole 目标角色代码
     * @return 申请结果消息
     * @author PlayerEG
     * @see #applyRoleDowngrade(Integer, Integer)
     */
    @Override
    @Transactional
    public String applyRoleUpgrade(Integer userId, Integer targetRole) {
        log.info("开始权限升级申请 - 用户ID: {}, 目标角色: {}", userId, targetRole);

        // 查询用户当前角色
        User currentUser = userMapper.selectAllUserInfoById(userId);
        if (currentUser == null) {
            log.error("用户不存在 - 用户ID: {}", userId);
            return "用户不存在";
        }

        Integer currentRole = currentUser.getUser_role();

        // 校验目标角色是否合法
        if (!isValidRole(targetRole)) {
            log.warn("非法的目标角色: {}", targetRole);
            return "非法的角色代码，允许的角色：11-普通用户, 22-创作者, 55-审核员, 66-工单管理员, 77-系统管理员";
        }

        // 校验是否为升级（目标角色必须大于当前角色）
        if (targetRole <= currentRole) {
            log.warn("权限升级申请被拒绝 - 用户ID: {}, 当前角色: {}, 目标角色: {} (非升级操作)", userId, currentRole, targetRole);
            return "只能申请比当前角色更高的权限";
        }

        // 清除旧的同类型待审核记录
        supersedePendingLocks(userId, 200);

        // 插入锁定记录，审核状态固定为待审核（20）
        UserDataChangeLock lock = new UserDataChangeLock();
        lock.setUser_id(userId);
        lock.setType(200); // 权限类型
        lock.setUser_role(targetRole);
        lock.setOld_data(String.valueOf(currentRole));
        lock.setApproval_status(20); // 固定待审核
        lock.setIs_delete(false);

        int insertResult = userDataChangeLockMapper.insertLock(lock);
        if (insertResult <= 0) {
            log.error("插入权限升级锁定记录失败 - 用户ID: {}", userId);
            return "申请失败，请稍后重试";
        }

        log.info("权限升级申请已提交 - 用户ID: {}, 当前角色: {}, 目标角色: {}, lockId: {}, 待人工审核",
            userId, currentRole, targetRole, lock.getLock_id());

        return "权限升级申请已提交，等待管理员审核";
    }

    /**
     * 自主降权（无需审核）
     * <p>
     * 用户主动降低自己的角色权限，无需管理员审核，直接更新数据库。
     * 降权后自动清除角色缓存以确保权限验证获取最新信息。
     * </p>
     *
     * <h3>使用场景</h3>
     * <ol>
     *   <li>用户主动放弃高权限角色</li>
     *   <li>需要邮箱验证码验证身份后方可降权</li>
     * </ol>
     *
     * <h3>注意事项</h3>
     * <ul>
     *   <li>只能降到比当前角色更低的角色</li>
     *   <li>不能有同类型待审核的记录</li>
     *   <li>降权操作立即生效，不可撤销</li>
     * </ul>
     *
     * @param userId     用户 ID
     * @param targetRole 目标角色代码
     * @return 降权结果消息
     * @author PlayerEG
     * @see #applyRoleUpgrade(Integer, Integer)
     */
    @Override
    @Transactional
    public String applyRoleDowngrade(Integer userId, Integer targetRole) {
        log.info("开始自主降权 - 用户ID: {}, 目标角色: {}", userId, targetRole);

        // 查询用户当前角色
        User currentUser = userMapper.selectAllUserInfoById(userId);
        if (currentUser == null) {
            log.error("用户不存在 - 用户ID: {}", userId);
            return "用户不存在";
        }

        Integer currentRole = currentUser.getUser_role();

        // 校验目标角色是否合法
        if (!isValidRole(targetRole)) {
            log.warn("非法的目标角色: {}", targetRole);
            return "非法的角色代码，允许的角色：11-普通用户, 22-创作者, 55-审核员, 66-工单管理员, 77-系统管理员";
        }

        // 校验是否为降权（目标角色必须小于当前角色）
        if (targetRole >= currentRole) {
            log.warn("降权申请被拒绝 - 用户ID: {}, 当前角色: {}, 目标角色: {} (非降权操作)", userId, currentRole, targetRole);
            return "只能降到比当前角色更低的权限";
        }

        // 直接更新用户角色
        int updateResult = userMapper.updateUserRole(userId, targetRole, userId);
        if (updateResult <= 0) {
            log.error("降权更新角色失败 - 用户ID: {}, 目标角色: {}", userId, targetRole);
            return "降权失败，请稍后重试";
        }

        // 清除角色缓存
        clearUserRoleCache(userId);

        log.info("降权成功 - 用户ID: {}, 原角色: {}, 新角色: {}", userId, currentRole, targetRole);

        return "降权成功";
    }

    /**
     * 校验角色代码是否合法
     *
     * @param role 角色代码
     * @return true-合法，false-非法
     */
    private boolean isValidRole(Integer role) {
        if (role == null) {
            return false;
        }
        return role == 11 || role == 22 || role == 55 || role == 66 || role == 77;
    }

    /**
     * 清除同一用户同类型的旧待审核记录
     * <p>
     * 仅在新记录的 approvalStatus 为 20 时调用。
     * 将旧的待审核记录软删除（is_delete = 1），
     * 头像类型同步将 .pend 文件重命名为 .del 标记废弃。
     * </p>
     *
     * @param userId 用户 ID
     * @param type   变更类型（100/200/300）
     */
    private void supersedePendingLocks(Integer userId, Integer type) {
        List<UserDataChangeLock> oldLocks = userDataChangeLockMapper
            .selectPendingByUserAndType(userId, type);

        if (oldLocks.isEmpty()) {
            return;
        }

        for (UserDataChangeLock oldLock : oldLocks) {
            // 头像类型：重命名旧的 .pend 文件为 .del
            if (type == 300 && oldLock.getAvatar_url() != null) {
                Path oldPath = Paths.get(FilePathConfig.AvatarPath, oldLock.getAvatar_url() + ".pend");
                Path newPath = Paths.get(FilePathConfig.AvatarPath, oldLock.getAvatar_url() + ".del");
                if (Files.exists(oldPath)) {
                    try {
                        Files.move(oldPath, newPath);
                        log.info("已将旧待审核头像重命名为 .del - {}", newPath);
                    } catch (Exception e) {
                        log.warn("旧待审核头像重命名失败 - {}", oldPath);
                    }
                }
            }
        }

        // 批量软删除
        int count = userDataChangeLockMapper.updatePendingToDeleted(userId, type);
        log.info("已软删除旧待审核记录 - userId: {}, type: {}, 条数: {}", userId, type, count);
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
     * 检查用户是否已存在指定类型的拓展数据
     *
     * @param userId   用户 ID
     * @param dataName 数据类型名称
     * @return true-已存在，false-不存在
     */
    @Override
    public boolean isUserDataExists(Integer userId, String dataName) {
        if (userId == null || userId <= 0 || dataName == null || dataName.isEmpty()) {
            return false;
        }
        return userDataMapper.countByUserIdAndDataName(userId, dataName) > 0;
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

        // 执行逻辑删除，update_user 设置为当前用户 ID（即自己注销）
        int result = userMapper.deleteUserAccount(userId, userId);

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
     * 根据用户名精确查询用户信息
     *
     * @param username 用户名
     * @return 用户对象，如果不存在返回 null
     */
    @Override
    public User selectUserByUsername(String username) {
        if (username == null || username.isEmpty()) {
            log.warn("查询用户信息失败：用户名为空");
            return null;
        }

        log.info("通过用户名精确查询用户：{}", username);
        User user = userMapper.selectAllUserInfoByUsername(username);

        if (user != null) {
            log.info("查询到用户 - 用户名：{}, 用户 ID: {}", user.getUsername(), user.getUser_id());
        } else {
            log.warn("用户不存在：{}", username);
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
    @Transactional(rollbackFor = Exception.class)
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

        // 执行逻辑删除，update_user 设置为管理员 ID
        int result = userMapper.deleteUserAccount(targetUserId, adminId);

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

        // 如果昵称为空，自动生成随机昵称
        if (nickname == null || nickname.isEmpty()) {
            nickname = StrSwitchUtils.generateRandomUserDefaultNickName("user");
            log.info("昵称为空，自动生成随机昵称: {}", nickname);
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

    /**
     * 管理员批量重置用户密码（仅系统管理员可调用）
     *
     * @param userIds 目标用户 ID 列表
     * @return 重置结果列表，每个元素包含 user_id, email, username, plainPassword
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public java.util.List<java.util.Map<String, Object>> batchResetUserPasswords(java.util.List<Integer> userIds) {
        log.info("开始批量重置用户密码 - 用户数量: {}", userIds != null ? userIds.size() : 0);

        if (userIds == null || userIds.isEmpty()) {
            log.warn("用户 ID 列表为空");
            return new java.util.ArrayList<>();
        }

        java.util.List<java.util.Map<String, Object>> resultList = new java.util.ArrayList<>();

        for (Integer userId : userIds) {
            try {
                // 1. 查询用户信息
                User user = userMapper.selectAllUserInfoById(userId);
                if (user == null) {
                    log.warn("用户不存在，跳过 - 用户 ID: {}", userId);
                    continue;
                }

                // 2. 生成随机密码
                String plainPassword = StrSwitchUtils.generateRandomPassword();
                log.debug("为用户生成临时密码 - 用户名: {}", user.getUsername());

                // 3. 加密密码
                String hashedPassword = StrSwitchUtils.PasswdToHash256(plainPassword);

                // 4. 更新数据库密码 (changeUserPassword 接受 email, oldPassword, newPassword)
                // 注意：这里我们直接更新，不验证旧密码，因为这是管理员操作
                int res = userMapper.changeUserPassword(user.getEmail(), null, hashedPassword);
                if (res != 1) {
                    log.error("更新密码失败 - 用户 ID: {}", userId);
                    continue;
                }

                // 5. 强制下线所有设备
                tokenWhitelistService.removeAllUserTokens(user.getUser_id(), user.getUsername());

                // 6. 记录结果
                java.util.Map<String, Object> resultItem = new java.util.HashMap<>();
                resultItem.put("user_id", user.getUser_id());
                resultItem.put("username", user.getUsername());
                resultItem.put("email", user.getEmail());
                resultItem.put("plainPassword", plainPassword);
                resultList.add(resultItem);

                log.info("用户密码重置成功 - 用户 ID: {}, 用户名: {}", userId, user.getUsername());

            } catch (Exception e) {
                log.error("处理用户密码重置时发生异常 - 用户 ID: {}, 错误: {}", userId, e.getMessage(), e);
            }
        }

        log.info("批量重置密码完成 - 成功数量: {}", resultList.size());
        return resultList;
    }

    /**
     * 分页查询待审核的用户数据变更记录
     *
     * @param current 当前页码
     * @param size    每页大小
     * @param type    变更类型筛选（可选，100/200/300）
     * @return 分页结果
     */
    @Override
    public IPage<UserDataChangeLockVO> getPendingLockPage(Long current, Long size, Integer type) {
        Page<UserDataChangeLockVO> page = new Page<>(current, size);
        IPage<UserDataChangeLockVO> result = userDataChangeLockMapper.selectPendingPage(page, type);

        // 批量查询昵称审核记录（仅 type=100 时有意义，但也统一查一下避免遗漏）
        if (result != null && !result.getRecords().isEmpty()) {
            List<Integer> lockIds = result.getRecords().stream()
                .map(UserDataChangeLockVO::getLock_id)
                .collect(java.util.stream.Collectors.toList());
            List<ContentAuditRecord> auditRecords = contentAuditRecordMapper
                .selectLatestByContentIds(400, lockIds);
            if (auditRecords != null && !auditRecords.isEmpty()) {
                java.util.Map<Integer, ContentAuditRecord> auditMap = auditRecords.stream()
                    .collect(java.util.stream.Collectors.toMap(
                        ContentAuditRecord::getContent_id, r -> r, (a, b) -> a));
                for (UserDataChangeLockVO vo : result.getRecords()) {
                    ContentAuditRecord audit = auditMap.get(vo.getLock_id());
                    if (audit != null) {
                        vo.setAudit_reason(audit.getAudit_reason());
                        vo.setInsult_words(audit.getInsult_words());
                    }
                }
            }
        }

        return result;
    }

    /**
     * 批量审核用户数据变更
     * <p>
     * 遍历 lockIds，验证当前状态必须为 20（待审核），
     * 通过时连带更新用户数据，拒绝时处理头像文件。
     * </p>
     *
     * @param lockIds  lock_id 列表
     * @param approved true-通过 / false-拒绝
     * @param adminId  执行操作的管理员 ID
     * @return 批量操作结果
     */
    @Override
    @Transactional
    public AdminBatchOperateWorkResult batchReviewUserDataChange(
        List<Integer> lockIds, Boolean approved, Integer adminId
    ) {
        List<Integer> failedIds = new ArrayList<>();
        int successCount = 0;

        // 布尔值转数据库状态：true(通过)→10，false(拒绝)→30
        Integer dbStatus = approved ? 10 : 30;

        for (Integer lockId : lockIds) {
            UserDataChangeLock lock = userDataChangeLockMapper.selectById(lockId);
            if (lock == null) {
                failedIds.add(lockId);
                continue;
            }

            // 只能审核待审核状态的记录
            if (lock.getApproval_status() != 20) {
                failedIds.add(lockId);
                continue;
            }

            Integer userId = lock.getUser_id();

            // 审核通过：连带更新用户数据
            if (dbStatus == 10) {
                switch (lock.getType()) {
                    case 100:
                        userMapper.updateUserNickname(userId, lock.getNickname(), adminId);
                        break;
                    case 200:
                        userMapper.updateUserRole(userId, lock.getUser_role(), adminId);
                        clearUserRoleCache(userId);
                        break;
                    case 300:
                        userMapper.updateUserAvatar(userId, lock.getAvatar_url(), adminId);
                        renameAvatarFile(lock.getAvatar_url(), ".pend", "");
                        break;
                    default:
                        log.warn("未知的变更类型 - lockId: {}, type: {}", lockId, lock.getType());
                        failedIds.add(lockId);
                        continue;
                }
            }

            // 审核拒绝：头像文件处理
            if (dbStatus == 30 && lock.getType() == 300) {
                renameAvatarFile(lock.getAvatar_url(), ".pend", ".fail");
            }

            // 更新审核状态
            lock.setApproval_status(dbStatus);
            userDataChangeLockMapper.updateById(lock);
            successCount++;
        }

        log.info("批量审核完成 - 总数: {}, 成功: {}, 失败: {}", lockIds.size(), successCount, failedIds.size());
        return new AdminBatchOperateWorkResult(lockIds.size(), successCount, failedIds);
    }

    /**
     * 重命名头像文件后缀
     * <p>将头像文件名中的指定后缀替换为新的后缀</p>
     *
     * @param avatarUrl  头像路径（文件名部分）
     * @param fromSuffix 原始后缀（如 .pend）
     * @param toSuffix   目标后缀（如 .fail 或空字符串）
     */
    private void renameAvatarFile(String avatarUrl, String fromSuffix, String toSuffix) {
        if (avatarUrl == null || avatarUrl.isEmpty()) {
            return;
        }
        // 磁盘文件带 .pend 后缀，存储的 avatar_url 不带后缀
        Path oldPath = Paths.get(FilePathConfig.AvatarPath, avatarUrl + fromSuffix);
        Path newPath = Paths.get(FilePathConfig.AvatarPath, avatarUrl + toSuffix);
        if (Files.exists(oldPath)) {
            try {
                Files.move(oldPath, newPath);
                log.info("头像文件重命名成功 - {} -> {}", oldPath, newPath);
            } catch (Exception e) {
                log.warn("头像文件重命名失败 - {}", oldPath);
            }
        } else {
            log.warn("头像文件不存在 - {}", oldPath);
        }
    }

    /**
     * 批量初始化用户头像和昵称
     * <p>
     * 为指定用户列表随机分配默认头像和随机昵称，模拟注册时的初始化逻辑。
     * 管理员不能初始化自己的头像和昵称。
     * </p>
     *
     * @param userIds 目标用户 ID 列表
     * @param adminId 执行操作的管理员 ID
     * @return 批量操作结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminBatchOperateWorkResult batchInitAvatarAndNickname(List<Integer> userIds, Integer adminId) {
        log.info("批量初始化用户头像和昵称 - 用户数量: {}, 管理员ID: {}", userIds.size(), adminId);

        List<Integer> failedIds = new ArrayList<>();
        int successCount = 0;

        for (Integer userId : userIds) {
            // 管理员不能修改自己的信息
            if (userId.equals(adminId)) {
                log.warn("管理员不能初始化自己的头像和昵称，跳过 - 用户 ID: {}", adminId);
                continue;
            }

            // 随机头像（default/1.png ~ default/21.png），模拟注册逻辑
            int randomAvatarNum = (int) (Math.random() * 21) + 1;
            String randomAvatar = "default/" + randomAvatarNum + ".png";

            // 随机昵称（user_xxxxxxxxxx），模拟注册时的默认昵称生成
            String randomNickname = StrSwitchUtils.generateRandomUserDefaultNickName("user");

            boolean avatarSuccess = updateUserAvatar(userId, randomAvatar, adminId);
            boolean nicknameSuccess = updateUserNickname(userId, randomNickname, adminId);

            if (avatarSuccess && nicknameSuccess) {
                successCount++;
                log.info("用户头像和昵称初始化成功 - 用户ID: {}, 头像: {}, 昵称: {}", userId, randomAvatar, randomNickname);
            } else {
                failedIds.add(userId);
                log.warn("用户头像和昵称初始化失败 - 用户ID: {}", userId);
            }
        }

        log.info("批量初始化完成 - 总数: {}, 成功: {}, 失败: {}", userIds.size(), successCount, failedIds.size());
        return new AdminBatchOperateWorkResult(userIds.size(), successCount, failedIds);
    }
}


