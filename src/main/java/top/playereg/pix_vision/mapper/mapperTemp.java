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
public interface mapperTemp extends BaseMapper<User> {
    int test();
}
