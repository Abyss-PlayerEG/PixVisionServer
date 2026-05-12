package top.playereg.pix_vision.service.Impl;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import top.playereg.pix_vision.service.PendingReviewsService;

import java.util.Arrays;
import java.util.List;

@SpringBootTest
public class PendingReviewsServiceImplTest {

    @Autowired
    private PendingReviewsService pendingReviewsService;

    @Test
    public void getWorkStatus() {

        int res = pendingReviewsService.getWorkStatus(1);

        System.out.println( "作品状态: " +  res );

    }

    @Test
    public void updateWorkStatusBatch() {
        // 准备测试数据
        List<Integer> workIds = Arrays.asList(1, 2, 3);
        Integer newStatus = 30; // 封禁状态

        // 执行批量更新
        top.playereg.pix_vision.pojo.adminPojo.AdminBatchOperateWorkResult result = 
            pendingReviewsService.updateWorkStatusBatch(workIds, newStatus);

        // 输出结果
        System.out.println("批量更新结果:");
        System.out.println("  - 总数: " + result.getTotalCount());
        System.out.println("  - 成功数: " + result.getSuccessCount());
        System.out.println("  - 失败ID列表: " + result.getFailedWorkIds());
    }
}
