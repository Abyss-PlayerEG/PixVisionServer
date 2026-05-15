package top.playereg.pix_vision.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import top.playereg.pix_vision.pojo.userPojo.User;

/**
 * 用户服务接口
 * <p>
 * 提供用户相关的业务逻辑处理，包括用户注册、登录、信息查询、资料修改等功能。
 * 所有方法都应该在实现类中进行事务控制和异常处理。
 * </p>
 *
 * <h3>使用场景</h3>
 * <ol>
 *   <li>用户认证相关（注册、登录、密码重置）</li>
 *   <li>用户信息管理（查询、修改昵称、修改头像、修改邮箱）</li>
 *   <li>用户权限管理（角色分配、状态管理、账户封禁）</li>
 *   <li>用户数据管理（拓展数据增删查）</li>
 * </ol>
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * // 示例1：用户注册
 * User user = userService.registerUser(username, password, nickname, email);
 * if (user != null) {
 *     log.info("用户注册成功: {}", username);
 * }
 *
 * // 示例2：分页查询用户
 * Page<User> page = new Page<>(1, 10);
 * IPage<User> result = userService.selectPageUserInfo(page, keyword, null);
 * List<User> users = result.getRecords();
 *
 * // 示例3：修改用户昵称
 * Boolean success = userService.updateUserNickname(userId, newNickname);
 * if (success) {
 *     log.info("昵称修改成功");
 * }
 *
 * // 示例4：管理员批量重置密码
 * List<Map<String, Object>> results = userService.batchResetUserPasswords(userIds);
 * for (Map<String, Object> result : results) {
 *     sendPasswordEmail(result.get("email"), result.get("plainPassword"));
 * }
 * }</pre>
 *
 * <h3>注意事项</h3>
 * <ul>
 *   <li>所有密码操作必须在 Service 层进行加密处理</li>
 *   <li>敏感操作需要记录操作日志</li>
 *   <li>修改密码后必须清除用户的所有 Token</li>
 *   <li>分页查询时，必须先创建 Page 对象再传入方法</li>
 *   <li>返回 null 表示操作失败或数据不存在</li>
 * </ul>
 *
 * <h3>最佳实践</h3>
 * <ul>
 *   <li>使用构造器注入依赖，避免使用 @Autowired 字段注入</li>
 *   <li>在 Service 层进行参数校验和业务逻辑处理</li>
 *   <li>使用 PixVisionLogger 记录关键操作日志</li>
 *   <li>对于批量操作，注意事务控制和数据一致性</li>
 *   <li>缓存更新要及时，避免脏数据</li>
 * </ul>
 *
 * @author PlayerEG
 * @see top.playereg.pix_vision.service.Impl.UserServiceImpl 用户服务实现类
 * @see top.playereg.pix_vision.pojo.userPojo.User 用户实体类
 * @since DEV-1.0.0
 */
public interface UserService {

    /**
     * 用户注册
     * <p>
     * 创建新用户账户，包括用户名唯一性检查、邮箱唯一性检查、密码加密等。
     * 注册成功后会自动生成用户 UUID 和随机头像。
     * </p>
     *
     * <h3>使用示例</h3>
     * <pre>{@code
     * // 基本用法
     * User user = userService.registerUser("test_user", "hashed_password", "测试用户", "test@example.com");
     * if (user != null) {
     *     log.info("注册成功，用户 ID: {}", user.getUser_id());
     * } else {
     *     log.warn("注册失败，用户名或邮箱已存在");
     * }
     * }</pre>
     *
     * <h3>注意事项</h3>
     * <ul>
     *   <li>密码必须在 Controller 层已经过 SHA-256 加密</li>
     *   <li>用户名和邮箱必须唯一，重复会返回 null</li>
     *   <li>昵称为空时会自动生成随机昵称</li>
     *   <li>默认角色为 11（普通用户），状态为 10（正常）</li>
     * </ul>
     *
     * @param username 用户名，6-16 位字母/数字/下划线
     * @param password 已加密的密码（SHA-256）
     * @param nickname 用户昵称，可为空，为空时自动生成
     * @param email    邮箱地址，必须符合标准邮箱格式
     * @return 注册成功的用户对象，失败返回 null
     * @author PlayerEG
     */
    User registerUser(String username, String password, String nickname, String email);

    /**
     * 根据用户名查询用户信息
     * <p>
     * 通过用户名精确查询用户完整信息，包括密码、角色、状态等所有字段。
     * 仅返回未删除的用户（is_delete = false）。
     * </p>
     *
     * <h3>使用示例</h3>
     * <pre>{@code
     * User user = userService.selectAllUserByUsername("test_user");
     * if (user != null) {
     *     log.info("找到用户: {}, 角色: {}", user.getUsername(), user.getUser_role());
     * } else {
     *     log.warn("用户不存在");
     * }
     * }</pre>
     *
     * <h3>注意事项</h3>
     * <ul>
     *   <li>仅查询未删除的用户</li>
     *   <li>返回完整的用户信息，包含敏感字段（如密码）</li>
     *   <li>用户名区分大小写</li>
     * </ul>
     *
     * @param username 用户名
     * @return 用户对象，不存在则返回 null
     * @author PlayerEG
     */
    User selectAllUserByUsername(String username);

