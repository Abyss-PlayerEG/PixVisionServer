package top.playereg.pix_vision.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;
import top.playereg.pix_vision.pojo.UserDataChangeLock;

/**
 * 用户数据变更锁定 Mapper
 *
 * @author PlayerEG
 */
@Mapper
@Repository
public interface UserDataChangeLockMapper extends BaseMapper<UserDataChangeLock> {

    /**
     * 插入一条变更锁定记录
     *
     * @param lock 锁定记录实体
     * @return 影响行数
     */
    int insertLock(UserDataChangeLock lock);
}
