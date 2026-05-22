package top.playereg.pix_vision.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;
import top.playereg.pix_vision.pojo.userPojo.User;

/**
 * Mapper 接口类模板
 *
 * @author PlayerEG
 */
@Mapper
@Repository // 持久层
public interface UserMapper extends BaseMapper<User> {

    /**
     * 添加用户
     *
     * @param user 用户实体（需包含 username, password, nickname, email）
     * @return 影响行数
     */
    int insertUser(User user);

    /**
     * 根据用户名查询用户 - 全字段查询
     *
     * @param username 用户名
     * @return 用户实体
     */
    User selectAllUserInfoByUsername(String username);


    /**
     * 根据邮箱查询用户 - 全字段查询
     *
     * @param email 邮箱
     * @return 用户实体
     */
    User selectAllUserInfoByEmail(String email);

    /**
     * 根据用户 ID 查询用户 - 全字段查询
     *
     * @param userId 用户 ID
     * @return 用户实体
     */
    User selectAllUserInfoById(Integer userId);

    /**
     * 根据 UUID 查询用户 - 全字段查询
     *
     * @param uuidBytes UUID 字节数组（16字节）
     * @return 用户实体
     */
    User selectAllUserInfoByUuid(@Param("uuidBytes") byte[] uuidBytes);

    /**
     * 根据用户 ID 查询用户角色和用户名（用于权限验证）
     * <p>
     * 仅查询 user_id, username, user_role 三个字段，提升性能
     * </p>
     *
     * @param userId 用户 ID
     * @return 用户实体（仅包含 id, username, user_role）
     */
    User selectUserRoleById(@Param("userId") Integer userId);

    /**
     * 分页查询用户信息 - 支持用户名、UUID、邮箱查询
     *
     * @param page 分页参数
     * @param user 查询条件对象（可选属性：username, user_uuid, email）
     * @return 分页用户列表
     */
    IPage<User> selectPageUserInfo(
        IPage<?> page,
        User user
    );

    /**
     * 分页查询所有用户（无筛选条件）
     *
     * @param page 分页参数
     * @return 分页用户列表
     */
    IPage<User> selectAllUsers(IPage<?> page);

    /**
     * 按角色列表分页查询用户（支持多个角色，OR 关系）
     *
     * @param page      分页参数
     * @param userRoles 用户角色列表
     * @return 分页用户列表
     */
    IPage<User> selectUsersByRoles(IPage<?> page, @Param("userRoles") java.util.List<Integer> userRoles);

    /**
     * 管理员分页查询用户列表（支持多条件过滤）
     *
     * @param page     分页对象
     * @param userRole 用户角色（可选）
     * @param status   用户状态（可选，10-正常, 20-冻结, 30-封禁）
     * @param isDelete 是否已删除（可选）
     * @param nickname 昵称关键字（可选，模糊查询）
     * @param orderBy  排序方式：'oldest' - 最早注册，其他值或 null - 最新注册（默认）
     * @return 分页用户列表
     */
    IPage<User> adminSelectUsers(Page<User> page,
                                  @Param("userRole") Integer userRole,
                                  @Param("status") Integer status,
                                  @Param("isDelete") Boolean isDelete,
                                  @Param("nickname") String nickname,
                                  @Param("orderBy") String orderBy);

    /**
     * 用户密码修改 / 重置密码（通用）
     *
     * @param email       用户的邮箱
     * @param oldPassword 用户的旧密码（可选，为空时不验证旧密码）
     * @param newPassword 用户的新密码
     * @return 影响行数
     */
    Integer changeUserPassword(String email, String oldPassword, String newPassword);

    /**
     * 更新用户头像
     *
     * @param userId    用户 ID
     * @param avatarUrl 头像路径
     * @param adminId   执行操作的用户 ID（用户自己更新时传自身 ID，管理员更新时传管理员 ID）
     * @return 影响行数
     */
    int updateUserAvatar(@Param("userId") Integer userId, @Param("avatarUrl") String avatarUrl, @Param("adminId") Integer adminId);

    /**
     * 更新用户昵称
     *
     * @param userId   用户 ID
     * @param nickname 新昵称
     * @param adminId  执行操作的用户 ID（用户自己更新时传自身 ID，管理员更新时传管理员 ID）
     * @return 影响行数
     */
    int updateUserNickname(@Param("userId") Integer userId, @Param("nickname") String nickname, @Param("adminId") Integer adminId);

    /**
     * 逻辑删除用户账户
     *
     * @param userId  用户 ID
     * @param adminId 执行操作的管理员 ID
     * @return 影响行数
     */
    int deleteUserAccount(@Param("userId") Integer userId, @Param("adminId") Integer adminId);

    /**
     * 更新用户邮箱
     *
     * @param userId 用户 ID
     * @param email  新邮箱
     * @return 影响行数
     */
    int updateUserEmail(@Param("userId") Integer userId, @Param("email") String email);

    /**
     * 更新用户角色（仅系统管理员可调用）
     *
     * @param userId  用户 ID
     * @param newRole 新角色代码
     * @param adminId 执行操作的管理员 ID
     * @return 影响行数
     */
    int updateUserRole(@Param("userId") Integer userId, @Param("newRole") Integer newRole, @Param("adminId") Integer adminId);

    /**
     * 更新用户状态（仅系统管理员可调用）
     *
     * @param userId    用户 ID
     * @param newStatus 新状态代码（10-正常, 20-冻结, 30-封禁）
     * @param adminId   执行操作的管理员 ID
     * @return 影响行数
     */
    int updateUserStatus(@Param("userId") Integer userId, @Param("newStatus") Integer newStatus, @Param("adminId") Integer adminId);

    /**
     * 检查用户名是否存在
     *
     * @param username 用户名
     * @return 存在返回 1，不存在返回 0
     */
    int countByUsername(@Param("username") String username);

    /**
     * 检查邮箱是否存在
     *
     * @param email 邮箱
     * @return 存在返回 1，不存在返回 0
     */
    int countByEmail(@Param("email") String email);
}
