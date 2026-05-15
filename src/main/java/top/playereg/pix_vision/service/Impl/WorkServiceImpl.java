package top.playereg.pix_vision.service.Impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.playereg.pix_vision.config.FilePathConfig;
import top.playereg.pix_vision.mapper.HistoryMapper;
import top.playereg.pix_vision.mapper.SeriesMapper;
import top.playereg.pix_vision.mapper.WorksMapper;
import top.playereg.pix_vision.pojo.History;
import top.playereg.pix_vision.pojo.Series;
import top.playereg.pix_vision.pojo.Works;
import top.playereg.pix_vision.service.WorkService;
import top.playereg.pix_vision.util.ImageUtils;
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

    // 允许上传的图片扩展名白名单（仅支持 JPG、JPEG、PNG）
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(
        "jpg", "jpeg", "png"
    );
    @Autowired
    private SeriesMapper seriesMapper;

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
        return worksMapper.selectHomepageWorks(page, workTitle, userId, seriesId, isOriginal);
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
        works.setApproval_status(20); // 新发布作品默认为待审核状态
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

        log.info("查询作品成功，作品 ID: {}", workId);
        return work;
    }

    /**
     * 增加作品浏览次数
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

        int affectedRows = worksMapper.incrementViewCount(workId);

        if (affectedRows > 0) {
            log.debug("作品浏览次数 +1，作品 ID: {}", workId);
            return true;
        } else {
            log.warn("增加浏览次数失败，作品可能不存在或已删除，作品 ID: {}", workId);
            return false;
        }
    }

    /**
     * 添加用户访问历史记录
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
            } else {
                log.warn("添加历史记录失败（影响行数为 0），用户 ID: {}, 作品 ID: {}", userId, workId);
            }
        } catch (Exception e) {
            log.error("添加历史记录异常，用户 ID: {}, 作品 ID: {}, 错误: {}", userId, workId, e.getMessage());
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

            // 5. 生成唯一文件名（保留原始扩展名）
            String uniqueFileName = UUID.randomUUID().toString().replace("-", "") + "." + extension;
            String savePath = Paths.get(FilePathConfig.WorksPath, uniqueFileName).toString();

            // 6. 保存新文件
            File saveFile = new File(savePath);
            File parentDir = saveFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }

            cn.hutool.core.io.FileUtil.writeBytes(fileBytes, saveFile);
            log.info("新作品图片保存成功: {}", savePath);

            // 7. 删除旧图片文件
            if (oldImgUrl != null && !oldImgUrl.isEmpty()) {
                deleteOldImageFile(oldImgUrl);
            }

            // 8. 设置新图片 URL
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

        // 执行更新（使用 newImgUrl、finalSeriesId 和 shouldUpdateSeriesId）
        int affectedRows = worksMapper.updateWorkInfo(workId, userId, workTitle, newImgUrl, finalSeriesId, shouldUpdateSeriesId, finalIsOriginal, finalOutUrl);

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
     * 删除旧图片文件（重命名为 .del 后缀）
     *
     * @param imgFileName 图片文件名
     * @author PlayerEG
     */
    private void deleteOldImageFile(String imgFileName) {
        if (imgFileName == null || imgFileName.isEmpty()) {
            return;
        }

        File originalFile = new File(FilePathConfig.WorksPath, imgFileName);
        File deletedFile = new File(FilePathConfig.WorksPath, imgFileName + ".del");

        if (originalFile.exists() && !deletedFile.exists()) {
            boolean renamed = originalFile.renameTo(deletedFile);
            if (renamed) {
                log.info("旧图片文件重命名为 .del 成功: {} -> {}", imgFileName, imgFileName + ".del");
            } else {
                log.error("旧图片文件重命名失败: {}", imgFileName);
            }
        } else if (!originalFile.exists()) {
            log.warn("旧图片文件不存在，跳过删除: {}", imgFileName);
        } else {
            log.warn("旧图片文件已标记为删除，跳过重命名: {}", imgFileName);
        }
    }

    /**
     * 批量更新作品审核状态（管理员接口）
     *
     * @param workIds 作品 ID 列表
     * @param approvalStatus 审核状态：10 - 正常、20 - 待审核、30 - 未过审
     * @return 批量操作结果（包含总数、成功数、失败ID列表）
     * @author PlayerEG
     */
    @Override
    public top.playereg.pix_vision.pojo.adminPojo.AdminBatchOperateWorkResult batchUpdateApprovalStatus(
        List<Integer> workIds,
        Integer approvalStatus
    ) {
        if (workIds == null || workIds.isEmpty()) {
            return new top.playereg.pix_vision.pojo.adminPojo.AdminBatchOperateWorkResult(0, 0, new java.util.ArrayList<>());
        }

        int totalCount = workIds.size();

        // 使用自定义 SQL 批量更新审核状态
        int affectedRows = worksMapper.adminBatchUpdateApprovalStatus(workIds, approvalStatus);

        int successCount = affectedRows > 0 ? affectedRows : 0;
        List<Integer> failedWorkIds = new java.util.ArrayList<>();

        // 计算失败的 ID（简化处理：如果影响行数小于总数，则认为全部失败）
        if (affectedRows < totalCount) {
            failedWorkIds.addAll(workIds);
        }

        log.info("批量更新作品审核状态完成 - 总数: {}, 成功: {}, 失败: {}, 新状态: {}",
            totalCount, successCount, failedWorkIds.size(), approvalStatus);

        return new top.playereg.pix_vision.pojo.adminPojo.AdminBatchOperateWorkResult(
            totalCount, successCount, failedWorkIds
        );
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

        // 3. 将文件后缀名改为 .del
        int renamedCount = 0;
        for (Works work : validWorks) {
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
}
