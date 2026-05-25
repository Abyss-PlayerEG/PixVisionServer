package top.playereg.pix_vision.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;
import top.playereg.pix_vision.pojo.UserDataChangeLock;
import top.playereg.pix_vision.pojo.UserDataChangeLockVO;

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
        @Param("user_id") Integer userId,
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
        @Param("user_id") Integer userId,
        @Param("type") Integer type
    );

    // ========== 管理员端 ==========

    /**
     * 分页查询待审核记录（含用户名关联）
     * <p>筛选条件：approval_status=20 AND is_delete=0</p>
     *
     * @param page 分页对象
     * @param type 变更类型筛选（可选，100/200/300）
     * @return 分页结果
     */
    IPage<UserDataChangeLockVO> selectPendingPage(
        Page<UserDataChangeLockVO> page,
        @Param("type") Integer type
    );

    /**
     * 根据 ID 列表批量查询锁定记录
     *
     * @param lockIds lock_id 列表
     * @return 锁定记录列表
     */
    List<UserDataChangeLock> selectLockByIds(@Param("lockIds") List<Integer> lockIds);

    /**
     * 批量更新审核状态
     *
     * @param lockIds        lock_id 列表
     * @param approvalStatus 目标审核状态（10-通过 / 30-拒绝）
     * @return 更新的记录数
     */
    int batchUpdateApprovalStatus(
        @Param("lockIds") List<Integer> lockIds,
        @Param("approvalStatus") Integer approvalStatus
    );
}
