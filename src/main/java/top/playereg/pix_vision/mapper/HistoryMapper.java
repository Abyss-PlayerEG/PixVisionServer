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
}
