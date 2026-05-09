package top.playereg.pix_vision.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import top.playereg.pix_vision.pojo.Works;

import java.util.List;

/**
 * 作品服务接口
 *
 * @author PlayerEG
 */
public interface WorkService {

    /**
     * 分页查询首页作品列表（支持多条件查询）
     *
     * @param page       分页对象
     * @param workTitle  作品标题（可选，模糊查询）
     * @param userId     用户 ID（可选，精确查询）
     * @param username   用户名（可选，模糊查询）
     * @param nickname   昵称（可选，模糊查询）
     * @param seriesId   系列 ID（可选，精确查询）
     * @param isOriginal 是否原创（可选，精确查询）
     * @return 分页结果
     * @author PlayerEG
     */
    IPage<Works> selectHomepageWorks(
        Page<Works> page,
        String workTitle,
        Integer userId,
        String username,
        String nickname,
        Integer seriesId,
        Boolean isOriginal
    );

    /**
     * 批量删除作品（支持单条和批量删除）
     *
     * @param workIds 要删除的作品 ID 列表
     * @param userId  当前用户 ID（用于权限验证）
     * @return 删除结果
     * @author PlayerEG
     */
    Boolean batchDeleteWorks(
        List<Integer> workIds,
        Integer userId
    );

    /**
     * 上传作品图片
     *
     * @param userId     用户 ID
     * @param fileBytes  图片文件字节数组
     * @param fileName   原始文件名
     * @param fileSize   文件大小
     * @param workTitle  作品标题
     * @param seriesId   系列 ID（null 表示不属于任何系列）
     * @param isOriginal 是否原创
     * @param outUrl     外部转载链接
     * @return 新创建的作品 ID
     * @author PlayerEG
     */
    Integer uploadWork(
        Integer userId,
        byte[] fileBytes,
        String fileName,
        long fileSize,
        String workTitle,
        Integer seriesId,
        Boolean isOriginal,
        String outUrl
    );

    /**
     * 根据 ID 查询单个作品
     *
     * @param workId 作品 ID
     * @return 作品信息，如果不存在或已删除则返回 null
     * @author PlayerEG
     */
    Works getWorkById(Integer workId);

    /**
     * 增加作品浏览次数
     *
     * @param workId 作品 ID
     * @return 是否成功
     * @author PlayerEG
     */
    Boolean incrementViewCount(Integer workId);

    /**
     * 添加用户访问历史记录
     *
     * @param userId 用户 ID
     * @param workId 作品 ID
     * @author PlayerEG
     */
    void addHistory(Integer userId, Integer workId);

    /**
     * 获取用户个人访问历史记录（分页）
     *
     * @param page   分页对象
     * @param userId 用户 ID
     * @return 分页作品列表
     * @author PlayerEG
     */
    com.baomidou.mybatisplus.core.metadata.IPage<Works> getUserHistory(
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<Works> page,
        Integer userId
    );

    /**
     * 修改作品信息（支持部分字段修改）
     *
     * @param workId      作品 ID
     * @param userId      当前用户 ID（用于权限验证）
     * @param workTitle   作品标题（可选，最多 16 个中文字符）
     * @param file        新的图片文件（可选，MultipartFile 类型）
     * @param seriesId    系列 ID（可选，0 表示不属于任何系列）
     * @param isOriginal  是否原创（可选）
     * @param outUrl      外部转载链接（可选）
     * @return 修改结果
     * @author PlayerEG
     */
    Boolean updateWork(
        Integer workId,
        Integer userId,
        String workTitle,
        org.springframework.web.multipart.MultipartFile file,
        Integer seriesId,
        Boolean isOriginal,
        String outUrl
    );
}
