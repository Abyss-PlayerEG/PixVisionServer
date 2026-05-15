package top.playereg.pix_vision.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;
import top.playereg.pix_vision.pojo.PendingReviews;

/**
 * 待审核数据访问层
 * <p>
 * 继承 MyBatis-Plus 的 BaseMapper，自动提供 CRUD 方法
 *
 * @author blue_sky_ks
 * @see PendingReviews
 */
@Mapper
@Repository
public interface PendingReviewsMapper extends BaseMapper<PendingReviews> {

    /**
     * 获取作品状态
     * @param workId 作品ID
     * @return 作品状态
     * */
    int getWorkStatus(Integer workId);

    /**
     * 批量修改作品状态
     * @param workIds 作品ID列表
     * @param status 新状态
     * @return 修改成功与否
     * */
    boolean updateWorkStatusBatch(java.util.List<Integer> workIds, Integer status);

    /**
     * 批量删除作品
     * @param workIds 作品ID列表
     * @param isDelete 是否删除
     * @return 批量操作结果（包含总数、成功数、失败ID列表）
     * */
    boolean deleteWorksBatch(java.util.List<Integer> workIds, Integer isDelete);
}
