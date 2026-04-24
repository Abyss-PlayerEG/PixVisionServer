package top.playereg.pix_vision.service.Impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import top.playereg.pix_vision.mapper.SeriesMapper;
import top.playereg.pix_vision.pojo.Series;
import top.playereg.pix_vision.service.SeriesService;

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
}
