package top.playereg.pix_vision.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;
import top.playereg.pix_vision.pojo.userPojo.UserData;

/**
 * 用户拓展数据 Mapper 接口
 *
 * @author PlayerEG
 */
@Mapper
@Repository
public interface UserDataMapper extends BaseMapper<UserData> {

    /**
     * 新增用户拓展数据
     *
     * @param userData 用户拓展数据实体
     * @return 影响行数
     */
    int insertUserData(UserData userData);
}
