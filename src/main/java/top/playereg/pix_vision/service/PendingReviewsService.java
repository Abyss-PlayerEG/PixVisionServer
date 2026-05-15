package top.playereg.pix_vision.service;

import top.playereg.pix_vision.pojo.adminPojo.AdminBatchOperateWorkResult;

import java.util.List;

public interface PendingReviewsService {

    /**
     * 获取作品状态
     *
     * @param workId 作品ID
     * @return 作品状态
     */
    int getWorkStatus(Integer workId);

    /**
     * 批量修改作品状态
     *
     * @param workIds 作品ID列表
     * @param status  新状态
     * @return 批量操作结果（包含总数、成功数、失败ID列表）
     */
    AdminBatchOperateWorkResult updateWorkStatusBatch(List<Integer> workIds, Integer status);

    /**
     * 批量删除作品
     *
     * @param workIds  作品ID列表
     * @param isDelete 是否删除
     * @return 批量操作结果（包含总数、成功数、失败ID列表）
     */
    AdminBatchOperateWorkResult batchDeleteWorks(List<Integer> workIds, Integer isDelete);

}
