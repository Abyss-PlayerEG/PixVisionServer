package top.playereg.pix_vision.mapper;

public interface PendingReviewsMapper {

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

}
