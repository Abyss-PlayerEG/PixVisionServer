package top.playereg.pix_vision.service.Impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import top.playereg.pix_vision.config.FilePathConfig;
import top.playereg.pix_vision.mapper.GuestHistoryMapper;
import top.playereg.pix_vision.mapper.HistoryMapper;
import top.playereg.pix_vision.mapper.SeriesMapper;
import top.playereg.pix_vision.mapper.WorksMapper;
import top.playereg.pix_vision.pojo.*;
import top.playereg.pix_vision.service.ContentAuditService;
import top.playereg.pix_vision.service.WorkService;
import top.playereg.pix_vision.util.ImageUtils;
import top.playereg.pix_vision.util.PageUtils;
import top.playereg.pix_vision.util.PixVisionLogger;
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
    private static final PixVisionLogger log = PixVisionLogger.create(WorkServiceImpl.class);

    @Autowired
    private WorksMapper worksMapper;

    @Autowired
    private HistoryMapper historyMapper;
    @Autowired
    private SeriesMapper seriesMapper;
    @Autowired
    private GuestHistoryMapper guestHistoryMapper;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private ContentAuditService contentAuditService;

    private static final String VIEW_COUNT_KEY_PREFIX = "pix:work:view:";
    private static final long CACHE_TTL_HOURS = 2; // Redis缓存TTL：2小时

    /** 最后作品 ID 缓存键 */
    private static final String LAST_WORK_ID_CACHE_KEY = "pix:work:last-id";
    /** 最后作品 ID 缓存 TTL：1分钟 */
    private static final long LAST_WORK_ID_CACHE_TTL_MINUTES = 5;

    // 允许上传的图片扩展名白名单（仅支持 JPG、JPEG、PNG）
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(
        "jpg", "jpeg", "png"
    );

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
    @Override
    public IPage<Works> selectHomepageWorks(Page<Works> page, String workTitle, Integer userId, Integer seriesId, Boolean isOriginal) {
        IPage<Works> result = worksMapper.selectHomepageWorks(page, workTitle, userId, seriesId, isOriginal);

        // 为列表中的每个作品填充最新的浏览量（优先 Redis，其次回源）
        if (result != null && !result.getRecords().isEmpty()) {
            for (Works work : result.getRecords()) {
                try {
                    String key = VIEW_COUNT_KEY_PREFIX + work.getWork_id();
                    Object viewCountObj = redisTemplate.opsForValue().get(key);
                    if (viewCountObj != null) {
                        work.setView_count(((Number) viewCountObj).intValue());
                    } else {
                        // Redis 缺失，触发回源并缓存
                        int dbCount = worksMapper.selectTotalViewCountByWorkId(work.getWork_id());
                        work.setView_count(dbCount);
                        redisTemplate.opsForValue().set(key, dbCount, CACHE_TTL_HOURS, java.util.concurrent.TimeUnit.HOURS);
                    }
                } catch (Exception e) {
                    log.warn("列表页填充浏览量失败，作品 ID: {}", work.getWork_id());
                }
            }
        }

        return result;
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

        // 3. 将原图和封面文件后缀名改为 .del（处理各种状态的文件）
        int renamedCount = 0;
        for (Works work : userWorks) {
            String imgFileName = work.getImg_url();
            if (imgFileName != null && !imgFileName.isEmpty()) {
                // 重命名原图文件
                if (renameWorkFile(imgFileName, ".del")) {
                    renamedCount++;
                }

                // 同步重命名封面文件（封面可能不存在，静默忽略）
                String thumbFileName = thumbFileName(imgFileName);
                if (thumbFileName != null && renameWorkFile(thumbFileName, ".del")) {
                    renamedCount++;
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
    public WorkUploadResult uploadWork(Integer userId, byte[] fileBytes, String fileName, long fileSize,
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

        // 8. 调用 AI 审核服务对作品标题进行内容安全审核
        String auditText;
        if (!isOriginal && outUrl != null && !outUrl.trim().isEmpty()) {
            auditText = "作品标题: " + workTitle.trim() + "\n转载地址: " + outUrl.trim();
        } else {
            auditText = "作品标题: " + workTitle.trim();
        }

        Integer approvalStatus = 20; // 默认待审核
        String auditReason = null;
        ContentAuditResult auditResult = contentAuditService.auditContent(auditText);
        if (auditResult != null) {
            auditReason = auditResult.getReason();
            switch (auditResult.getStatus()) {
                case "violation":
                    approvalStatus = 30; // 违规，未过审
                    break;
                case "normal":
                case "neutral":
                default:
                    approvalStatus = 20; // 正常/中立/未知，待审核
                    break;
            }
            log.info("AI 作品审核结果 - 状态: {}, 原因: {}, 命中敏感词: {}, 最终审核状态: {}",
                auditResult.getStatus(), auditResult.getReason(), auditResult.getInsult_words(), approvalStatus);
        }

        // 9. 生成唯一文件名（根据审核状态决定文件后缀）
        String uniqueFileName = UUID.randomUUID().toString().replace("-", "") + "." + extension;
        String fileSuffix = (approvalStatus == 30) ? ".fail" : ".pend";
        String fileNameWithSuffix = uniqueFileName + fileSuffix;
        String savePath = Paths.get(FilePathConfig.WorksPath, fileNameWithSuffix).toString();

        // 10. 保存文件
        File saveFile = new File(savePath);
        File parentDir = saveFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }

        cn.hutool.core.io.FileUtil.writeBytes(fileBytes, saveFile);
        log.info("作品图片保存成功（审核状态: {}）: {}", approvalStatus, savePath);

        // 10.5 生成并保存封面缩略图
        String thumbName = thumbFileName(uniqueFileName);
        String thumbSavePath = Paths.get(FilePathConfig.WorksPath, thumbName + fileSuffix).toString();
        try {
            byte[] thumbBytes = ImageUtils.generateThumbnail(fileBytes, 512);
            cn.hutool.core.io.FileUtil.writeBytes(thumbBytes, new File(thumbSavePath));
            log.info("封面缩略图保存成功（审核状态: {}）: {}", approvalStatus, thumbSavePath);
        } catch (Exception e) {
            log.error("封面生成失败，thumb_url 设为 NULL，原图: {}", uniqueFileName, e);
            thumbName = null;
        }

        // 11. 构建 Works 对象并插入数据库（数据库存储正常格式的文件名）
        Works works = new Works();
        works.setUser_id(userId);
        works.setWork_title(workTitle.trim());
        works.setImg_url(uniqueFileName); // 数据库存储正常格式（如 uuid.png），不随审核状态变化
        works.setThumb_url(thumbName); // 封面缩略图文件名（如 uuid_thumb.jpg），生成失败时为 null
        works.setSeries_id(finalSeriesId); // null 表示不属于任何系列，> 0 表示具体系列 ID
        works.setLike_count(0);
        works.setStar_count(0);
        works.setView_count(0);
        works.setIs_original_work(isOriginal);
        works.setOut_url(isOriginal ? "" : outUrl.trim()); // 原创时为空字符串
        works.setApproval_status(approvalStatus);
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
        return new WorkUploadResult(works.getWork_id(), approvalStatus, auditReason);
    }

    /**
     * 根据 ID 查询单个作品
     *
     * @param workId 作品 ID
     * @return 作品信息，如果不存在或已删除则返回 null
     * @author PlayerEG
     */
    @Override
    public Works getWorkById(Integer workId) {
        if (workId == null || workId <= 0) {
            log.warn("作品 ID 无效: {}", workId);
            return null;
        }

        Works work = worksMapper.selectWorkById(workId);

        // 检查作品是否存在且未删除
        if (work == null || work.getIs_delete()) {
            log.warn("作品不存在或已删除，作品 ID: {}", workId);
            return null;
        }

        // 从 Redis 获取最新的浏览量并覆盖
        try {
            String key = VIEW_COUNT_KEY_PREFIX + workId;
            Object viewCountObj = redisTemplate.opsForValue().get(key);
            if (viewCountObj != null) {
                work.setView_count(((Number) viewCountObj).intValue());
            } else {
                // Redis 缓存缺失，触发回源：从数据库统计并重新缓存
                log.info("Redis 缓存缺失，触发浏览量回源，作品 ID: {}", workId);
                int dbCount = worksMapper.selectTotalViewCountByWorkId(workId);
                work.setView_count(dbCount);
                // 存入 Redis 并设置 TTL
                redisTemplate.opsForValue().set(key, dbCount, CACHE_TTL_HOURS, java.util.concurrent.TimeUnit.HOURS);
            }
        } catch (Exception e) {
            log.warn("从 Redis 获取浏览量失败，使用数据库默认值，作品 ID: {}", workId);
        }

        log.info("查询作品成功，作品 ID: {}", workId);
        return work;
    }

    /**
     * 增加作品浏览次数（优化为 Redis 计数）
     *
     * @param workId 作品 ID
     * @return 是否成功
     * @author PlayerEG
     */
    @Override
    public Boolean incrementViewCount(Integer workId) {
        if (workId == null || workId <= 0) {
            log.warn("作品 ID 无效，无法增加浏览次数: {}", workId);
            return false;
        }

        try {
            String key = VIEW_COUNT_KEY_PREFIX + workId;
            // 1. 在 Redis 中自增
            Long count = redisTemplate.opsForValue().increment(key);
            // 2. 如果自增后为 1，说明是刚创建的 Key，设置过期时间
            if (count != null && count == 1) {
                redisTemplate.expire(key, CACHE_TTL_HOURS, java.util.concurrent.TimeUnit.HOURS);
            }
            log.debug("作品浏览量 Redis 自增成功，作品 ID: {}, 当前值: {}", workId, count);
            return true;
        } catch (Exception e) {
            log.error("Redis 增加浏览量异常，作品 ID: {}, 错误: {}", workId, e.getMessage());
            // 降级处理：如果 Redis 失败，可以尝试直接更新数据库（可选）
            return false;
        }
    }

    /**
     * 添加用户访问历史记录（同步更新 Redis 浏览量）
     *
     * @param userId 用户 ID
     * @param workId 作品 ID
     * @author PlayerEG
     */
    @Override
    public void addHistory(Integer userId, Integer workId) {
        if (userId == null || workId == null) {
            return;
        }

        try {
            int rows = historyMapper.insertHistory(userId, workId);
            if (rows > 0) {
                log.debug("添加历史记录成功，用户 ID: {}, 作品 ID: {}", userId, workId);
                // 历史记录插入成功后，同步增加 Redis 中的浏览量计数
                incrementViewCount(workId);
            } else {
                log.warn("添加历史记录失败（影响行数为 0），用户 ID: {}, 作品 ID: {}", userId, workId);
            }
        } catch (Exception e) {
            log.error("添加历史记录异常，用户 ID: {}, 作品 ID: {}, 错误: {}", userId, workId, e.getMessage());
        }
    }

    /**
     * 清空用户的所有访问历史记录
     *
     * @param userId 当前用户 ID
     * @return 删除结果
     * @author PlayerEG
     */
    @Override
    public Boolean clearUserHistory(Integer userId) {
        if (userId == null || userId <= 0) {
            log.warn("无效的用户 ID，无法清空历史记录: {}", userId);
            return false;
        }

        int affectedRows = historyMapper.clearAllHistoryByUserId(userId);
        if (affectedRows > 0) {
            log.info("清空历史记录成功，用户 ID: {}, 删除数量: {}", userId, affectedRows);
            return true;
        } else {
            log.info("无历史记录可清空，用户 ID: {}", userId);
            return true; // 即使没有记录也视为成功
        }
    }

    /**
     * 添加游客访问历史记录（同步更新 Redis 浏览量）
     *
     * @param workId 作品 ID
     * @author PlayerEG
     */
    public void addGuestHistory(Integer workId) {
        if (workId == null) {
            return;
        }

        try {
            int rows = guestHistoryMapper.insertGuestHistory(workId);
            if (rows > 0) {
                log.debug("添加游客历史记录成功，作品 ID: {}", workId);
                // 历史记录插入成功后，同步增加 Redis 中的浏览量计数
                incrementViewCount(workId);
            } else {
                log.warn("添加游客历史记录失败（影响行数为 0），作品 ID: {}", workId);
            }
        } catch (Exception e) {
            log.error("添加游客历史记录异常，作品 ID: {}, 错误: {}", workId, e.getMessage());
        }
    }

    /**
     * 获取用户个人访问历史记录（分页）
     *
     * @param page   分页对象
     * @param userId 用户 ID
     * @return 分页作品列表
     * @author PlayerEG
     */
    @Override
    public IPage<History> getUserHistory(
        Page<History> page,
        Integer userId
    ) {
        if (userId == null || userId <= 0) {
            log.warn("无效的用户 ID，无法查询历史记录: {}", userId);
            return new Page<>();
        }

        log.info("查询用户历史记录，用户 ID: {}, 页码: {}, 每页大小: {}", userId, page.getCurrent(), page.getSize());
        return historyMapper.selectUserHistory(page, userId);
    }

    /**
     * 批量删除用户访问历史记录
     *
     * @param workIds 作品 ID 列表
     * @param userId  当前用户 ID（用于权限验证）
     * @return 删除结果
     * @author PlayerEG
     */
    @Override
    public Boolean batchDeleteHistory(List<Integer> workIds, Integer userId) {
        if (workIds == null || workIds.isEmpty()) {
            log.warn("作品 ID 列表为空，用户 ID: {}", userId);
            return false;
        }

        // 执行数据库逻辑删除（SQL 层面验证 user_id，确保只能删除自己的历史记录）
        int affectedRows = historyMapper.batchDeleteHistory(userId, workIds);

        if (affectedRows > 0) {
            log.info("历史记录删除成功，用户 ID: {}, 删除数量: {}", userId, affectedRows);
            return true;
        } else {
            log.warn("历史记录删除失败或无匹配记录，用户 ID: {}, 作品 ID 列表: {}", userId, workIds);
            return false;
        }
    }

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
    @Override
    public Boolean updateWork(Integer workId, Integer userId, String workTitle,
                              org.springframework.web.multipart.MultipartFile file,
                              Integer seriesId,
                              Boolean isOriginal, String outUrl) {
        log.info("开始修改作品，作品 ID: {}, 用户 ID: {}", workId, userId);

        // 参数校验
        if (workId == null || workId <= 0) {
            log.warn("作品 ID 无效: {}", workId);
            return false;
        }

        if (userId == null || userId <= 0) {
            log.warn("用户 ID 无效: {}", userId);
            return false;
        }

        // 检查是否所有参数都为空
        boolean allNull = (workTitle == null || workTitle.trim().isEmpty())
            && (file == null || file.isEmpty())
            && seriesId == null
            && isOriginal == null
            && (outUrl == null || outUrl.trim().isEmpty());

        if (allNull) {
            log.warn("所有修改参数均为空，作品 ID: {}", workId);
            return null; // 返回 null 表示无修改内容
        }

        // 验证作品是否存在且属于当前用户
        Works existingWork = worksMapper.selectWorkById(workId);
        if (existingWork == null || existingWork.getIs_delete()) {
            log.warn("作品不存在或已删除，作品 ID: {}", workId);
            throw new IllegalArgumentException("作品不存在或已删除");
        }

        if (!existingWork.getUser_id().equals(userId)) {
            log.warn("无权修改该作品，作品 ID: {}, 用户 ID: {}", workId, userId);
            throw new SecurityException("无权修改该作品");
        }

        // 保存旧图片文件名，用于后续删除
        String oldImgUrl = existingWork.getImg_url();
        String newImgUrl = null; // 新图片文件名
        String newThumbUrl = null; // 新封面文件名

        // 处理图片文件上传（如果提供）
        if (file != null && !file.isEmpty()) {
            // 1. 验证文件格式
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || originalFilename.isEmpty()) {
                log.warn("文件名为空");
                throw new IllegalArgumentException("文件名不能为空");
            }

            String extension = getFileExtension(originalFilename);
            if (!ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
                log.warn("不支持的文件格式: {}", extension);
                throw new IllegalArgumentException("不支持的文件格式，仅支持: " + String.join(", ", ALLOWED_EXTENSIONS));
            }

            // 2. 验证文件大小（最大 32MB）
            long fileSize = file.getSize();
            long maxSize = 32 * 1024 * 1024; // 32MB
            if (fileSize > maxSize) {
                log.warn("文件大小超出限制: {} bytes ({} MB)", fileSize, fileSize / 1024.0 / 1024.0);
                throw new IllegalArgumentException("文件大小超出限制，最大允许 32MB");
            }

            // 3. 读取文件字节数组
            byte[] fileBytes;
            try {
                fileBytes = file.getBytes();
            } catch (java.io.IOException e) {
                log.error("读取文件失败: {}", e.getMessage());
                throw new RuntimeException("文件读取失败");
            }

            // 4. 验证是否为真实的图片格式（通过魔数检查）
            if (!ImageUtils.isValidImage(fileBytes)) {
                log.warn("文件不是有效的图片格式，文件大小: {} bytes", fileBytes.length);
                throw new IllegalArgumentException("文件不是有效的图片格式，请上传 JPG/JPEG/PNG 格式的图片");
            }

            // 5. 生成唯一文件名（保留原始扩展名，添加 .pend 后缀表示待审核）
            String uniqueFileName = UUID.randomUUID().toString().replace("-", "") + "." + extension;
            String pendFileName = uniqueFileName + ".pend"; // 待审核文件
            String savePath = Paths.get(FilePathConfig.WorksPath, pendFileName).toString();

            // 6. 保存新文件（以 .pend 后缀保存）
            File saveFile = new File(savePath);
            File parentDir = saveFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }

            cn.hutool.core.io.FileUtil.writeBytes(fileBytes, saveFile);
            log.info("新作品图片保存成功（待审核状态）: {}", savePath);

            // 6.5 生成并保存新封面缩略图
            String thumbName = thumbFileName(uniqueFileName);
            String thumbSavePath = Paths.get(FilePathConfig.WorksPath, thumbName + ".pend").toString();
            try {
                byte[] thumbBytes = ImageUtils.generateThumbnail(fileBytes, 400);
                cn.hutool.core.io.FileUtil.writeBytes(thumbBytes, new File(thumbSavePath));
                newThumbUrl = thumbName;
                log.info("新封面缩略图保存成功: {}", thumbSavePath);
            } catch (Exception e) {
                log.error("新封面生成失败，thumb_url 设为 NULL，原图: {}", uniqueFileName, e);
                newThumbUrl = null;
            }

            // 7. 删除旧图片文件（处理各种状态的文件）
            if (oldImgUrl != null && !oldImgUrl.isEmpty()) {
                deleteOldImageFile(oldImgUrl);
            }

            // 8. 设置新图片 URL（数据库存储正常格式）
            newImgUrl = uniqueFileName;
        }

        // 验证作品标题长度（如果提供）
        if (workTitle != null && !workTitle.trim().isEmpty()) {
            String trimmedTitle = workTitle.trim();
            try {
                int byteLength = trimmedTitle.getBytes("UTF-8").length;
                if (byteLength > 48) {
                    log.warn("作品标题过长: {} 字节", byteLength);
                    throw new IllegalArgumentException("作品标题过长，最多 16 个中文字符（48 字节），当前: " + byteLength + " 字节");
                }
                workTitle = trimmedTitle; // 使用去除空格后的标题
            } catch (java.io.UnsupportedEncodingException e) {
                log.error("系统不支持 UTF-8 编码: {}", e.getMessage());
                throw new RuntimeException("系统编码配置错误");
            }
        } else {
            workTitle = null; // 空字符串转为 null，不更新
        }

        // 处理系列 ID（如果提供）
        Integer finalSeriesId = null;
        boolean shouldUpdateSeriesId = false; // 标记是否需要更新 series_id
        if (seriesId != null) {
            if (seriesId == 0) {
                // seriesId = 0 表示清空系列，设置为 NULL
                finalSeriesId = null;
                shouldUpdateSeriesId = true; // 需要更新为 NULL
                log.info("将作品从系列中移除，作品 ID: {}", workId);
            } else if (seriesId > 0) {
                // seriesId > 0 需要验证系列是否存在且属于当前用户
                Series series = seriesMapper.selectSeriesById(seriesId);
                if (series == null || series.getIs_delete()) {
                    log.warn("系列不存在或已删除，系列 ID: {}", seriesId);
                    throw new IllegalArgumentException("系列不存在或已删除");
                }

                // 验证系列是否属于当前用户
                if (!series.getUser_id().equals(userId)) {
                    log.warn("无权将作品关联到该系列，系列 ID: {}, 用户 ID: {}", seriesId, userId);
                    throw new SecurityException("无权将作品关联到该系列");
                }

                // 系列验证通过，使用传入的 seriesId
                finalSeriesId = seriesId;
                shouldUpdateSeriesId = true; // 需要更新为具体值
                log.info("将作品关联到系列，作品 ID: {}, 系列 ID: {}", workId, seriesId);
            } else {
                // seriesId < 0 是无效值
                log.warn("无效的系列 ID: {}", seriesId);
                throw new IllegalArgumentException("系列 ID 不能为负数");
            }
        }

        // 验证转载链接（处理原创/转载逻辑）
        Boolean finalIsOriginal = isOriginal;
        String finalOutUrl = outUrl;

        if (finalIsOriginal != null) {
            if (!finalIsOriginal) {
                // 设置为非原创（转载）
                // 必须提供外部链接
                if (finalOutUrl == null || finalOutUrl.trim().isEmpty()) {
                    log.warn("转载作品未提供外部链接");
                    throw new IllegalArgumentException("转载作品必须提供外部链接");
                }
                // URL 格式校验
                if (!RegexUtils.isURL(finalOutUrl.trim())) {
                    log.warn("外部链接格式不正确: {}", finalOutUrl);
                    throw new IllegalArgumentException("外部链接格式不正确，请输入有效的 URL 地址");
                }
                finalOutUrl = finalOutUrl.trim();
            } else {
                // 设置为原创
                // 如果提供了 outUrl，报错
                if (finalOutUrl != null && !finalOutUrl.trim().isEmpty()) {
                    log.warn("原创作品不能填写外部链接");
                    throw new IllegalArgumentException("原创作品不能填写外部链接");
                }
                // 将 outUrl 设置为空字符串（清空原有的转载链接）
                finalOutUrl = "";
            }
        } else {
            // isOriginal 为 null，不修改原创状态
            // 但如果提供了 outUrl，需要验证格式
            if (finalOutUrl != null && !finalOutUrl.trim().isEmpty()) {
                finalOutUrl = finalOutUrl.trim();
                // 验证 URL 格式
                if (!RegexUtils.isURL(finalOutUrl)) {
                    log.warn("外部链接格式不正确: {}", finalOutUrl);
                    throw new IllegalArgumentException("外部链接格式不正确，请输入有效的 URL 地址");
                }
            } else {
                finalOutUrl = null; // 空字符串转为 null，不更新
            }
        }

        // 执行更新（使用 newImgUrl、newThumbUrl、finalSeriesId 和 shouldUpdateSeriesId）
        int affectedRows = worksMapper.updateWorkInfo(workId, userId, workTitle, newImgUrl, newThumbUrl, finalSeriesId, shouldUpdateSeriesId, finalIsOriginal, finalOutUrl);

        if (affectedRows > 0) {
            log.info("作品修改成功，作品 ID: {}, 用户 ID: {}, 审核状态已重置为待审核", workId, userId);
            return true;
        } else {
            log.error("作品修改失败，作品 ID: {}, 用户 ID: {}", workId, userId);
            return false;
        }
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

    /**
     * 根据原图文件名推导封面缩略图文件名
     * <p>
     * 将原图的扩展名替换为 _thumb.jpg，生成封面文件名。
     * 支持 png、jpg、jpeg 三种格式的原图。
     *
     * @param imgUrl 原图文件名（如 uuid.png）
     * @return 封面文件名（如 uuid_thumb.jpg），输入为 null 时返回 null
     * @author PlayerEG
     */
    private String thumbFileName(String imgUrl) {
        if (imgUrl == null) return null;
        return imgUrl.replaceFirst("\\.(png|jpg|jpeg)$", "_thumb.jpg");
    }

    /**
     * 删除旧图片文件（重命名为 .del 后缀），同步处理原图和封面
     *
     * @param imgFileName 原图文件名（数据库存储的正常格式，如 uuid.png）
     * @author PlayerEG
     */
    private void deleteOldImageFile(String imgFileName) {
        if (imgFileName == null || imgFileName.isEmpty()) {
            return;
        }

        // 1. 重命名旧原图文件为 .del
        renameWorkFile(imgFileName, ".del");

        // 2. 同步重命名旧封面文件为 .del（封面可能不存在，静默忽略）
        String thumbFileName = thumbFileName(imgFileName);
        if (thumbFileName != null) {
            renameWorkFile(thumbFileName, ".del");
        }
    }

    /**
     * 批量更新作品审核状态（管理员接口）
     *
     * @param workIds        作品 ID 列表
     * @param approvalStatus 审核状态：10 - 正常、20 - 待审核、30 - 未过审
     * @return 批量操作结果（包含总数、成功数、失败ID列表）
     * @author PlayerEG
     */
    @Override
    public top.playereg.pix_vision.pojo.adminPojo.AdminBatchOperateWorkResult batchUpdateApprovalStatus(
        List<Integer> workIds,
        Integer approvalStatus,
        Integer userId
    ) {
        if (workIds == null || workIds.isEmpty()) {
            return new top.playereg.pix_vision.pojo.adminPojo.AdminBatchOperateWorkResult(0, 0, new java.util.ArrayList<>());
        }

        int totalCount = workIds.size();

        // 1. 先查询所有作品信息，获取文件名和当前审核状态
        List<Works> worksList = worksMapper.selectBatchIds(workIds);
        if (worksList == null || worksList.isEmpty()) {
            log.warn("未找到对应的作品，作品 ID 列表: {}", workIds);
            return new top.playereg.pix_vision.pojo.adminPojo.AdminBatchOperateWorkResult(totalCount, 0, workIds);
        }

        // 2. 过滤出未删除的作品
        List<Works> validWorks = worksList.stream()
            .filter(work -> !work.getIs_delete())
            .toList();

        if (validWorks.isEmpty()) {
            log.warn("没有可更新的作品（可能已全部删除），作品 ID 列表: {}", workIds);
            return new top.playereg.pix_vision.pojo.adminPojo.AdminBatchOperateWorkResult(totalCount, 0, workIds);
        }

        // 3. 根据新的审核状态重命名原图和封面文件
        int renamedCount = 0;
        for (Works work : validWorks) {
            String imgFileName = work.getImg_url();
            if (imgFileName != null && !imgFileName.isEmpty()) {
                // 确定目标后缀
                String targetSuffix = getFileSuffixByApprovalStatus(approvalStatus);

                // 重命名原图文件
                if (renameWorkFile(imgFileName, targetSuffix)) {
                    renamedCount++;
                }

                // 同步重命名封面文件（封面可能不存在，静默忽略）
                String thumbFileName = thumbFileName(imgFileName);
                if (thumbFileName != null && renameWorkFile(thumbFileName, targetSuffix)) {
                    renamedCount++;
                }
            }
        }

        log.info("文件重命名完成，成功: {}/{}, 操作者 ID: {}", renamedCount, validWorks.size(), userId);

        // 4. 使用自定义 SQL 批量更新审核状态
        List<Integer> validWorkIds = validWorks.stream()
            .map(Works::getWork_id)
            .toList();
        int affectedRows = worksMapper.adminBatchUpdateApprovalStatus(validWorkIds, approvalStatus, userId);

        int successCount = affectedRows > 0 ? affectedRows : 0;
        List<Integer> failedWorkIds = new java.util.ArrayList<>();

        // 计算失败的 ID（简化处理：如果影响行数小于总数，则认为全部失败）
        if (affectedRows < validWorkIds.size()) {
            failedWorkIds.addAll(validWorkIds);
        }

        log.info("批量更新作品审核状态完成 - 总数: {}, 成功: {}, 失败: {}, 新状态: {}, 文件重命名: {}, 操作者 ID: {}",
            totalCount, successCount, failedWorkIds.size(), approvalStatus, renamedCount, userId);

        return new top.playereg.pix_vision.pojo.adminPojo.AdminBatchOperateWorkResult(
            totalCount, successCount, failedWorkIds
        );
    }

    /**
     * 根据审核状态获取文件后缀
     *
     * @param approvalStatus 审核状态（10-正常、20-待审核、30-未过审）
     * @return 文件后缀（空字符串表示正常格式，.pend 表示待审核，.fail 表示未过审）
     * @author PlayerEG
     */
    private String getFileSuffixByApprovalStatus(Integer approvalStatus) {
        return switch (approvalStatus) {
            case 10 -> "";      // 正常：无后缀
            case 20 -> ".pend"; // 待审核：.pend 后缀
            case 30 -> ".fail"; // 未过审：.fail 后缀
            default -> "";      // 默认无后缀
        };
    }

    /**
     * 查找作品实际存在的文件（尝试不同的后缀）
     *
     * @param baseFileName 基础文件名（如 uuid.png）
     * @return 实际存在的文件对象，如果都不存在则返回 null
     * @author PlayerEG
     */
    private File findActualWorkFile(String baseFileName) {
        // 尝试的顺序：正常格式 > .pend > .fail
        String[] possibleSuffixes = {"", ".pend", ".fail"};

        for (String suffix : possibleSuffixes) {
            String fileName = baseFileName + suffix;
            File file = new File(FilePathConfig.WorksPath, fileName);
            if (file.exists() && file.isFile()) {
                return file;
            }
        }

        return null; // 都没找到
    }

    /**
     * 将作品文件重命名为指定后缀
     * <p>
     * 自动查找当前实际存在的文件（可能为正常格式、.pend 或 .fail），
     * 并将其重命名为目标后缀格式。如果目标文件已存在或当前文件已是目标状态，则跳过。
     * <p>
     * 封面文件可能不存在（thumb_url 为 NULL 时），重命名失败不会抛出异常，静默忽略。
     *
     * @param baseFileName 基础文件名（如 uuid.png 或 uuid_thumb.jpg），正常格式，不含状态后缀
     * @param targetSuffix 目标后缀（如 ".del"、".pend"、".fail"，空字符串表示正常格式）
     * @return 是否成功重命名（null 输入、文件不存在、目标已存在均返回 false）
     * @author PlayerEG
     */
    private boolean renameWorkFile(String baseFileName, String targetSuffix) {
        if (baseFileName == null || baseFileName.isEmpty()) {
            return false;
        }

        // 查找实际存在的文件（可能是正常格式、.pend 或 .fail）
        File actualFile = findActualWorkFile(baseFileName);
        if (actualFile == null || !actualFile.exists()) {
            log.debug("文件不存在，跳过重命名: {}", baseFileName);
            return false;
        }

        File targetFile = new File(FilePathConfig.WorksPath, baseFileName + targetSuffix);

        // 如果目标文件已存在，跳过
        if (targetFile.exists()) {
            log.warn("目标文件已存在，跳过重命名: {}", actualFile.getName());
            return false;
        }

        // 如果当前文件已经是目标文件，无需重命名
        String targetName = baseFileName + targetSuffix;
        if (actualFile.getName().equals(targetName)) {
            log.debug("文件已是目标状态，无需重命名: {}", targetName);
            return false;
        }

        boolean renamed = actualFile.renameTo(targetFile);
        if (renamed) {
            log.info("文件重命名成功: {} -> {}", actualFile.getName(), targetName);
        } else {
            log.error("文件重命名失败: {}", actualFile.getName());
        }
        return renamed;
    }

    /**
     * 管理员批量删除作品（逻辑删除）
     *
     * @param workIds 作品 ID 列表
     * @return 批量操作结果（包含总数、成功数、失败ID列表）
     * @author PlayerEG
     */
    @Override
    public top.playereg.pix_vision.pojo.adminPojo.AdminBatchOperateWorkResult adminBatchDeleteWorks(
        List<Integer> workIds
    ) {
        if (workIds == null || workIds.isEmpty()) {
            return new top.playereg.pix_vision.pojo.adminPojo.AdminBatchOperateWorkResult(0, 0, new java.util.ArrayList<>());
        }

        // 1. 先查询所有作品信息，获取文件名
        List<Works> worksList = worksMapper.selectBatchIds(workIds);
        if (worksList == null || worksList.isEmpty()) {
            log.warn("未找到对应的作品，作品 ID 列表: {}", workIds);
            return new top.playereg.pix_vision.pojo.adminPojo.AdminBatchOperateWorkResult(workIds.size(), 0, workIds);
        }

        // 2. 过滤出未删除的作品
        List<Works> validWorks = worksList.stream()
            .filter(work -> !work.getIs_delete())
            .toList();

        if (validWorks.isEmpty()) {
            log.warn("没有可删除的作品（可能已全部删除），作品 ID 列表: {}", workIds);
            return new top.playereg.pix_vision.pojo.adminPojo.AdminBatchOperateWorkResult(workIds.size(), 0, workIds);
        }

        // 3. 将原图和封面文件后缀名改为 .del（处理各种状态的文件）
        int renamedCount = 0;
        for (Works work : validWorks) {
            String imgFileName = work.getImg_url();
            if (imgFileName != null && !imgFileName.isEmpty()) {
                // 重命名原图文件
                if (renameWorkFile(imgFileName, ".del")) {
                    renamedCount++;
                }

                // 同步重命名封面文件（封面可能不存在，静默忽略）
                String thumbFileName = thumbFileName(imgFileName);
                if (thumbFileName != null && renameWorkFile(thumbFileName, ".del")) {
                    renamedCount++;
                }
            }
        }

        log.info("文件重命名完成，成功: {}/{}", renamedCount, validWorks.size());

        // 4. 提取需要删除的作品 ID 列表
        List<Integer> validWorkIds = validWorks.stream()
            .map(Works::getWork_id)
            .toList();

        // 5. 执行数据库逻辑删除（管理员权限，不验证 user_id）
        int affectedRows = worksMapper.adminBatchDeleteWorks(validWorkIds);

        int successCount = affectedRows > 0 ? affectedRows : 0;
        List<Integer> failedWorkIds = new java.util.ArrayList<>();

        // 计算失败的 ID
        if (affectedRows < validWorkIds.size()) {
            // 找出哪些 ID 没有被更新（简化处理：假设全部成功或全部失败）
            failedWorkIds.addAll(validWorkIds);
        }

        log.info("批量删除作品完成 - 总数: {}, 成功: {}, 失败: {}, 文件重命名: {}",
            workIds.size(), successCount, failedWorkIds.size(), renamedCount);

        return new top.playereg.pix_vision.pojo.adminPojo.AdminBatchOperateWorkResult(
            workIds.size(), successCount, failedWorkIds
        );
    }

    /**
     * 分页查询用户自己的作品列表（只过滤已删除，不过滤审核状态）
     *
     * @param page           分页对象
     * @param userId         用户 ID
     * @param approvalStatus 审核状态（可选，10-正常、20-待审核、30-未过审）
     * @return 分页结果
     * @author PlayerEG
     */
    @Override
    public IPage<Works> getMyWorks(Page<Works> page, Integer userId, Integer approvalStatus) {
        if (userId == null || userId <= 0) {
            log.warn("用户 ID 无效: {}", userId);
            return new Page<>();
        }

        log.info("开始查询用户作品，用户 ID: {}, 审核状态: {}", userId, approvalStatus);

        // 调用 Mapper 层查询
        IPage<Works> result = worksMapper.selectMyWorks(page, userId, approvalStatus);

        log.info("查询用户作品完成，用户 ID: {}, 总数: {}, 当前页: {}",
            userId, result.getTotal(), result.getCurrent());

        return result;
    }

    /**
     * 管理员分页查询作品列表（支持多条件过滤）
     *
     * @param current 当前页码
     * @param size    每页大小
     * @param keyword 关键字（可选，模糊搜索标题）
     * @param orderBy 排序方式：'oldest' - 按最早发布，其他值或 null - 按最新发布（默认）
     * @return 分页结果
     * @author PlayerEG
     */
    @Override
    public IPage<Works> getAdminWorksPage(Long current, Long size, String keyword, String orderBy) {
        // 参数校验与默认值处理
        current = PageUtils.getValidCurrent(current);
        size = PageUtils.getValidSize(size);

        log.info("开始管理员分页查询作品，页码: {}, 每页大小: {}, 关键字: {}, 排序: {}",
            current, size, keyword, orderBy);

        // 创建分页对象
        Page<Works> page = new Page<>(current, size);

        // 调用 Mapper 层查询
        IPage<Works> result = worksMapper.adminSelectWorks(page, keyword, orderBy);

        // 为列表中的每个作品填充最新的浏览量（优先 Redis，其次回源）
        if (result != null && !result.getRecords().isEmpty()) {
            for (Works work : result.getRecords()) {
                try {
                    String key = VIEW_COUNT_KEY_PREFIX + work.getWork_id();
                    Object viewCountObj = redisTemplate.opsForValue().get(key);
                    if (viewCountObj != null) {
                        work.setView_count(((Number) viewCountObj).intValue());
                    } else {
                        // Redis 缺失，触发回源并缓存
                        int dbCount = worksMapper.selectTotalViewCountByWorkId(work.getWork_id());
                        work.setView_count(dbCount);
                        redisTemplate.opsForValue().set(key, dbCount, 5, java.util.concurrent.TimeUnit.MINUTES);
                    }
                } catch (Exception e) {
                    log.warn("管理员分页查询时填充浏览量失败，作品 ID: {}", work.getWork_id());
                }
            }
        }

        log.info("管理员分页查询作品完成，总数: {}, 当前页: {}, 每页大小: {}",
            result.getTotal(), result.getCurrent(), result.getSize());

        return result;
    }

    /**
     * 查询用户统计数据（作品数、点赞总数、收藏总数、查看总数）
     *
     * @param userId 用户 ID
     * @return 包含 workCount, totalLikes, totalStars, totalViews 的 Map
     * @author PlayerEG
     */
    @Override
    public java.util.Map<String, Object> getUserStats(Integer userId) {
        if (userId == null || userId <= 0) {
            log.warn("查询用户统计数据失败 - 无效的用户 ID: {}", userId);
            return new java.util.HashMap<>();
        }

        log.info("开始查询用户统计数据 - 用户 ID: {}", userId);

        // 调用 Mapper 层查询统计数据
        java.util.Map<String, Object> stats = worksMapper.selectUserStats(userId);

        if (stats == null) {
            log.warn("用户统计数据为空 - 用户 ID: {}", userId);
            stats = new java.util.HashMap<>();
            stats.put("work_count", 0);
            stats.put("total_likes", 0L);
            stats.put("total_stars", 0L);
            stats.put("total_views", 0L);
        }

        log.info("查询用户统计数据成功 - 用户 ID: {}, 作品数: {}, 点赞总数: {}, 收藏总数: {}, 查看总数: {}",
            userId, stats.get("work_count"), stats.get("total_likes"),
            stats.get("total_stars"), stats.get("total_views"));

        return stats;
    }

    /**
     * 批量更新作品标题（管理员接口）
     *
     * @param workIds   作品 ID 列表
     * @param workTitle 作品标题
     * @return 批量操作结果（包含总数、成功数、失败ID列表）
     * @author blue_sky_ks
     */
    @Override
    public top.playereg.pix_vision.pojo.adminPojo.AdminBatchOperateWorkResult batchUpdateWorkTitle(
        List<Integer> workIds,
        String workTitle,
        Integer userId
    ) {
        if (workIds == null || workIds.isEmpty()) {
            return new top.playereg.pix_vision.pojo.adminPojo.AdminBatchOperateWorkResult(0, 0, new java.util.ArrayList<>());
        }

        if (workTitle == null || workTitle.trim().isEmpty()) {
            log.warn("作品标题为空");
            return new top.playereg.pix_vision.pojo.adminPojo.AdminBatchOperateWorkResult(workIds.size(), 0, workIds);
        }

        // 验证标题长度（最多 16 个中文字符）
        String trimmedTitle = workTitle.trim();
        if (trimmedTitle.length() > 16) {
            log.warn("作品标题长度不符合要求，标题长度: {}", trimmedTitle.length());
            return new top.playereg.pix_vision.pojo.adminPojo.AdminBatchOperateWorkResult(workIds.size(), 0, workIds);
        }

        int totalCount = workIds.size();

        // 1. 先查询所有作品信息，验证是否存在且未删除
        List<Works> worksList = worksMapper.selectBatchIds(workIds);

        // 构建查询到的作品ID集合
        java.util.Set<Integer> foundWorkIds = new java.util.HashSet<>();
        if (worksList != null && !worksList.isEmpty()) {
            for (Works work : worksList) {
                foundWorkIds.add(work.getWork_id());
            }
        }

        // 2. 找出未找到的作品ID（不存在的作品）
        List<Integer> notFoundWorkIds = new java.util.ArrayList<>();
        for (Integer workId : workIds) {
            if (!foundWorkIds.contains(workId)) {
                notFoundWorkIds.add(workId);
            }
        }

        // 3. 过滤出未删除的作品
        List<Works> validWorks = worksList != null ? worksList.stream()
            .filter(work -> !work.getIs_delete())
            .toList() : new java.util.ArrayList<>();

        // 4. 找出已删除的作品ID
        List<Integer> deletedWorkIds = new java.util.ArrayList<>();
        if (worksList != null) {
            for (Works work : worksList) {
                if (work.getIs_delete()) {
                    deletedWorkIds.add(work.getWork_id());
                }
            }
        }

        // 合并所有失败的ID（不存在的 + 已删除的）
        List<Integer> failedWorkIds = new java.util.ArrayList<>();
        failedWorkIds.addAll(notFoundWorkIds);
        failedWorkIds.addAll(deletedWorkIds);

        if (validWorks.isEmpty()) {
            log.warn("没有可更新的作品（可能全部不存在或已删除），作品 ID 列表: {}", workIds);
            return new top.playereg.pix_vision.pojo.adminPojo.AdminBatchOperateWorkResult(totalCount, 0, failedWorkIds);
        }

        // 5. 使用自定义 SQL 批量更新作品标题
        List<Integer> validWorkIds = validWorks.stream()
            .map(Works::getWork_id)
            .toList();
        int affectedRows = worksMapper.adminBatchUpdateWorkTitle(validWorkIds, trimmedTitle, userId);

        int successCount = affectedRows > 0 ? affectedRows : 0;

        // 如果数据库更新的影响行数小于有效作品数量，说明有部分更新失败
        if (affectedRows < validWorkIds.size()) {
            // 简化处理：将所有有效作品ID都标记为失败（因为无法精确知道哪些失败了）
            failedWorkIds.addAll(validWorkIds);
            successCount = 0;
        }

        log.info("批量更新作品标题完成 - 总数: {}, 成功: {}, 失败: {}, 新标题: {}, 操作者 ID: {}",
            totalCount, successCount, failedWorkIds.size(), trimmedTitle, userId);

        if (!notFoundWorkIds.isEmpty()) {
            log.warn("以下作品ID不存在: {}", notFoundWorkIds);
        }
        if (!deletedWorkIds.isEmpty()) {
            log.warn("以下作品ID已删除: {}", deletedWorkIds);
        }

        return new top.playereg.pix_vision.pojo.adminPojo.AdminBatchOperateWorkResult(
            totalCount, successCount, failedWorkIds
        );
    }

    /**
     * 获取最后一个公开作品的 work_id（仅统计未删除且审核通过的作品）
     * <p>
     * 优先从 Redis 读取，缓存 TTL 为 1 分钟，减少数据库压力。
     *
     * @return 最大 work_id，如果不存在则返回 0
     * @author PlayerEG
     */
    @Override
    public Integer getLastWorkId() {
        // 优先从 Redis 读取缓存
        Object cached = redisTemplate.opsForValue().get(LAST_WORK_ID_CACHE_KEY);
        if (cached != null) {
            return ((Number) cached).intValue();
        }

        // 缓存未命中，查询数据库
        Integer lastWorkId = worksMapper.selectLastWorkId();
        int result = lastWorkId != null ? lastWorkId : 0;

        // 写入 Redis 缓存，TTL 1 分钟
        redisTemplate.opsForValue().set(
            LAST_WORK_ID_CACHE_KEY, result,
            LAST_WORK_ID_CACHE_TTL_MINUTES, java.util.concurrent.TimeUnit.MINUTES
        );

        return result;
    }
}
