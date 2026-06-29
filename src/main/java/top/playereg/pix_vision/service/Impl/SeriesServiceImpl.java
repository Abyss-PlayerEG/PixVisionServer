package top.playereg.pix_vision.service.Impl;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.playereg.pix_vision.enums.MessageProject;
import top.playereg.pix_vision.mapper.ContentAuditRecordMapper;
import top.playereg.pix_vision.mapper.SeriesMapper;
import top.playereg.pix_vision.mapper.WorksMapper;
import top.playereg.pix_vision.pojo.VO.admin.AdminSeriesVO;
import top.playereg.pix_vision.pojo.admin.AdminBatchOperateWorkResult;
import top.playereg.pix_vision.pojo.dto.ContentAuditResult;
import top.playereg.pix_vision.pojo.dto.SeriesOperationResult;
import top.playereg.pix_vision.pojo.entity.ContentAuditRecord;
import top.playereg.pix_vision.pojo.entity.Series;
import top.playereg.pix_vision.pojo.entity.Works;
import top.playereg.pix_vision.service.ContentAuditService;
import top.playereg.pix_vision.service.MessageService;
import top.playereg.pix_vision.service.SeriesService;
import top.playereg.pix_vision.util.PixVisionLogger;

import java.io.File;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 系列服务实现类
 *
 * @author PlayerEG
 */
@Service
public class SeriesServiceImpl implements SeriesService {

    private static final PixVisionLogger log = PixVisionLogger.create(SeriesServiceImpl.class);

    private final SeriesMapper seriesMapper;

    @Autowired
    private WorksMapper worksMapper;

    @Autowired
    private ContentAuditService contentAuditService;

    @Autowired
    private ContentAuditRecordMapper contentAuditRecordMapper;

    @Autowired
    private MessageService messageService;

    public SeriesServiceImpl(SeriesMapper seriesMapper) {
        this.seriesMapper = seriesMapper;
    }