    /**
     * 根据用户 ID 查询用户信息
     * <p>
     * 通过用户 ID 精确查询用户完整信息。
     * 仅返回未删除的用户（is_delete = false）。
     * </p>
     *
     * <h3>使用示例</h3>
     * <pre>{@code
     * User user = userService.selectAllUserById(1);
     * if (user != null) {
     *     log.info("用户 ID: {}, 用户名: {}", user.getUser_id(), user.getUsername());
     * }
     * }</pre>
     *
     * <h3>注意事项</h3>
     * <ul>
     *   <li>仅查询未删除的用户</li>
     *   <li>返回完整的用户信息，包含敏感字段</li>
     *   <li>用户 ID 必须大于 0</li>
     * </ul>
     *
     * @param userId 用户 ID
     * @return 用户对象，不存在则返回 null
     * @author PlayerEG
     */
    User selectAllUserById(Integer userId);

    /**
     * 分页查询用户信息（支持关键词统一查询）
     * <p>
     * 支持通过关键词同时搜索用户名、邮箱、昵称（模糊匹配），或通过 UUID 精确查询。
     * 返回分页结果，包含总记录数和当前页数据列表。
     * </p>
     *
     * <h3>使用示例</h3>
     * <pre>{@code
     * // 示例1：关键词模糊查询
     * Page<User> page = new Page<>(1, 10);
     * IPage<User> result = userService.selectPageUserInfo(page, "test", null);
     * log.info("总数: {}, 当前页: {}", result.getTotal(), result.getRecords().size());
     *
     * // 示例2：UUID 精确查询
     * byte[] uuidBytes = StrSwitchUtils.uuid2Bytes("550e8400-e29b-41d4-a716-446655440000");
     * IPage<User> result2 = userService.selectPageUserInfo(page, null, uuidBytes);
     * }</pre>
     *
     * <h3>注意事项</h3>
     * <ul>
     *   <li>keyword 和 uuidBytes 不能同时使用，优先使用 uuidBytes</li>
     *   <li>keyword 会对用户名、邮箱、昵称进行 OR 模糊查询</li>
     *   <li>uuidBytes 必须是 16 字节的二进制 UUID</li>
     *   <li>必须先创建 Page 对象再传入方法</li>
     *   <li>仅返回未删除的用户</li>
     * </ul>
     *
     * @param page      分页对象，包含当前页码和每页大小
     * @param keyword   关键词（可选），模糊查询用户名/邮箱/昵称
     * @param uuidBytes UUID 字节数组（可选），精确查询
     * @return 分页用户列表，无数据时返回空的 IPage 对象
     * @author PlayerEG
     */
    IPage<User> selectPageUserInfo(IPage<User> page, String keyword, byte[] uuidBytes);

    /**
     * 更新用户昵称
     * <p>
     * 修改指定用户的昵称，会验证昵称长度（1-20 个字符）。
     * 更新成功后会自动更新 update_time 和 update_user 字段。
     * </p>
     *
     * <h3>使用示例</h3>
     * <pre>{@code
     * Boolean success = userService.updateUserNickname(1, "新昵称");
     * if (success) {
     *     log.info("昵称修改成功");
     * } else {
     *     log.warn("昵称修改失败");
     * }
     * }</pre>
     *
     * <h3>注意事项</h3>
     * <ul>
     *   <li>昵称长度必须在 1-20 个字符之间</li>
     *   <li>用户 ID 必须有效且用户存在</li>
     *   <li>昵称可以包含中文、英文、数字和特殊字符</li>
     * </ul>
     *
     * @param userId   用户 ID
     * @param nickname 新昵称
     * @return 是否成功
     * @author PlayerEG
     */
    Boolean updateUserNickname(Integer userId, String nickname);

    /**
     * 管理员批量重置用户密码
     * <p>
     * 为指定用户列表生成随机密码，更新数据库并强制下线所有设备。
     * 返回包含用户 ID、邮箱和明文密码的列表，用于后续邮件发送。
     * 此操作会使所有用户的 Token 失效。
     * </p>
     *
     * <h3>使用示例</h3>
     * <pre>{@code
     * List<Integer> userIds = Arrays.asList(1, 2, 3);
     * List<Map<String, Object>> results = userService.batchResetUserPasswords(userIds);
     *
     * for (Map<String, Object> result : results) {
     *     Integer userId = (Integer) result.get("user_id");
     *     String email = (String) result.get("email");
     *     String plainPassword = (String) result.get("plainPassword");
     *
     *     // 发送邮件通知用户新密码
     *     emailService.sendResetPasswordEmail(email, plainPassword);
     * }
     * }</pre>
     *
     * <h3>注意事项</h3>
     * <ul>
     *   <li>此操作仅限系统管理员调用</li>
     *   <li>会为每个用户生成 12 位随机密码（包含大小写字母和数字）</li>
     *   <li>会清除所有用户的所有 Token，强制下线</li>
     *   <li>返回的明文密码需要通过邮件等方式安全地发送给用户</li>
     *   <li>部分用户不存在时会跳过，不影响其他用户</li>
     * </ul>
     *
     * <h3>最佳实践</h3>
     * <ul>
     *   <li>批量操作建议在事务中执行</li>
     *   <li>重置密码后应立即发送邮件通知用户</li>
     *   <li>记录操作日志，便于审计</li>
     *   <li>建议限制单次批量重置的用户数量（如不超过 100 个）</li>
     * </ul>
     *
     * @param userIds 目标用户 ID 列表
     * @return 重置结果列表，每个元素包含 user_id, email, username, plainPassword
     * @author PlayerEG
     */
    java.util.List<java.util.Map<String, Object>> batchResetUserPasswords(java.util.List<Integer> userIds);
}
