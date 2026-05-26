package top.playereg.pix_vision.controller.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import top.playereg.pix_vision.pojo.OperateLogVO;
import top.playereg.pix_vision.pojo.ResponsePojo;
import top.playereg.pix_vision.service.OperateLogService;
import top.playereg.pix_vision.util.Annotation.LogRecord;
import top.playereg.pix_vision.util.Annotation.RequireRole;
import top.playereg.pix_vision.util.PageUtils;
import top.playereg.pix_vision.util.PixVisionLogger;

@RestController
@RequestMapping("/api/admin/logs")
@RequiredArgsConstructor
@Tag(name = "系统管理员接口 - 日志管理", description = "提供给超级管理员，用于查看管理员的操作日志")
@RequireRole(value = {77})
public class AdminLoggerController {
    private static final PixVisionLogger log = PixVisionLogger.create(AdminLoggerController.class);

    private final OperateLogService operateLogService;

    /**
     * 分页查询操作日志
     *
     * @param current 当前页码
     * @param size    每页大小
     * @param keyword 关键字（可选）
     * @param orderBy 排序方式（可选）
     * @return 分页日志视图列表
     * @author PlayerEG
     */
    @Operation(
        summary = "分页查询操作日志",
        description = """
            # 分页查询操作日志（需要登录认证 + 角色权限[77]）

            ## 特性
            - 需要系统管理员角色（role=77）才能访问
            - 支持分页查询所有操作日志
            - 用户名通过 Redis 缓存解析（缓存 1 小时）
            - 即使用户被删除，历史日志中的用户名仍可正常展示
            - 支持关键字模糊搜索（匹配操作事件 log_event）
            - 支持按时间排序（最新/最早）

            ## 参数说明：
            - **current**: **当前页码**，Long 类型，必填，从 1 开始
            - **size**: **每页大小**，Long 类型，必填，范围 1-500
            - **keyword**: **关键字**，String 类型，可选，模糊搜索操作事件（log_event）
            - **orderBy**: **排序方式**，String 类型，可选
              - 'oldest': 按最早操作排列
              - 其他值或 null: 按最新操作排列（默认）

            ## 返回说明：
            - **成功**：返回 IPage<OperateLogVO> 对象，包含日志列表和分页信息
            - **无数据**：返回空的分页结果（total=0, records=[]）
            - 每条记录包含 sys_log_id、user_id、username、log_datetime、log_event

            ## 业务逻辑：
            1. 校验分页参数（current>=1, 1<=size<=500）
            2. 构建 MyBatis-Plus 分页对象查询 tb_sys_logs
            3. 收集所有 user_id，优先从 Redis 获取用户名缓存
            4. 缓存未命中时查询 tb_user 并将结果缓存 1 小时
            5. 即使用户已被删除，Redis 缓存仍保留历史用户名
            6. 返回分页结果集

            ## 注意事项：
            - 用户名优先从 Redis 缓存读取，TTL 为 1 小时
            - 关键字搜索只匹配 log_event 字段，不匹配用户名
            - 日志按 log_datetime 排序，默认最新在前
            """
    )
    @GetMapping("/page/{current}/{size}")
    public ResponsePojo<IPage<OperateLogVO>> getOperateLogsPage(
        @Parameter(description = "当前页码（从 1 开始）", required = true, example = "1") @PathVariable Long current,
        @Parameter(description = "每页大小（范围 1-500）", required = true, example = "10") @PathVariable Long size,
        @Parameter(description = "关键字（可选，模糊搜索操作事件）", required = false) @RequestParam(required = false) String keyword,
        @Schema(description = "排序方式：'oldest' 按最早，其他值或 null 按最新（默认）", allowableValues = {"newest", "oldest"}, example = "newest") @RequestParam(required = false, defaultValue = "newest") String orderBy
    ) {
        // 参数校验
        ResponsePojo<?> error = PageUtils.validatePageParams(current, size);
        if (error != null) {
            return (ResponsePojo<IPage<OperateLogVO>>) (ResponsePojo<?>) error;
        }

        try {
            IPage<OperateLogVO> result = operateLogService.getOperateLogsPage(current, size, keyword, orderBy);
            return ResponsePojo.success(result, "查询成功");
        } catch (Exception e) {
            log.error("分页查询操作日志异常，错误: {}", e.getMessage(), e);
            return ResponsePojo.error(null, "查询失败：" + e.getMessage());
        }
    }
}
