package top.playereg.pix_vision.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;
import top.playereg.pix_vision.pojo.entity.user.UserData;

import java.util.List;

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

    /**
     * 根据用户 ID 查询所有拓展数据（排除逻辑删除）
     *
     * @param userId 用户 ID
     * @return 用户拓展数据列表
     */
    List<UserData> selectUserDataByUserId(@Param("userId") Integer userId);

    /**
     * 删除用户拓展数据（逻辑删除）
     *
     * @param dataId 数据 ID
     * @param userId 用户 ID（用于权限验证）
     * @return 影响行数
     */
    int deleteUserDataById(@Param("dataId") Integer dataId, @Param("userId") Integer userId);

    /**
     * 批量删除用户拓展数据（逻辑删除）
     *
     * @param dataIds 数据 ID 列表
     * @param userId  用户 ID（用于权限验证）
     * @return 影响行数
     */
    int batchDeleteUserDataByIds(@Param("dataIds") List<Integer> dataIds, @Param("userId") Integer userId);

    /**
     * 统计用户指定类型的拓展数据数量（排除逻辑删除）
     *
     * @param userId   用户 ID
     * @param dataName 数据类型名称
     * @return 数量，0 表示不存在
     */
    int countByUserIdAndDataName(@Param("userId") Integer userId, @Param("dataName") String dataName);
}
