package top.playereg.pix_vision.service.Impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.playereg.pix_vision.mapper.PendingReviewsMapper;
import top.playereg.pix_vision.pojo.adminPojo.AdminBatchOperateCommentResult;
import top.playereg.pix_vision.pojo.adminPojo.AdminBatchOperateWorkResult;
import top.playereg.pix_vision.service.PendingReviewsService;

import java.util.ArrayList;
import java.util.List;

/**
 * 审核实现类
 *
 * @author blue_sky_ks
 */
@Service
public class PendingReviewsServiceImpl implements PendingReviewsService {

    @Autowired
    private PendingReviewsMapper pendingReviewsMapper;

    /**
     * 获取作品状态
     *
     * @param workId 作品ID
     * @return 作品状态
     * @author blue_sky_ks
     */
    public int getWorkStatus(Integer workId) {
        return pendingReviewsMapper.getWorkStatus(workId);
    }

    /**
     * 批量修改作品状态
     *
     * @param workIds 作品ID列表
     * @param status  新状态
     * @return 批量操作结果（包含总数、成功数、失败ID列表）
     * @author blue_sky_ks
     */
    public AdminBatchOperateWorkResult updateWorkStatusBatch(List<Integer> workIds, Integer status) {
        if (workIds == null || workIds.isEmpty()) {
            return new AdminBatchOperateWorkResult(0, 0, new ArrayList<>());
        }

        int totalCount = workIds.size();
        List<Integer> failedWorkIds = new ArrayList<>();
        int successCount = 0;

        // 逐个更新，记录失败的 ID
        for (Integer workId : workIds) {
            try {
                boolean result = pendingReviewsMapper.updateWorkStatusBatch(
                    java.util.Collections.singletonList(workId),
                    status
                );

                if (result) {
                    successCount++;
                } else {
                    failedWorkIds.add(workId);
                }
            } catch (Exception e) {
                // 如果更新异常，也视为失败
                failedWorkIds.add(workId);
            }
        }

        return new AdminBatchOperateWorkResult(totalCount, successCount, failedWorkIds);
    }

    /**
     * 批量删除作品
     *
     * @param workIds  作品ID列表
     * @param isDelete 是否删除
     * @return 批量操作结果（包含总数、成功数、失败ID列表）
     */
    public AdminBatchOperateWorkResult batchDeleteWorks(List<Integer> workIds, Integer isDelete) {
        if (workIds == null || workIds.isEmpty()) {
            return new AdminBatchOperateWorkResult(0, 0, new ArrayList<>());
        }
        int totalCount = workIds.size();
        List<Integer> failedWorkIds = new ArrayList<>();
        int successCount = 0;
        for (Integer workId : workIds) {

            try {
                boolean result = pendingReviewsMapper.deleteWorksBatch(
                    java.util.Collections.singletonList(workId),
                    isDelete
                );

                if (result) {
                    successCount++;
                } else {
                    failedWorkIds.add(workId);
                }
            } catch (Exception e) {
                // 如果更新异常，也视为失败
                failedWorkIds.add(workId);
            }

        }
        return new AdminBatchOperateWorkResult(totalCount, successCount, failedWorkIds);
    }
}
