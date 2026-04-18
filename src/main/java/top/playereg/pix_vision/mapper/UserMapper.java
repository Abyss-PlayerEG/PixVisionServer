package top.playereg.pix_vision.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
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
     * 分页查询用户信息 - 支持用户名、UUID、邮箱查询
     * @param page 分页参数
     * @param user 查询条件对象（可选属性：username, user_uuid, email）
     * @return 分页用户列表
     */
    IPage<User> selectPageUserInfo(
            IPage<?> page,
            User user
    );

    /**
     * 用户密码修改 / 重置密码（通用）
     * @param email 用户的邮箱
     * @param oldPassword 用户的旧密码（可选，为空时不验证旧密码）
     * @param newPassword 用户的新密码
     * @return 影响行数
     * */
    Integer changeUserPassword( String email, String oldPassword, String newPassword );

    /**
     * 更新用户头像
     * @param userId 用户 ID
     * @param avatarUrl 头像路径
     * @return 影响行数
     */
    int updateUserAvatar(@Param("userId") Integer userId, @Param("avatarUrl") String avatarUrl);

    /**
     * 更新用户昵称
     *
     * @param userId   用户 ID
     * @param nickname 新昵称
     * @return 影响行数
     */
    int updateUserNickname(@Param("userId") Integer userId, @Param("nickname") String nickname);
}
