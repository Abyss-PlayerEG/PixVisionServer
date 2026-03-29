package top.playereg.pix_vision.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;
import top.playereg.pix_vision.pojo.User;

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
     * 根据用户名查询用户
     *
     * @param username 用户名
     * @return 用户实体
     */
    User selectUserByUsername(String username);

    /**
     * 根据邮箱查询用户
     *
     * @param email 邮箱
     * @return 用户实体
     */
    User selectUserByEmail(String email);
}
