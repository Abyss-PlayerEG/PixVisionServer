package top.playereg.pix_vision.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;
import top.playereg.pix_vision.pojo.User;

/**
 * Mapper 接口类模板
 * @author PlayerEG
 * */
@Mapper
@Repository // 持久层
public interface UserMapper extends BaseMapper<User> {
    // 创建用户
    int createUser(User user);
}
