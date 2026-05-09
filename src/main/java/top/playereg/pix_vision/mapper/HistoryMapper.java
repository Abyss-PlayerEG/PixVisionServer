package top.playereg.pix_vision.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;
import top.playereg.pix_vision.pojo.History;

/**
 * 历史记录 Mapper 接口
 *
 * @author PlayerEG
 */
@Mapper
@Repository
public interface HistoryMapper extends com.baomidou.mybatisplus.core.mapper.BaseMapper<History> {

    /**
     * 添加用户访问历史记录
     *
     * @param userId 用户 ID
     * @param workId 作品 ID
     * @return 影响行数
     * @author PlayerEG
     */
    int insertHistory(@org.apache.ibatis.annotations.Param("userId") Integer userId, 
                      @org.apache.ibatis.annotations.Param("workId") Integer workId);

    /**
     * 查询用户的访问历史记录（关联作品表）
     *
     * @param userId 用户 ID
     * @param page   分页对象
     * @return 历史记录列表（包含作品信息）
     * @author PlayerEG
     */
    com.baomidou.mybatisplus.core.metadata.IPage<top.playereg.pix_vision.pojo.Works> selectUserHistory(
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<top.playereg.pix_vision.pojo.Works> page,
        @org.apache.ibatis.annotations.Param("userId") Integer userId
    );

    /**
     * 批量删除用户的历史记录
     *
     * @param userId 用户 ID
     * @param workIds 作品 ID 列表
     * @return 影响行数
     * @author PlayerEG
     */
    int batchDeleteHistory(@org.apache.ibatis.annotations.Param("userId") Integer userId, 
                           @org.apache.ibatis.annotations.Param("workIds") java.util.List<Integer> workIds);
}