    /**
     * 新增作品系列
     *
     * @param userId      用户 ID（从 Token 中获取）
     * @param seriesTitle 系列标题
     * @param aboutText   系列描述文本
     * @return 系列操作结果，包含操作是否成功、AI审核状态和审核原因
     */
    @Override
    public SeriesOperationResult addSeries(Integer userId, String seriesTitle, String aboutText) {
        log.info("开始新增系列，用户 ID: {}, 系列标题: {}", userId, seriesTitle);

        // 调用 AI 审核服务对系列标题和描述进行内容安全审核
        String auditText = buildSeriesAuditText(seriesTitle, aboutText);
        ContentAuditResult auditResult = contentAuditService.auditContent(auditText);
        Integer approvalStatus = resolveApprovalStatus(auditResult);
        String auditReason = auditResult != null ? auditResult.getReason() : null;

        // 创建系列对象
        Series series = new Series();
        series.setUser_id(userId);
        series.setSeries_title(seriesTitle);
        series.setAbout_text(aboutText);
        series.setApproval_status(approvalStatus);
        series.setIs_delete(Boolean.FALSE);

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
            log.info("系列新增成功，系列 ID: {}, 用户 ID: {}, 审核状态: {}", series.getSeries_id(), userId, approvalStatus);

            // 将 AI 审核结果写入审核记录表
            if (auditResult != null) {
                ContentAuditRecord auditRecord = new ContentAuditRecord();
                auditRecord.setContent_type(300);
                auditRecord.setContent_id(series.getSeries_id());
                auditRecord.setApproval_status(approvalStatus);
                auditRecord.setAudit_reason(auditResult.getReason());
                auditRecord.setInsult_words(auditResult.getInsult_words() != null
                    ? JSON.toJSONString(auditResult.getInsult_words()) : null);
                String seriesOriginalContent = seriesTitle;
                if (aboutText != null && !aboutText.trim().isEmpty()) {
                    seriesOriginalContent += "|" + aboutText.trim();
                }
                auditRecord.setOriginal_content(seriesOriginalContent);
                auditRecord.setCreate_time(new Timestamp(System.currentTimeMillis()));
                contentAuditRecordMapper.insertRecord(auditRecord);
                log.info("系列审核记录已保存 - 系列ID: {}, 审核状态: {}", series.getSeries_id(), approvalStatus);
            }

            return new SeriesOperationResult(true, approvalStatus, auditReason);
        } else {
            log.error("系列新增失败，用户 ID: {}", userId);
            return new SeriesOperationResult(false, null, null);
        }
    }

    /**
     * 分页查询作品系列列表（支持按用户筛选和关键词搜索）
     *
     * @param userId  用户 ID（可选，为 null 时查询所有用户的系列）
     * @param current 当前页码
     * @param size    每页数量
     * @param keyword 搜索关键词（可选，同时匹配标题和描述，标题匹配优先排序）
     * @return 分页作品系列列表
     */
    @Override
    public IPage<Series> getSeriesByUserId(Integer userId, Integer current, Integer size, String keyword) {
        log.debug("分页查询作品系列 - 用户 ID: {}, 页码: {}, 每页数量: {}, 关键词: {}", userId, current, size, keyword);

        // 创建分页对象
        Page<Series> page = new Page<>(current != null ? current : 1, size != null ? size : 10);

        // 调用 Mapper 分页查询
        IPage<Series> result = seriesMapper.selectSeriesByUserId(page, userId, keyword);

        if (result != null) {
            log.info("分页查询成功 - 用户 ID: {}, 总记录数: {}, 当前页记录数: {}",
                userId, result.getTotal(), result.getRecords().size());
        } else {
            log.warn("分页查询失败 - 用户 ID: {}", userId);
        }

        return result;
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
            return Boolean.FALSE;
        }

        if (userId == null || userId <= 0) {
            log.warn("用户 ID 无效: {}", userId);
            return Boolean.FALSE;
        }

        if (deleteWorks == null) {
            log.warn("删除作品标志为空，默认为 false");
            deleteWorks = Boolean.FALSE;
        }

        // 1. 验证系列是否存在且属于当前用户
        Series series = seriesMapper.selectSeriesById(seriesId);
        if (series == null || series.getIs_delete()) {
            log.warn("系列不存在或已删除，系列 ID: {}", seriesId);
            return Boolean.FALSE;
        }

        if (!series.getUser_id().equals(userId)) {
            log.warn("无权删除该系列，系列 ID: {}, 用户 ID: {}", seriesId, userId);
            return Boolean.FALSE;
        }

        // 2. 处理系列内的作品
        if (deleteWorks) {
            // 选择删除作品：查询系列下所有作品 ID，然后批量删除
            List<Integer> workIds = worksMapper.selectWorkIdsBySeriesId(seriesId, userId);
            if (workIds != null && !workIds.isEmpty()) {
                log.info("系列包含 {} 个作品，将一并删除", Integer.valueOf(workIds.size()));

                // 调用 WorkService 的批量删除方法（会重命名文件为 .del）
                // 注意：这里需要注入 WorkService，但为了避免循环依赖，我们直接使用 WorksMapper
                // 先重命名文件
                int renamedCount = renameWorksFilesToDelete(workIds);
                log.info("作品文件重命名完成，成功: {}/{}", Integer.valueOf(renamedCount), Integer.valueOf(workIds.size()));

                // 执行逻辑删除
                int deletedCount = worksMapper.batchDeleteWorks(workIds, userId);
                log.info("作品删除完成，删除数量: {}", Integer.valueOf(deletedCount));
            } else {
                log.info("系列下没有作品，跳过作品删除");
            }
        } else {
            // 选择保留作品：将系列下所有作品的 series_id 置空
            int clearedCount = worksMapper.clearSeriesIdBySeriesId(seriesId, userId);
            log.info("已将 {} 个作品的 series_id 置空", Integer.valueOf(clearedCount));
        }

        // 3. 逻辑删除系列
        int affectedRows = seriesMapper.deleteSeriesById(seriesId, userId);

        if (affectedRows > 0) {
            log.info("系列删除成功，系列 ID: {}, 用户 ID: {}", seriesId, userId);
            return Boolean.TRUE;
        } else {
            log.error("系列删除失败，系列 ID: {}, 用户 ID: {}", seriesId, userId);
            return Boolean.FALSE;
        }
    }

    /**
     * 更新系列信息（支持部分字段修改）
     *
     * @param seriesId    系列 ID
     * @param userId      当前用户 ID（用于权限验证）
     * @param seriesTitle 系列标题（可选，最多 16 个中文字符）
     * @param aboutText   系列描述（可选，最多 24 个中文字符）
     * @return 系列操作结果，包含操作是否成功、AI审核状态和审核原因
     */
    @Override
    public SeriesOperationResult updateSeriesInfo(Integer seriesId, Integer userId, String seriesTitle, String aboutText) {
        log.info("开始更新系列信息，系列 ID: {}, 用户 ID: {}", seriesId, userId);

        // 参数校验
        if (seriesId == null || seriesId <= 0) {
            log.warn("系列 ID 无效: {}", seriesId);
            return new SeriesOperationResult(false, null, null);
        }

        if (userId == null || userId <= 0) {
            log.warn("用户 ID 无效: {}", userId);
            return new SeriesOperationResult(false, null, null);
        }

        // 检查是否所有参数都为空
        boolean allParamsEmpty = (seriesTitle == null || seriesTitle.trim().isEmpty())
            && (aboutText == null || aboutText.trim().isEmpty());

        if (allParamsEmpty) {
            log.warn("无修改内容，系列 ID: {}, 用户 ID: {}", seriesId, userId);
            throw new IllegalArgumentException("无修改内容");
        }

        // 验证系列是否存在且属于当前用户
        Series series = seriesMapper.selectSeriesById(seriesId);
        if (series == null || series.getIs_delete()) {
            log.warn("系列不存在或已删除，系列 ID: {}", seriesId);
            throw new IllegalArgumentException("系列不存在或已删除");
        }

        if (!series.getUser_id().equals(userId)) {
            log.warn("无权修改该系列，系列 ID: {}, 用户 ID: {}", seriesId, userId);
            throw new SecurityException("无权修改该系列");
        }

        // 如果提供了新的标题，需要验证长度和唯一性
        String finalSeriesTitle = null;
        if (seriesTitle != null && !seriesTitle.trim().isEmpty()) {
            String trimmedTitle = seriesTitle.trim();

            // 验证标题长度
            if (trimmedTitle.length() > 16) {
                log.warn("系列标题长度不符合要求，系列 ID: {}, 标题长度: {}", seriesId, Integer.valueOf(trimmedTitle.length()));
                throw new IllegalArgumentException("系列标题长度不能超过 16 个字符");
            }

            // 检查标题是否与当前标题不同
            if (!trimmedTitle.equals(series.getSeries_title())) {
                finalSeriesTitle = trimmedTitle;
            }
        }

        // 如果提供了新的描述，需要验证长度
        String finalAboutText = null;
        if (aboutText != null && !aboutText.trim().isEmpty()) {
            String trimmedText = aboutText.trim();

            // 验证描述长度
            if (trimmedText.length() > 24) {
                log.warn("系列描述长度不符合要求，系列 ID: {}, 描述长度: {}", seriesId, Integer.valueOf(trimmedText.length()));
                throw new IllegalArgumentException("系列描述长度不能超过 24 个字符");
            }

            // 检查描述是否与当前描述不同
            if (!trimmedText.equals(series.getAbout_text())) {
                finalAboutText = trimmedText;
            }
        }

        // 再次检查是否有实际修改
        if (finalSeriesTitle == null && finalAboutText == null) {
            log.warn("无修改内容（内容与当前相同），系列 ID: {}, 用户 ID: {}", seriesId, userId);
            throw new IllegalArgumentException("无修改内容");
        }

        // 调用 AI 审核服务对更新后的系列内容进行审核
        String auditText = buildSeriesAuditText(
            finalSeriesTitle != null ? finalSeriesTitle : series.getSeries_title(),
            finalAboutText != null ? finalAboutText : series.getAbout_text()
        );
        ContentAuditResult auditResult = contentAuditService.auditContent(auditText);
        Integer approvalStatus = resolveApprovalStatus(auditResult);
        String auditReason = auditResult != null ? auditResult.getReason() : null;

        // 执行更新
        int affectedRows = seriesMapper.updateSeriesInfo(seriesId, userId, finalSeriesTitle, finalAboutText, approvalStatus);

        if (affectedRows > 0) {
            log.info("系列信息更新成功，系列 ID: {}, 用户 ID: {}, 审核状态: {}", seriesId, userId, approvalStatus);

            // 将 AI 审核结果写入审核记录表
            if (auditResult != null) {
                ContentAuditRecord auditRecord = new ContentAuditRecord();
                auditRecord.setContent_type(300);
                auditRecord.setContent_id(seriesId);
                auditRecord.setApproval_status(approvalStatus);
                auditRecord.setAudit_reason(auditResult.getReason());
                auditRecord.setInsult_words(auditResult.getInsult_words() != null
                    ? JSON.toJSONString(auditResult.getInsult_words()) : null);
                String seriesOriginalContent = finalSeriesTitle != null ? finalSeriesTitle : series.getSeries_title();
                String effectiveAbout = finalAboutText != null ? finalAboutText : series.getAbout_text();
                if (effectiveAbout != null && !effectiveAbout.trim().isEmpty()) {
                    seriesOriginalContent += "|" + effectiveAbout.trim();
                }
                auditRecord.setOriginal_content(seriesOriginalContent);
                auditRecord.setCreate_time(new Timestamp(System.currentTimeMillis()));
                contentAuditRecordMapper.insertRecord(auditRecord);
                log.info("系列审核记录已保存 - 系列ID: {}, 审核状态: {}", seriesId, approvalStatus);
            }

            return new SeriesOperationResult(true, approvalStatus, auditReason);
        } else {
            log.error("系列信息更新失败，系列 ID: {}, 用户 ID: {}", seriesId, userId);
            return new SeriesOperationResult(false, null, null);
        }
    }

    /**
     * 批量将作品添加到指定合集
     *
     * @param seriesId 合集 ID
     * @param workIds  作品 ID 列表
     * @param userId   当前用户 ID（用于权限验证）
     * @return 添加结果
     */
    @Override
    public Boolean batchAddWorksToSeries(Integer seriesId, List<Integer> workIds, Integer userId) {
        log.info("开始批量添加作品到合集，合集 ID: {}, 用户 ID: {}, 作品数量: {}", seriesId, userId, workIds != null ? workIds.size() : 0);

        // 参数校验
        if (seriesId == null || seriesId <= 0) {
            log.warn("合集 ID 无效: {}", seriesId);
            throw new IllegalArgumentException("合集 ID 无效");
        }
        if (workIds == null || workIds.isEmpty()) {
            log.warn("作品 ID 列表为空");
            throw new IllegalArgumentException("作品 ID 列表不能为空");
        }
        if (userId == null || userId <= 0) {
            log.warn("用户 ID 无效: {}", userId);
            throw new IllegalArgumentException("用户 ID 无效");
        }

        // 验证合集是否存在且属于当前用户
        Series series = seriesMapper.selectSeriesById(seriesId);
        if (series == null || series.getIs_delete()) {
            log.warn("合集不存在或已删除，合集 ID: {}", seriesId);
            throw new IllegalArgumentException("合集不存在或已删除");
        }
        if (!series.getUser_id().equals(userId)) {
            log.warn("无权操作该合集，合集 ID: {}, 用户 ID: {}", seriesId, userId);
            throw new SecurityException("无权操作该合集");
        }

        // 查询作品信息，验证是否存在且属于当前用户
        List<Works> worksList = worksMapper.selectBatchIds(workIds);
        if (worksList == null || worksList.isEmpty()) {
            log.warn("所有作品均不存在，合集 ID: {}, 作品 ID 列表: {}", seriesId, workIds);
            throw new IllegalArgumentException("作品不存在或已删除");
        }

        // 过滤有效作品：未删除且属于当前用户
        List<Integer> validWorkIds = worksList.stream()
            .filter(w -> !w.getIs_delete() && w.getUser_id().equals(userId))
            .map(Works::getWork_id)
            .collect(Collectors.toList());

        if (validWorkIds.isEmpty()) {
            log.warn("没有有效的作品可以添加，合集 ID: {}, 用户 ID: {}", seriesId, userId);
            throw new IllegalArgumentException("没有可操作的作品，请确认作品存在且属于当前用户");
        }

        // 批量设置 series_id
        int affectedRows = worksMapper.batchSetSeriesId(validWorkIds, seriesId, userId);
        log.info("批量添加作品到合集完成，合集 ID: {}, 用户 ID: {}, 有效作品数: {}, 更新行数: {}",
            seriesId, userId, validWorkIds.size(), affectedRows);

        return affectedRows > 0;
    }

    /**
     * 批量将作品从指定合集中移除
     *
     * @param seriesId 合集 ID
     * @param workIds  作品 ID 列表
     * @param userId   当前用户 ID（用于权限验证）
     * @return 移除结果
     */
    @Override
    public Boolean batchRemoveWorksFromSeries(Integer seriesId, List<Integer> workIds, Integer userId) {
        log.info("开始批量从合集移除作品，合集 ID: {}, 用户 ID: {}, 作品数量: {}", seriesId, userId, workIds != null ? workIds.size() : 0);

        // 参数校验
        if (seriesId == null || seriesId <= 0) {
            log.warn("合集 ID 无效: {}", seriesId);
            throw new IllegalArgumentException("合集 ID 无效");
        }
        if (workIds == null || workIds.isEmpty()) {
            log.warn("作品 ID 列表为空");
            throw new IllegalArgumentException("作品 ID 列表不能为空");
        }
        if (userId == null || userId <= 0) {
            log.warn("用户 ID 无效: {}", userId);
            throw new IllegalArgumentException("用户 ID 无效");
        }

        // 验证合集是否存在且属于当前用户
        Series series = seriesMapper.selectSeriesById(seriesId);
        if (series == null || series.getIs_delete()) {
            log.warn("合集不存在或已删除，合集 ID: {}", seriesId);
            throw new IllegalArgumentException("合集不存在或已删除");
        }
        if (!series.getUser_id().equals(userId)) {
            log.warn("无权操作该合集，合集 ID: {}, 用户 ID: {}", seriesId, userId);
            throw new SecurityException("无权操作该合集");
        }

        // 查询作品信息，验证是否存在且属于当前用户
        List<Works> worksList = worksMapper.selectBatchIds(workIds);
        if (worksList == null || worksList.isEmpty()) {
            log.warn("所有作品均不存在，合集 ID: {}, 作品 ID 列表: {}", seriesId, workIds);
            throw new IllegalArgumentException("作品不存在或已删除");
        }

        // 过滤有效作品：未删除且属于当前用户
        List<Integer> validWorkIds = worksList.stream()
            .filter(w -> !w.getIs_delete() && w.getUser_id().equals(userId))
            .map(Works::getWork_id)
            .collect(Collectors.toList());

        if (validWorkIds.isEmpty()) {
            log.warn("没有有效的作品可以移除，合集 ID: {}, 用户 ID: {}", seriesId, userId);
            throw new IllegalArgumentException("没有可操作的作品，请确认作品存在且属于当前用户");
        }

        // 批量清空 series_id（SQL 层面验证 user_id、series_id 和 is_delete）
        int affectedRows = worksMapper.batchClearSeriesId(validWorkIds, seriesId, userId);
        log.info("批量从合集移除作品完成，合集 ID: {}, 用户 ID: {}, 有效作品数: {}, 实际移除行数: {}",
            seriesId, userId, validWorkIds.size(), affectedRows);

        // 如果 SQL 层面没有更新任何行，说明作品可能不属于该合集
        if (affectedRows == 0) {
            log.warn("移除操作未影响任何行，作品可能不属于该合集，合集 ID: {}, 用户 ID: {}", seriesId, userId);
            throw new IllegalArgumentException("移除失败，作品可能不属于该合集");
        }

        return true;
    }

    /**
     * 构建系列审核文本
     * <p>
     * 将标题和描述拼接为 AI 审核所需的文本格式
     * </p>
     *
     * @param seriesTitle 系列标题
     * @param aboutText   系列描述
     * @return 审核文本
     */
    private String buildSeriesAuditText(String seriesTitle, String aboutText) {
        StringBuilder sb = new StringBuilder();
        if (seriesTitle != null && !seriesTitle.trim().isEmpty()) {
            sb.append("标题：").append(seriesTitle.trim());
        }
        if (aboutText != null && !aboutText.trim().isEmpty()) {
            if (sb.length() > 0) {
                sb.append("\n");
            }
            sb.append("描述：").append(aboutText.trim());
        }
        return sb.toString();
    }

    /**
     * 根据 AI 审核结果解析审核状态
     * <p>
     * 审核结果映射规则：
     * <ul>
     *   <li>{@code normal} - 正常内容，对应审核状态 10（通过）</li>
     *   <li>{@code neutral} - 中立/存疑，对应审核状态 20（待审核）</li>
     *   <li>{@code violation} - 违规内容，对应审核状态 30（未过审）</li>
     * </ul>
     * </p>
     *
     * @param auditResult AI 审核结果
     * @return 审核状态码（10、20 或 30）
     */
    private Integer resolveApprovalStatus(ContentAuditResult auditResult) {
        Integer approvalStatus = 20; // 默认待审核
        if (auditResult != null) {
            switch (auditResult.getStatus()) {
                case "normal":
                    approvalStatus = 10;
                    break;
                case "violation":
                    approvalStatus = 30;
                    break;
                case "neutral":
                default:
                    approvalStatus = 20;
                    break;
            }
            log.info("AI 系列审核结果 - 状态: {}, 原因: {}, 命中敏感词: {}, 最终审核状态: {}",
                auditResult.getStatus(), auditResult.getReason(), auditResult.getInsult_words(), approvalStatus);
        } else {
            log.warn("AI 审核服务不可用，系列降级为待审核");
        }
        return approvalStatus;
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
        List<Works> worksList = worksMapper.selectBatchIds(workIds);
        if (worksList == null || worksList.isEmpty()) {
            return 0;
        }

        int renamedCount = 0;
        for (Works work : worksList) {
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

    /**
     * 批量更新系列审核状态（管理员操作）
     *
     * @param seriesIds       系列 ID 列表
     * @param approvalStatus  审核状态（10-正常、20-待审核、30-未过审）
     * @return 批量操作结果（包含总数、成功数、失败ID列表）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminBatchOperateWorkResult batchUpdateApprovalStatus(List<Integer> seriesIds, Integer approvalStatus, Integer userId) {
        if (seriesIds == null || seriesIds.isEmpty()) {
            return new AdminBatchOperateWorkResult(0, 0, new ArrayList<>());
        }

        int totalCount = seriesIds.size();

        // 1. 先查询所有系列信息，验证是否存在且未删除
        List<Series> seriesList = seriesMapper.selectBatchIds(seriesIds);

        // 构建查询到的系列ID集合
        java.util.Set<Integer> foundSeriesIds = new java.util.HashSet<>();
        if (seriesList != null && !seriesList.isEmpty()) {
            for (Series series : seriesList) {
                foundSeriesIds.add(series.getSeries_id());
            }
        }

        // 2. 找出未找到的系列ID（不存在的系列）
        List<Integer> notFoundSeriesIds = new ArrayList<>();
        for (Integer seriesId : seriesIds) {
            if (!foundSeriesIds.contains(seriesId)) {
                notFoundSeriesIds.add(seriesId);
            }
        }

        // 3. 过滤出未删除的系列
        List<Series> validSeries = seriesList != null ? seriesList.stream()
            .filter(series -> !series.getIs_delete())
            .toList() : new ArrayList<>();

        // 4. 找出已删除的系列ID
        List<Integer> deletedSeriesIds = new ArrayList<>();
        if (seriesList != null) {
            for (Series series : seriesList) {
                if (series.getIs_delete()) {
                    deletedSeriesIds.add(series.getSeries_id());
                }
            }
        }

        // 合并所有失败的ID（不存在的 + 已删除的）
        List<Integer> failedSeriesIds = new ArrayList<>();
        failedSeriesIds.addAll(notFoundSeriesIds);
        failedSeriesIds.addAll(deletedSeriesIds);

        if (validSeries.isEmpty()) {
            log.warn("没有可更新的系列（可能全部不存在或已删除），系列 ID 列表: {}", seriesIds);
            return new AdminBatchOperateWorkResult(totalCount, 0, failedSeriesIds);
        }

        // 5. 使用自定义 SQL 批量更新审核状态
        List<Integer> validSeriesIds = validSeries.stream()
            .map(Series::getSeries_id)
            .toList();
        int affectedRows = seriesMapper.adminBatchUpdateApprovalStatus(validSeriesIds, approvalStatus, userId);

        int successCount = affectedRows > 0 ? affectedRows : 0;

        // 如果数据库更新的影响行数小于有效系列数量，说明有部分更新失败
        if (affectedRows < validSeriesIds.size()) {
            // 简化处理：将所有有效系列ID都标记为失败（因为无法精确知道哪些失败了）
            failedSeriesIds.addAll(validSeriesIds);
            successCount = 0;
        }

        String statusName = getStatusName(approvalStatus);
        log.info("批量更新系列审核状态完成 - 总数: {}, 成功: {}, 失败: {}, 新状态: {} ({}), 操作者 ID: {}",
            totalCount, successCount, failedSeriesIds.size(), approvalStatus, statusName, userId);

        if (!notFoundSeriesIds.isEmpty()) {
            log.warn("以下系列ID不存在: {}", notFoundSeriesIds);
        }
        if (!deletedSeriesIds.isEmpty()) {
            log.warn("以下系列ID已删除: {}", deletedSeriesIds);
        }

        // 发送管理员审核通知
        if (successCount > 0 && (approvalStatus == 10 || approvalStatus == 30)) {
            for (Series series : validSeries) {
                try {
                    String statusText = approvalStatus == 10 ? "通过" : "未通过";
                    // 使用 Markdown 格式构建消息
                    String content = StrUtil.format("""
                        # 合集审核{}
                        
                        ---
                        
                        ## 你的合集已审核{}
                        
                        **合集** : {}
                        """, statusText, statusText, series.getSeries_title());
                    messageService.sendSystemNotice(
                        0,
                        series.getUser_id(),
                        content,
                        MessageProject.SERIES_AUDIT,
                        series.getSeries_id()
                    );
                } catch (Exception e) {
                    log.warn("发送合集审核通知失败 - 合集ID: {}, 错误: {}", series.getSeries_id(), e.getMessage());
                }
            }
            log.info("管理员合集审核通知已发送 - 成功数: {}, 审核状态: {}", successCount, approvalStatus);
        }

        return new AdminBatchOperateWorkResult(
            totalCount, successCount, failedSeriesIds
        );
    }

    /**
     * 获取审核状态名称
     *
     * @param approvalStatus 审核状态代码
     * @return 状态名称
     */
    private String getStatusName(Integer approvalStatus) {
        return switch (approvalStatus) {
            case 10 -> "正常";
            case 20 -> "待审核";
            case 30 -> "未过审";
            default -> "未知";
        };
    }

    /**
     * 批量删除作品合集（管理员操作）
     *
     * @param seriesIds   系列 ID 列表
     * @param deleteWorks 是否删除系列内的作品（true=删除作品，false=将作品的 series_id 置空）
     * @param userId      操作者 ID
     * @return 批量操作结果（包含总数、成功数、失败ID列表）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminBatchOperateWorkResult batchDeleteSeries(List<Integer> seriesIds, Boolean deleteWorks, Integer userId) {
        if (seriesIds == null || seriesIds.isEmpty()) {
            return new AdminBatchOperateWorkResult(0, 0, new ArrayList<>());
        }

        if (deleteWorks == null) {
            log.warn("删除作品标志为空，默认为 false");
            deleteWorks = Boolean.FALSE;
        }

        int totalCount = seriesIds.size();

        // 1. 先查询所有系列信息，验证是否存在且未删除
        List<Series> seriesList = seriesMapper.selectBatchIds(seriesIds);

        // 构建查询到的系列ID集合
        java.util.Set<Integer> foundSeriesIds = new java.util.HashSet<>();
        if (seriesList != null && !seriesList.isEmpty()) {
            for (Series series : seriesList) {
                foundSeriesIds.add(series.getSeries_id());
            }
        }

        // 2. 找出未找到的系列ID（不存在的系列）
        List<Integer> notFoundSeriesIds = new ArrayList<>();
        for (Integer seriesId : seriesIds) {
            if (!foundSeriesIds.contains(seriesId)) {
                notFoundSeriesIds.add(seriesId);
            }
        }

        // 3. 过滤出未删除的系列
        List<Series> validSeries = seriesList != null ? seriesList.stream()
            .filter(series -> !series.getIs_delete())
            .toList() : new ArrayList<>();

        // 4. 找出已删除的系列ID
        List<Integer> deletedSeriesIds = new ArrayList<>();
        if (seriesList != null) {
            for (Series series : seriesList) {
                if (series.getIs_delete()) {
                    deletedSeriesIds.add(series.getSeries_id());
                }
            }
        }

        // 合并所有失败的ID（不存在的 + 已删除的）
        List<Integer> failedSeriesIds = new ArrayList<>();
        failedSeriesIds.addAll(notFoundSeriesIds);
        failedSeriesIds.addAll(deletedSeriesIds);

        if (validSeries.isEmpty()) {
            log.warn("没有可删除的系列（可能全部不存在或已删除），系列 ID 列表: {}", seriesIds);
            return new AdminBatchOperateWorkResult(totalCount, 0, failedSeriesIds);
        }

        // 5. 逐个处理有效的系列
        int successCount = 0;
        for (Series series : validSeries) {
            Integer seriesId = series.getSeries_id();
            Integer workOwnerId = series.getUser_id();

            try {
                // 处理系列内的作品
                if (deleteWorks) {
                    // 选择删除作品：查询系列下所有作品 ID，然后批量删除
                    List<Integer> workIds = worksMapper.selectWorkIdsBySeriesId(seriesId, workOwnerId);
                    if (workIds != null && !workIds.isEmpty()) {
                        log.info("系列 {} 包含 {} 个作品，将一并删除，操作者 ID: {}", seriesId, workIds.size(), userId);

                        // 先重命名文件
                        int renamedCount = renameWorksFilesToDelete(workIds);
                        log.info("作品文件重命名完成，成功: {}/{}", renamedCount, workIds.size());

                        // 执行逻辑删除
                        int deletedCount = worksMapper.batchDeleteWorks(workIds, workOwnerId);
                        log.info("作品删除完成，删除数量: {}", deletedCount);
                    } else {
                        log.info("系列 {} 下没有作品，跳过作品删除", seriesId);
                    }
                } else {
                    // 选择保留作品：将系列下所有作品的 series_id 置空
                    int clearedCount = worksMapper.clearSeriesIdBySeriesId(seriesId, workOwnerId);
                    log.info("已将 {} 个作品的 series_id 置空", clearedCount);
                }

                // 逻辑删除系列
                int affectedRows = seriesMapper.deleteSeriesById(seriesId, workOwnerId);

                if (affectedRows > 0) {
                    successCount++;
                    log.info("系列删除成功，系列 ID: {}, 作品所有者 ID: {}, 操作者 ID: {}", seriesId, workOwnerId, userId);
                } else {
                    log.error("系列删除失败，系列 ID: {}, 作品所有者 ID: {}", seriesId, workOwnerId);
                    failedSeriesIds.add(seriesId);
                }
            } catch (Exception e) {
                log.error("处理系列 {} 时发生异常: {}", seriesId, e.getMessage(), e);
                failedSeriesIds.add(seriesId);
            }
        }

        log.info("批量删除系列完成 - 总数: {}, 成功: {}, 失败: {}, 操作者 ID: {}",
            totalCount, successCount, failedSeriesIds.size(), userId);

        if (!notFoundSeriesIds.isEmpty()) {
            log.warn("以下系列ID不存在: {}", notFoundSeriesIds);
        }
        if (!deletedSeriesIds.isEmpty()) {
            log.warn("以下系列ID已删除: {}", deletedSeriesIds);
        }

        return new AdminBatchOperateWorkResult(
            totalCount, successCount, failedSeriesIds
        );
    }

    /**
     * 批量更新系列标题和描述（管理员操作）
     *
     * @param seriesIds   系列 ID 列表
     * @param seriesTitle 系列标题（可选，最多 16 个字符）
     * @param aboutText   系列描述（可选，最多 24 个字符）
     * @param userId      操作者 ID
     * @return 批量操作结果（包含总数、成功数、失败ID列表）
     * @author blue_sky_ks
     */
    @Override
    public AdminBatchOperateWorkResult batchUpdateSeriesInfo(List<Integer> seriesIds, String seriesTitle, String aboutText, Integer userId) {
        if (seriesIds == null || seriesIds.isEmpty()) {
            return new AdminBatchOperateWorkResult(0, 0, new ArrayList<>());
        }

        // 参数校验：至少需要提供一个字段
        boolean hasTitle = seriesTitle != null && !seriesTitle.trim().isEmpty();
        boolean hasDescription = aboutText != null && !aboutText.trim().isEmpty();

        if (!hasTitle && !hasDescription) {
            log.warn("系列标题和描述不能同时为空");
            return new AdminBatchOperateWorkResult(seriesIds.size(), 0, seriesIds);
        }

        // 验证标题长度（最多 16 个字符）
        String finalTitle = null;
        if (hasTitle) {
            String trimmedTitle = seriesTitle.trim();
            if (trimmedTitle.length() > 16) {
                log.warn("系列标题长度不符合要求，标题长度: {}", trimmedTitle.length());
                return new AdminBatchOperateWorkResult(seriesIds.size(), 0, seriesIds);
            }
            finalTitle = trimmedTitle;
        }

        // 验证描述长度（最多 24 个字符）
        String finalDescription = null;
        if (hasDescription) {
            String trimmedDescription = aboutText.trim();
            if (trimmedDescription.length() > 24) {
                log.warn("系列描述长度不符合要求，描述长度: {}", trimmedDescription.length());
                return new AdminBatchOperateWorkResult(seriesIds.size(), 0, seriesIds);
            }
            finalDescription = trimmedDescription;
        }

        int totalCount = seriesIds.size();

        // 1. 先查询所有系列信息，验证是否存在且未删除
        List<Series> seriesList = seriesMapper.selectBatchIds(seriesIds);

        // 构建查询到的系列ID集合
        java.util.Set<Integer> foundSeriesIds = new java.util.HashSet<>();
        if (seriesList != null && !seriesList.isEmpty()) {
            for (Series series : seriesList) {
                foundSeriesIds.add(series.getSeries_id());
            }
        }

        // 2. 找出未找到的系列ID（不存在的系列）
        List<Integer> notFoundSeriesIds = new ArrayList<>();
        for (Integer seriesId : seriesIds) {
            if (!foundSeriesIds.contains(seriesId)) {
                notFoundSeriesIds.add(seriesId);
            }
        }

        // 3. 过滤出未删除的系列
        List<Series> validSeries = seriesList != null ? seriesList.stream()
            .filter(series -> !series.getIs_delete())
            .toList() : new ArrayList<>();

        // 4. 找出已删除的系列ID
        List<Integer> deletedSeriesIds = new ArrayList<>();
        if (seriesList != null) {
            for (Series series : seriesList) {
                if (series.getIs_delete()) {
                    deletedSeriesIds.add(series.getSeries_id());
                }
            }
        }

        // 合并所有失败的ID（不存在的 + 已删除的）
        List<Integer> failedSeriesIds = new ArrayList<>();
        failedSeriesIds.addAll(notFoundSeriesIds);
        failedSeriesIds.addAll(deletedSeriesIds);

        if (validSeries.isEmpty()) {
            log.warn("没有可更新的系列（可能全部不存在或已删除），系列 ID 列表: {}", seriesIds);
            return new AdminBatchOperateWorkResult(totalCount, 0, failedSeriesIds);
        }

        // 5. 使用自定义 SQL 批量更新系列信息
        List<Integer> validSeriesIds = validSeries.stream()
            .map(Series::getSeries_id)
            .toList();
        int affectedRows = seriesMapper.adminBatchUpdateSeriesInfo(validSeriesIds, finalTitle, finalDescription, userId);

        int successCount = affectedRows > 0 ? affectedRows : 0;

        // 如果数据库更新的影响行数小于有效系列数量，说明有部分更新失败
        if (affectedRows < validSeriesIds.size()) {
            // 简化处理：将所有有效系列ID都标记为失败（因为无法精确知道哪些失败了）
            failedSeriesIds.addAll(validSeriesIds);
            successCount = 0;
        }

        log.info("批量更新系列信息完成 - 总数: {}, 成功: {}, 失败: {}, 操作者 ID: {}",
            totalCount, successCount, failedSeriesIds.size(), userId);

        if (!notFoundSeriesIds.isEmpty()) {
            log.warn("以下系列ID不存在: {}", notFoundSeriesIds);
        }
        if (!deletedSeriesIds.isEmpty()) {
            log.warn("以下系列ID已删除: {}", deletedSeriesIds);
        }

        return new AdminBatchOperateWorkResult(
            totalCount, successCount, failedSeriesIds
        );
    }

    /**
     * 管理员分页查询作品合集（支持多条件筛选和排序）
     *
     * @param current        当前页码
     * @param size           每页数量
     * @param keyword        搜索关键词（可选，同时匹配标题和描述，标题匹配优先排序）
     * @param approvalStatus 审核状态（可选，10-正常、20-待审核、30-未过审）
     * @param isDelete       是否删除（可选，true-已删除、false-未删除）
     * @param userId         用户 ID（可选）
     * @param orderBy        排序方式（可选，'oldest'-按最早创建，其他值-按最新创建）
     * @return 分页作品合集列表
     * @author blue_sky_ks
     */
    @Override
    public IPage<AdminSeriesVO> getAdminSeriesPage(Long current, Long size, String keyword, Integer approvalStatus, Boolean is_delete, Integer userId, String orderBy) {
        log.debug("管理员分页查询作品合集 - 页码: {}, 每页: {}, 关键词: {}, 审核状态: {}, 是否删除: {}, 用户ID: {}, 排序: {}",
            current, size, keyword, approvalStatus, is_delete, userId, orderBy);

        // 创建分页对象
        Page<AdminSeriesVO> page = new Page<>(
            current != null ? current : 1,
            size != null ? size : 10
        );

        // 调用 Mapper 分页查询（已联表查username）
        IPage<AdminSeriesVO> result = seriesMapper.selectAdminSeriesPage(page, keyword, approvalStatus, is_delete, userId, orderBy);

        // 批量查询审核记录
        if (result != null && !result.getRecords().isEmpty()) {
            List<Integer> seriesIds = result.getRecords().stream()
                .map(AdminSeriesVO::getSeries_id)
                .collect(Collectors.toList());
            List<ContentAuditRecord> auditRecords = contentAuditRecordMapper
                .selectLatestByContentIds(300, seriesIds);
            if (auditRecords != null) {
                Map<Integer, ContentAuditRecord> auditMap = auditRecords.stream()
                    .collect(Collectors.toMap(ContentAuditRecord::getContent_id, r -> r, (a, b) -> a));
                result.getRecords().forEach(vo -> {
                    ContentAuditRecord audit = auditMap.get(vo.getSeries_id());
                    if (audit != null) {
                        vo.setAudit_reason(audit.getAudit_reason());
                        vo.setInsult_words(audit.getInsult_words());
                    }
                });
            }
        }

        if (result != null) {
            log.info("管理员分页查询成功 - 总记录数: {}, 当前页记录数: {}",
                result.getTotal(), result.getRecords().size());
        } else {
            log.warn("管理员分页查询返回空结果");
        }

        return result;
    }
}
