package top.playereg.pix_vision.service.Impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.playereg.pix_vision.mapper.SeriesMapper;
import top.playereg.pix_vision.mapper.WorksMapper;
import top.playereg.pix_vision.pojo.Series;
import top.playereg.pix_vision.service.SeriesService;

import java.io.File;
import java.sql.Timestamp;
import java.util.List;

/**
 * 系列服务实现类
 *
 * @author PlayerEG
 */
@Service
public class SeriesServiceImpl implements SeriesService {

    private static final Logger log = LoggerFactory.getLogger(SeriesServiceImpl.class);

    private final SeriesMapper seriesMapper;

    @Autowired
    private WorksMapper worksMapper;

    public SeriesServiceImpl(SeriesMapper seriesMapper) {
        this.seriesMapper = seriesMapper;
    }

    /**
     * 新增作品系列
     *
     * @param userId      用户 ID（从 Token 中获取）
     * @param seriesTitle 系列标题
     * @param aboutText   系列描述文本
     * @return 新增的系列对象
     */
    @Override
    public Series addSeries(Integer userId, String seriesTitle, String aboutText) {
        log.info("开始新增系列，用户 ID: {}, 系列标题: {}", userId, seriesTitle);

        // 检查系列标题是否已存在
        int count = seriesMapper.countSeriesByTitle(userId, seriesTitle);
        if (count > 0) {
            log.warn("系列标题已存在，用户 ID: {}, 系列标题: {}", userId, seriesTitle);
            throw new IllegalArgumentException("系列标题已存在，请使用其他标题");
        }

        // 创建系列对象
        Series series = new Series();
        series.setUser_id(userId);
        series.setSeries_title(seriesTitle);
        series.setAbout_text(aboutText);
        series.setIs_delete(false);

        // 设置时间戳和操作者
        Timestamp now = new Timestamp(System.currentTimeMillis());
        series.setCreate_time(now);
        series.setUpdate_time(now);
        series.setCreate_user(userId);
        series.setUpdate_user(userId);

        // 使用自定义 XML SQL 插入数据
        int result = seriesMapper.insertSeries(series);

        if (result > 0) {
            // useGeneratedKeys 会自动将生成的 series_id 回填到 series 对象中
            log.info("系列新增成功，系列 ID: {}, 用户 ID: {}", series.getSeries_id(), userId);
            return series;
        } else {
            log.error("系列新增失败，用户 ID: {}", userId);
            return null;
        }
    }

    /**
     * 根据用户 ID 查询所有作品系列
     *
     * @param userId 用户 ID
     * @return 作品系列列表
     */
    @Override
    public List<Series> getSeriesByUserId(Integer userId) {
        log.debug("查询用户作品系列 - 用户 ID: {}", userId);

        // 参数校验
        if (userId == null || userId <= 0) {
            log.warn("用户 ID 无效: {}", userId);
            return null;
        }

        // 调用 Mapper 查询
        List<Series> seriesList = seriesMapper.selectSeriesByUserId(userId);

        if (seriesList != null) {
            log.info("查询成功 - 用户 ID: {}, 系列数量: {}", userId, seriesList.size());
        } else {
            log.warn("查询失败 - 用户 ID: {}", userId);
        }

        return seriesList;
    }

    /**
     * 删除作品系列（支持保留或删除系列内作品）
     *
     * @param seriesId    系列 ID
     * @param userId      当前用户 ID（用于权限验证）
     * @param deleteWorks 是否删除系列内的作品（true=删除作品，false=将作品的 series_id 置空）
     * @return 删除结果
     */
    @Override
    public Boolean deleteSeries(Integer seriesId, Integer userId, Boolean deleteWorks) {
        log.info("开始删除系列，系列 ID: {}, 用户 ID: {}, 是否删除作品: {}", seriesId, userId, deleteWorks);

        // 参数校验
        if (seriesId == null || seriesId <= 0) {
            log.warn("系列 ID 无效: {}", seriesId);
            return false;
        }

        if (userId == null || userId <= 0) {
            log.warn("用户 ID 无效: {}", userId);
            return false;
        }

        if (deleteWorks == null) {
            log.warn("删除作品标志为空，默认为 false");
            deleteWorks = false;
        }

        // 1. 验证系列是否存在且属于当前用户
        Series series = seriesMapper.selectSeriesById(seriesId);
        if (series == null || series.getIs_delete()) {
            log.warn("系列不存在或已删除，系列 ID: {}", seriesId);
            return false;
        }

        if (!series.getUser_id().equals(userId)) {
            log.warn("无权删除该系列，系列 ID: {}, 用户 ID: {}", seriesId, userId);
            return false;
        }

        // 2. 处理系列内的作品
        if (deleteWorks) {
            // 选择删除作品：查询系列下所有作品 ID，然后批量删除
            List<Integer> workIds = worksMapper.selectWorkIdsBySeriesId(seriesId, userId);
            if (workIds != null && !workIds.isEmpty()) {
                log.info("系列包含 {} 个作品，将一并删除", workIds.size());

                // 调用 WorkService 的批量删除方法（会重命名文件为 .del）
                // 注意：这里需要注入 WorkService，但为了避免循环依赖，我们直接使用 WorksMapper
                // 先重命名文件
                int renamedCount = renameWorksFilesToDelete(workIds);
                log.info("作品文件重命名完成，成功: {}/{}", renamedCount, workIds.size());

                // 执行逻辑删除
                int deletedCount = worksMapper.batchDeleteWorks(workIds, userId);
                log.info("作品删除完成，删除数量: {}", deletedCount);
            } else {
                log.info("系列下没有作品，跳过作品删除");
            }
        } else {
            // 选择保留作品：将系列下所有作品的 series_id 置空
            int clearedCount = worksMapper.clearSeriesIdBySeriesId(seriesId, userId);
            log.info("已将 {} 个作品的 series_id 置空", clearedCount);
        }

        // 3. 逻辑删除系列
        int affectedRows = seriesMapper.deleteSeriesById(seriesId, userId);

        if (affectedRows > 0) {
            log.info("系列删除成功，系列 ID: {}, 用户 ID: {}", seriesId, userId);
            return true;
        } else {
            log.error("系列删除失败，系列 ID: {}, 用户 ID: {}", seriesId, userId);
            return false;
        }
    }

    /**
     * 重命名作品文件为 .del 后缀
     *
     * @param workIds 作品 ID 列表
     * @return 成功重命名的数量
     */
    private int renameWorksFilesToDelete(List<Integer> workIds) {
        if (workIds == null || workIds.isEmpty()) {
            return 0;
        }

        // 查询作品信息获取文件名
        List<top.playereg.pix_vision.pojo.Works> worksList = worksMapper.selectBatchIds(workIds);
        if (worksList == null || worksList.isEmpty()) {
            return 0;
        }

        int renamedCount = 0;
        for (top.playereg.pix_vision.pojo.Works work : worksList) {
            String imgFileName = work.getImg_url();
            if (imgFileName != null && !imgFileName.isEmpty()) {
                File originalFile = new File(top.playereg.pix_vision.config.FilePathConfig.WorksPath, imgFileName);
                File deletedFile = new File(top.playereg.pix_vision.config.FilePathConfig.WorksPath, imgFileName + ".del");

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

        return renamedCount;
    }
}
