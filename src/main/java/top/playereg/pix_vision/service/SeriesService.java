package top.playereg.pix_vision.service;

import top.playereg.pix_vision.pojo.Series;

import java.util.List;

/**
 * 系列服务接口
 *
 * @author PlayerEG
 */
public interface SeriesService {

    /**
     * 新增作品系列
     *
     * @param userId      用户 ID（从 Token 中获取）
     * @param seriesTitle 系列标题
     * @param aboutText   系列描述文本
     * @return 新增的系列对象
     */
    Series addSeries(Integer userId, String seriesTitle, String aboutText);

    /**
     * 根据用户 ID 查询所有作品系列
     *
     * @param userId 用户 ID
     * @return 作品系列列表
     */
    List<Series> getSeriesByUserId(Integer userId);
}
