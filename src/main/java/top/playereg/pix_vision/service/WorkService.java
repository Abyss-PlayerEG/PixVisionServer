package top.playereg.pix_vision.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.web.multipart.MultipartFile;
import top.playereg.pix_vision.pojo.History;
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
     * @param seriesId   系列 ID（可选，精确查询）
     * @param isOriginal 是否原创（可选，精确查询）
     * @return 分页结果
     * @author PlayerEG
     */
    IPage<Works> selectHomepageWorks(
        Page<Works> page,
        String workTitle,
        Integer userId,
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
     * 添加游客访问历史记录
     *
     * @param workId 作品 ID
     * @author PlayerEG
     */
    void addGuestHistory(Integer workId);

    /**
     * 获取用户个人访问历史记录（分页）
     *
     * @param page   分页对象
     * @param userId 用户 ID
     * @return 分页作品列表
     * @author PlayerEG
     */
    IPage<History> getUserHistory(
        Page<History> page,
        Integer userId
    );

    /**
     * 批量删除用户访问历史记录
     *
     * @param workIds 作品 ID 列表
     * @param userId  当前用户 ID（用于权限验证）
     * @return 删除结果
     * @author PlayerEG
     */
    /**
     * 批量删除访问历史记录
     *
     * @param workIds 要删除的作品 ID 列表
     * @param userId  当前用户 ID（用于权限验证）
     * @return 删除结果，true 表示至少有一条记录被成功删除
     * @author PlayerEG
     */
    Boolean batchDeleteHistory(List<Integer> workIds, Integer userId);

    /**
     * 清空用户的所有访问历史记录
     *
     * @param userId 当前用户 ID
     * @return 删除结果
     * @author PlayerEG
     */
    Boolean clearUserHistory(Integer userId);

    /**
     * 修改作品信息（支持部分字段修改）
     *
     * @param workId     作品 ID
     * @param userId     当前用户 ID（用于权限验证）
     * @param workTitle  作品标题（可选，最多 16 个中文字符）
     * @param file       新的图片文件（可选，MultipartFile 类型）
     * @param seriesId   系列 ID（可选，0 表示不属于任何系列）
     * @param isOriginal 是否原创（可选）
     * @param outUrl     外部转载链接（可选）
     * @return 修改结果
     * @author PlayerEG
     */
    Boolean updateWork(
        Integer workId,
        Integer userId,
        String workTitle,
        MultipartFile file,
        Integer seriesId,
        Boolean isOriginal,
        String outUrl
    );

    /**
     * 批量更新作品审核状态（管理员接口）
     *
     * @param workIds        作品 ID 列表
     * @param approvalStatus 审核状态：10 - 正常、20 - 待审核、30 - 未过审
     * @param userId         操作者 ID
     * @return 批量操作结果（包含总数、成功数、失败ID列表）
     * @author PlayerEG
     */
    top.playereg.pix_vision.pojo.adminPojo.AdminBatchOperateWorkResult batchUpdateApprovalStatus(
        List<Integer> workIds,
        Integer approvalStatus,
        Integer userId
    );

    /**
     * 管理员批量删除作品（逻辑删除）
     *
     * @param workIds 作品 ID 列表
     * @return 批量操作结果（包含总数、成功数、失败ID列表）
     * @author PlayerEG
     */
    top.playereg.pix_vision.pojo.adminPojo.AdminBatchOperateWorkResult adminBatchDeleteWorks(
        List<Integer> workIds
    );

    /**
     * 分页查询用户自己的作品列表（只过滤已删除，不过滤审核状态）
     *
     * @param page           分页对象
     * @param userId         用户 ID
     * @param approvalStatus 审核状态（可选，10-正常、20-待审核、30-未过审）
     * @return 分页结果
     * @author PlayerEG
     */
    IPage<Works> getMyWorks(
        Page<Works> page,
        Integer userId,
        Integer approvalStatus
    );

    /**
     * 管理员分页查询作品列表（支持多条件过滤）
     *
     * @param current   当前页码
     * @param size      每页大小
     * @param keyword   关键字（可选，模糊搜索标题）
     * @param orderBy   排序方式：'oldest' - 按最早发布，其他值或 null - 按最新发布（默认）
     * @return 分页结果
     * @author PlayerEG
     */
    IPage<Works> getAdminWorksPage(Long current, Long size, String keyword, String orderBy);

    /**
     * 查询用户统计数据（作品数、点赞总数、收藏总数、查看总数）
     *
     * @param userId 用户 ID
     * @return 包含 workCount, totalLikes, totalStars, totalViews 的 Map
     * @author PlayerEG
     */
    java.util.Map<String, Object> getUserStats(Integer userId);

    /**
     * 批量更新作品标题（管理员接口）
     *
     * @param workIds   作品 ID 列表
     * @param workTitle 作品标题
     * @param userId    操作者 ID
     * @return 批量操作结果（包含总数、成功数、失败ID列表）
     * @author blue_sky_ks
     */
    top.playereg.pix_vision.pojo.adminPojo.AdminBatchOperateWorkResult batchUpdateWorkTitle(
        List<Integer> workIds,
        String workTitle,
        Integer userId
    );
}
