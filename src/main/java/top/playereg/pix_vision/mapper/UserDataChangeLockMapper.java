package top.playereg.pix_vision.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;
import top.playereg.pix_vision.pojo.UserDataChangeLock;

import java.util.List;

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

    /**
     * 查询指定用户指定类型的有效待审核记录
     * <p>筛选条件：user_id = ? AND type = ? AND approval_status = 20 AND is_delete = 0</p>
     *
     * @param userId 用户 ID
     * @param type   变更类型（100/200/300）
     * @return 待审核记录列表
     */
    List<UserDataChangeLock> selectPendingByUserAndType(
        @Param("userId") Integer userId,
        @Param("type") Integer type
    );

    /**
     * 软删除指定用户指定类型的待审核记录
     * <p>将 approval_status=20 AND is_delete=0 的记录标记为 is_delete=1</p>
     *
     * @param userId 用户 ID
     * @param type   变更类型（100/200/300）
     * @return 更新的记录数
     */
    int updatePendingToDeleted(
        @Param("userId") Integer userId,
        @Param("type") Integer type
    );
}
