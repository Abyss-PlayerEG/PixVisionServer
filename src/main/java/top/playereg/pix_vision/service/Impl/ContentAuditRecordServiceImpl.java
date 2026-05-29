package top.playereg.pix_vision.service.Impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.playereg.pix_vision.mapper.ContentAuditRecordMapper;
import top.playereg.pix_vision.pojo.VO.admin.AdminAuditRecordVO;
import top.playereg.pix_vision.pojo.entity.ContentAuditRecord;
import top.playereg.pix_vision.service.ContentAuditRecordService;
import top.playereg.pix_vision.util.PixVisionLogger;

import java.util.List;
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

        // 转换为 VO（original_content 已从数据库直接获取）
        List<AdminAuditRecordVO> voList = result.getRecords().stream().map(record -> {
            AdminAuditRecordVO vo = new AdminAuditRecordVO();
            BeanUtils.copyProperties(record, vo);
            return vo;
        }).collect(Collectors.toList());

        Page<AdminAuditRecordVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(voList);

        log.info("分页查询审核记录完成 - 页码: {}, 每页: {}, 内容类型: {}, 审核状态: {}, 关键词: {}, 排序: {}, 总条数: {}",
            current, size, contentType, approvalStatus, keyword, orderBy, result.getTotal());

        return voPage;
    }
}
