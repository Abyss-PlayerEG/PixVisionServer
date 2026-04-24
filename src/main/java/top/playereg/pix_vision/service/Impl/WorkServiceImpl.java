package top.playereg.pix_vision.service.Impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.playereg.pix_vision.config.FilePathConfig;
import top.playereg.pix_vision.mapper.SeriesMapper;
import top.playereg.pix_vision.mapper.WorksMapper;
import top.playereg.pix_vision.pojo.Series;
import top.playereg.pix_vision.pojo.Works;
import top.playereg.pix_vision.service.WorkService;
import top.playereg.pix_vision.util.ImageUtils;
import top.playereg.pix_vision.util.RegexUtils;

import java.io.File;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

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

    // 允许上传的图片扩展名白名单（仅支持 JPG、JPEG、PNG）
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(
        "jpg", "jpeg", "png"
    );
    @Autowired
    private SeriesMapper seriesMapper;

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
    @Override
    public Integer uploadWork(Integer userId, byte[] fileBytes, String fileName, long fileSize,
                              String workTitle, Integer seriesId, Boolean isOriginal, String outUrl) {
        // 1. 验证文件格式
        String extension = getFileExtension(fileName);
        if (!ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            log.warn("不支持的文件格式: {}", extension);
            throw new IllegalArgumentException("不支持的文件格式，仅支持: " + String.join(", ", ALLOWED_EXTENSIONS));
        }

        // 2. 验证文件大小（最大 32MB）
        long maxSize = 32 * 1024 * 1024; // 32MB
        if (fileSize > maxSize) {
            log.warn("文件大小超出限制: {} bytes ({} MB)", fileSize, fileSize / 1024.0 / 1024.0);
            throw new IllegalArgumentException("文件大小超出限制，最大允许 32MB");
        }

        // 3. 验证文件是否为空或太小
        if (fileBytes.length < 4) {
            log.warn("文件太小，可能不是有效图片，大小: {} bytes", fileBytes.length);
            throw new IllegalArgumentException("文件太小，不是有效的图片");
        }

        // 4. 验证是否为真实的图片格式（通过魔数检查）
        if (!ImageUtils.isValidImage(fileBytes)) {
            log.warn("文件不是有效的图片格式，文件大小: {} bytes", fileBytes.length);
            throw new IllegalArgumentException("文件不是有效的图片格式，请上传 JPG/JPEG/PNG 格式的图片");
        }

        // 5. 验证作品标题长度（最多 16 个中文字符 = 48 字节）
        if (workTitle == null || workTitle.trim().isEmpty()) {
            log.warn("作品标题为空");
            throw new IllegalArgumentException("作品标题不能为空");
        }

        String trimmedTitle = workTitle.trim();
        try {
            int byteLength = trimmedTitle.getBytes("UTF-8").length;
            if (byteLength > 48) {
                log.warn("作品标题过长: {} 字节", byteLength);
                throw new IllegalArgumentException("作品标题过长，最多 16 个中文字符（48 字节），当前: " + byteLength + " 字节");
            }
        } catch (java.io.UnsupportedEncodingException e) {
            // UTF-8 编码不支持的情况极少发生，这里作为防御性编程
            log.error("系统不支持 UTF-8 编码: {}", e.getMessage());
            throw new RuntimeException("系统编码配置错误");
        }

        // 6. 验证系列 ID（null 表示无系列）
        Integer finalSeriesId = null; // 默认为 null，表示不属于任何系列
        if (seriesId != null && seriesId > 0) {
            Series series = seriesMapper.selectSeriesById(seriesId);
            if (series == null || series.getIs_delete()) {
                log.warn("系列不存在或已删除，系列 ID: {}", seriesId);
                throw new IllegalArgumentException("系列不存在或已删除");
            }

            // 验证系列是否属于当前用户
            if (!series.getUser_id().equals(userId)) {
                log.warn("无权在该系列下发布作品，系列 ID: {}, 用户 ID: {}", seriesId, userId);
                throw new SecurityException("无权在该系列下发布作品");
            }

            // 系列验证通过，使用传入的 seriesId
            finalSeriesId = seriesId;
        }

        // 7. 验证转载链接
        if (!isOriginal) {
            if (outUrl == null || outUrl.trim().isEmpty()) {
                log.warn("转载作品未提供外部链接");
                throw new IllegalArgumentException("转载作品必须提供外部链接");
            }
            // URL 格式校验
            if (!RegexUtils.isURL(outUrl.trim())) {
                log.warn("外部链接格式不正确: {}", outUrl);
                throw new IllegalArgumentException("外部链接格式不正确，请输入有效的 URL 地址");
            }
        }

        // 8. 生成唯一文件名（保留原始扩展名）
        String uniqueFileName = UUID.randomUUID().toString().replace("-", "") + "." + extension;
        String savePath = Paths.get(FilePathConfig.WorksPath, uniqueFileName).toString();

        // 9. 保存文件
        File saveFile = new File(savePath);
        File parentDir = saveFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }

        cn.hutool.core.io.FileUtil.writeBytes(fileBytes, saveFile);
        log.info("作品图片保存成功: {}", savePath);

        // 10. 构建 Works 对象并插入数据库
        Works works = new Works();
        works.setUser_id(userId);
        works.setWork_title(workTitle.trim());
        works.setImg_url(uniqueFileName); // 只存文件名
        works.setSeries_id(finalSeriesId); // null 表示不属于任何系列，> 0 表示具体系列 ID
        works.setLike_count(0);
        works.setStar_count(0);
        works.setView_count(0);
        works.setIs_original_work(isOriginal);
        works.setOut_url(isOriginal ? "" : outUrl.trim()); // 原创时为空字符串
        works.setIs_delete(false);
        works.setCreate_user(userId);
        works.setCreate_time(new Timestamp(System.currentTimeMillis()));

        int rows = worksMapper.insert(works);
        if (rows <= 0) {
            log.error("作品发布失败，用户 ID: {}", userId);
            // 如果数据库插入失败，删除已上传的文件
            if (saveFile.exists()) {
                saveFile.delete();
            }
            throw new RuntimeException("作品发布失败");
        }

        log.info("作品发布成功，用户 ID: {}, 作品 ID: {}, 文件路径: {}", userId, works.getWork_id(), uniqueFileName);
        return works.getWork_id();
    }

    /**
     * 获取文件扩展名
     *
     * @param filename 文件名
     * @return 扩展名(不含点)
     * @author PlayerEG
     */
    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf(".");
        if (lastDotIndex > 0 && lastDotIndex < filename.length() - 1) {
            return filename.substring(lastDotIndex + 1);
        }
        return "";
    }
}
