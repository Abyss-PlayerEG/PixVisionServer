package top.playereg.pix_vision.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;
import top.playereg.pix_vision.pojo.entity.GuestHistory;

/**
 * 游客历史记录 Mapper 接口
 *
 * @author PlayerEG
 */
@Mapper
@Repository
public interface GuestHistoryMapper extends com.baomidou.mybatisplus.core.mapper.BaseMapper<GuestHistory> {

    /**
     * 添加游客访问历史记录
     *
     * @param workId 作品 ID
     * @return 影响行数
     * @author PlayerEG
     */
    int insertGuestHistory(@Param("workId") Integer workId);

    /**
     * 统计指定作品的游客访问记录数（用于缓存回源）
     *
     * @param workId 作品 ID
     * @return 游客访问记录总数
     * @author PlayerEG
     */
    int selectCountByWorkId(@Param("workId") Integer workId);
}
