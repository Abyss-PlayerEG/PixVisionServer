package top.playereg.pix_vision.service.Impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.playereg.pix_vision.config.FilePathConfig;
import top.playereg.pix_vision.mapper.WorksMapper;
import top.playereg.pix_vision.pojo.Works;
import top.playereg.pix_vision.service.WorkService;

import java.io.File;
import java.util.List;

/**
 * 作品服务实现类
 *
 * @author PlayerEG
 */
@Service
public class WorkServiceImpl implements WorkService {
    private static final Logger log = LoggerFactory.getLogger(WorkServiceImpl.class);

    @Autowired
    private WorksMapper worksMapper;

    /**
     * 分页查询首页作品列表
     *
     * @param page 分页对象
     * @return 分页结果
     * @author PlayerEG
     */
    @Override
    public IPage<Works> selectHomepageWorks(Page<Works> page) {
        return worksMapper.selectHomepageWorks(page);
    }

    /**
     * 批量删除作品（支持单条和批量删除）
     *
     * @param workIds 要删除的作品 ID 列表
     * @param userId  当前用户 ID（用于权限验证）
     * @return 删除结果
     * @author PlayerEG
     */
    @Override
    public Boolean batchDeleteWorks(List<Integer> workIds, Integer userId) {
        if (workIds == null || workIds.isEmpty()) {
            log.warn("作品 ID 列表为空，用户 ID: {}", userId);
            return false;
        }

        // 1. 先查询所有作品信息，获取文件名
        List<Works> worksList = worksMapper.selectBatchIds(workIds);
        if (worksList == null || worksList.isEmpty()) {
            log.warn("未找到对应的作品，用户 ID: {}, 作品 ID 列表: {}", userId, workIds);
            return false;
        }

        // 2. 过滤出属于当前用户的作品（SQL 层面已验证，这里再次确认）
        List<Works> userWorks = worksList.stream()
            .filter(work -> work.getUser_id().equals(userId))
            .filter(work -> !work.getIs_delete()) // 只处理未删除的作品
            .toList();

        if (userWorks.isEmpty()) {
            log.warn("没有可删除的作品（可能不属于当前用户或已删除），用户 ID: {}", userId);
            return false;
        }

        // 3. 将文件后缀名改为 .del
        int renamedCount = 0;
        for (Works work : userWorks) {
            String imgFileName = work.getImg_url();
            if (imgFileName != null && !imgFileName.isEmpty()) {
                File originalFile = new File(FilePathConfig.WorksPath, imgFileName);
                File deletedFile = new File(FilePathConfig.WorksPath, imgFileName + ".del");

                if (originalFile.exists() && !deletedFile.exists()) {
                    boolean renamed = originalFile.renameTo(deletedFile);
                    if (renamed) {
                        renamedCount++;
                        log.info("作品文件重命名为 .del 成功: {} -> {}", imgFileName, imgFileName + ".del");
                    } else {
                        log.error("作品文件重命名失败: {}", imgFileName);
                    }
                } else if (!originalFile.exists()) {
                    log.warn("作品文件不存在，跳过重命名: {}", imgFileName);
                } else {
                    log.warn("作品文件已标记为删除，跳过重命名: {}", imgFileName);
                }
            }
        }

        log.info("文件重命名完成，成功: {}/{}", renamedCount, userWorks.size());

        // 4. 提取需要删除的作品 ID 列表
        List<Integer> validWorkIds = userWorks.stream()
            .map(Works::getWork_id)
            .toList();

        // 5. 执行数据库逻辑删除（SQL 层面再次验证 user_id）
        int affectedRows = worksMapper.batchDeleteWorks(validWorkIds, userId);

        if (affectedRows > 0) {
            log.info("作品删除成功，用户 ID: {}, 删除数量: {}, 文件重命名数量: {}",
                userId, affectedRows, renamedCount);
            return true;
        } else {
            log.error("作品删除失败，用户 ID: {}, 作品 ID 列表: {}", userId, validWorkIds);
            return false;
        }
    }
}
