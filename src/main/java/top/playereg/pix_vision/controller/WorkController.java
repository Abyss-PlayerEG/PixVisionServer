package top.playereg.pix_vision.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.playereg.pix_vision.pojo.ResponsePojo;
import top.playereg.pix_vision.pojo.Works;
import top.playereg.pix_vision.service.WorkService;
import top.playereg.pix_vision.util.Annotation.PublicAccess;

/**
 * 作品控制器 - 提供作品相关的接口
 *
 * @author PlayerEG
 */
@RestController
@RequestMapping("/api/work")
@RequiredArgsConstructor
@Tag(name = "作品接口", description = "提供作品查询、展示等接口")
public class WorkController {
    private static final Logger log = LoggerFactory.getLogger(WorkController.class);

    private final WorkService workService;

    /**
     * 获取首页作品列表（分页）
     *
     * @param current 页码，从 1 开始
     * @param size    每页大小，范围 1-100
     * @return 分页作品列表
     * @author PlayerEG
     */
    @Operation(
        summary = "获取首页作品列表",
        description = """
            # 获取首页作品列表（无需登录认证）

            ## 特性
            - 公开接口（无需认证）
            - 分页查询
            - 仅返回未删除的作品
            - 按创建时间倒序排列（最新作品优先）
            - 返回完整作品实体信息

            ## 参数说明：
            - current: **页码**，长整数类型，必填，从 1 开始
            - size: **每页大小**，长整数类型，必填，范围 1-100

            ## 返回说明：
            - **查询成功**：返回 **{"data": IPage<Works>}** 和提示信息
              - records: 作品列表（包含完整 Works 实体字段）
              - total: 总记录数
              - current: 当前页码
              - size: 每页大小
              - pages: 总页数
            - **参数错误**：返回 **{"data": null}** 和错误提示
            - **无数据**：返回 **{"data": null}** 和“查询失败，返回结果为空”提示

            ## 业务逻辑：
            1. 校验分页参数（current >= 1, size 在 1-100 范围内）
            2. 构建 MyBatis-Plus 分页对象
            3. 查询未删除的作品（is_delete = false）
            4. 按创建时间倒序排列（create_time DESC）
            5. 返回分页结果集（IPage<Works>）

            ## 注意事项：
            - 该接口**无需认证**，任何人都可以访问
            - 仅返回**未删除**的作品（is_delete = false）
            - 作品按**创建时间倒序**排列，最新作品在前
            - 图片 URL 为文件名，完整访问路径为：`/api/image/works?filePath={img_url}`
            - 每页大小限制：**1-100**，超出范围会返回错误
            - 返回完整的 Works 实体，包含所有字段
            - 使用 **RESTful 风格**路径参数，格式：`/homepage/{current}/{size}`

            ## 使用示例：
            ```
            # 获取第 1 页，每页 10 条
            GET /api/work/homepage/1/10

            # 获取第 2 页，每页 20 条
            GET /api/work/homepage/2/20

            # 获取第 1 页，每页 50 条
            GET /api/work/homepage/1/50
            ```
            """
    )
    @PublicAccess("获取首页作品列表，无需认证")
    @GetMapping("/homepage/{current}/{size}")
    public ResponsePojo<IPage<Works>> getHomepageWorks(
        @Parameter(description = "页码，从 1 开始", required = true, example = "1")
        @PathVariable Long current,
        @Parameter(description = "每页大小，范围 1-100", required = true, example = "10")
        @PathVariable Long size
    ) {
        // 参数校验
        if (current == null || current < 1) {
            return ResponsePojo.error(null, "页码必须大于 0");
        }
        if (size == null || size < 1 || size > 100) {
            return ResponsePojo.error(null, "每页大小必须在 1-100 之间");
        }

        // 构建分页对象
        Page<Works> page = new Page<>(current, size);

        // 调用服务层查询首页作品列表
        IPage<Works> result = workService.selectHomepageWorks(page);

        // 返回结果为空，则返回错误信息
        if (result == null || result.getRecords().isEmpty()) {
            log.warn("分页查询返回结果为空 - 页码：{}, 每页：{}", current, size);
            return ResponsePojo.error(null, "查询失败，返回结果为空");
        }

        log.info("分页查询成功 - 页码：{}, 每页：{}, 总数：{}, 返回：{}",
            current, size, result.getTotal(), result.getRecords().size());

        return ResponsePojo.success(result, "查询成功");
    }
}
