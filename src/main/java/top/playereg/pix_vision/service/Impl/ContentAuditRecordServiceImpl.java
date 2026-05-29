package top.playereg.pix_vision.service.Impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.playereg.pix_vision.mapper.*;
import top.playereg.pix_vision.pojo.VO.admin.AdminAuditRecordVO;
import top.playereg.pix_vision.pojo.entity.Comments;
import top.playereg.pix_vision.pojo.entity.ContentAuditRecord;
import top.playereg.pix_vision.pojo.entity.Series;
import top.playereg.pix_vision.pojo.entity.Works;
import top.playereg.pix_vision.pojo.entity.user.UserDataChangeLock;
import top.playereg.pix_vision.service.ContentAuditRecordService;
import top.playereg.pix_vision.util.PixVisionLogger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 审核记录查询服务实现
 *
 * @author PlayerEG
 */
@Service
public class ContentAuditRecordServiceImpl implements ContentAuditRecordService {
    private static final PixVisionLogger log = PixVisionLogger.create(ContentAuditRecordServiceImpl.class);

    @Autowired
    private ContentAuditRecordMapper contentAuditRecordMapper;

    @Autowired
    private WorksMapper worksMapper;

    @Autowired
    private CommentsMapper commentsMapper;

    @Autowired
    private SeriesMapper seriesMapper;

    @Autowired
    private UserDataChangeLockMapper userDataChangeLockMapper;

    @Override
    public IPage<AdminAuditRecordVO> getAuditRecordsPage(Long current, Long size,
                                                          Integer contentType, Integer approvalStatus,
                                                          String keyword, String orderBy) {
        // 校验并转换排序方向，防止 SQL 注入
        String sortOrder = "DESC";
        if ("oldest".equals(orderBy)) {
            sortOrder = "ASC";
        }

        Page<ContentAuditRecord> page = new Page<>(current, size);
        IPage<ContentAuditRecord> result = contentAuditRecordMapper.selectAuditRecordsPage(
            page, contentType, approvalStatus, keyword, sortOrder
        );

        // 批量查询原始内容
        Map<String, String> originalContentMap = new HashMap<>();
        if (result.getRecords() != null && !result.getRecords().isEmpty()) {
            // 按 content_type 分组收集 content_id
            Map<Integer, List<Integer>> idsByType = new HashMap<>();
            for (ContentAuditRecord record : result.getRecords()) {
                idsByType.computeIfAbsent(record.getContent_type(), k -> new ArrayList<>())
                    .add(record.getContent_id());
            }

            // 按类型批量查询原始内容
            for (Map.Entry<Integer, List<Integer>> entry : idsByType.entrySet()) {
                Integer ct = entry.getKey();
                List<Integer> ids = entry.getValue();
                switch (ct) {
                    case 100: {
                        List<Works> works = worksMapper.selectBatchIds(ids);
                        for (Works w : works) {
                            originalContentMap.put("100_" + w.getWork_id(), w.getWork_title());
                        }
                        break;
                    }
                    case 200: {
                        List<Comments> comments = commentsMapper.selectBatchIds(ids);
                        for (Comments c : comments) {
                            originalContentMap.put("200_" + c.getComment_id(), c.getComment_text());
                        }
                        break;
                    }
                    case 300: {
                        List<Series> seriesList = seriesMapper.selectBatchIds(ids);
                        for (Series s : seriesList) {
                            String content = s.getSeries_title();
                            if (s.getAbout_text() != null && !s.getAbout_text().isEmpty()) {
                                content += "|" + s.getAbout_text();
                            }
                            originalContentMap.put("300_" + s.getSeries_id(), content);
                        }
                        break;
                    }
                    case 400: {
                        List<UserDataChangeLock> locks = userDataChangeLockMapper.selectBatchIds(ids);
                        for (UserDataChangeLock lock : locks) {
                            originalContentMap.put("400_" + lock.getLock_id(),
                                lock.getNickname() != null ? lock.getNickname() : "");
                        }
                        break;
                    }
                }
            }
        }

        // 转换为 VO 并填充原始内容
        List<AdminAuditRecordVO> voList = result.getRecords().stream().map(record -> {
            AdminAuditRecordVO vo = new AdminAuditRecordVO();
            BeanUtils.copyProperties(record, vo);
            String key = record.getContent_type() + "_" + record.getContent_id();
            vo.setOriginal_content(originalContentMap.getOrDefault(key, ""));
            return vo;
        }).collect(Collectors.toList());

        Page<AdminAuditRecordVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(voList);

        log.info("分页查询审核记录完成 - 页码: {}, 每页: {}, 内容类型: {}, 审核状态: {}, 关键词: {}, 排序: {}, 总条数: {}",
            current, size, contentType, approvalStatus, keyword, orderBy, result.getTotal());

        return voPage;
    }
}
