package top.playereg.pix_vision.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import top.playereg.pix_vision.pojo.ResponsePojo;
import top.playereg.pix_vision.pojo.Works;
import top.playereg.pix_vision.service.WorkService;

/**
 * API 文档规范示例 Controller
 * 
 * 本文件展示了如何按照项目规范编写 Swagger 文档。
 */
@RestController
@RequestMapping("/api/example")
@Tag(name = "示例接口", description = "用于演示 Swagger 文档编写规范")
public class ExampleDocController {

    private final WorkService workService;

    public ExampleDocController(WorkService workService) {
        this.workService = workService;
    }

    @Operation(
        summary = "查询单个作品详情",
        description = """
            # 查询单个作品（无需登录认证）

            ## 特性
            - 公开接口（无需 Token 认证）
            - 根据作品 ID 精确查询
            - 仅返回未删除且状态正常的作品
            - 自动增加作品的浏览次数统计

            ## 参数说明：
            - workId: **作品 ID**，Integer 类型，路径变量，必填

            ## 返回说明：
            - **查询成功**：返回 `{"data": {Works对象}}`，包含作品标题、图片 URL、作者信息等完整详情
            - **作品不存在**：返回 `{"data": null}` 和 "作品不存在或已删除" 提示
            - **参数错误**：返回 `{"data": null}` 和 "作品 ID 无效" 提示

            ## 业务逻辑：
            1. 校验作品 ID 参数的有效性（非空、大于 0）
            2. 调用 Service 层查询作品详细信息
            3. 验证作品是否存在且未被逻辑删除
            4. 异步增加该作品的浏览次数（view_count + 1）
            5. 封装并返回作品数据

            ## 注意事项：
            - 这是一个**公开接口**，任何用户均可访问
            - 返回的图片 URL 为文件名，完整访问路径需拼接：`/api/image/works?filePath={img_url}`
            - 浏览次数增加操作为异步执行，不影响主查询响应速度
            """
    )
    @GetMapping("/work/{workId}")
    public ResponsePojo<Works> getWorkDetail(
        @Parameter(description = "作品唯一标识 ID", required = true, example = "1001") 
        @PathVariable Integer workId
    ) {
        // 业务逻辑实现...
        Works work = workService.getWorkById(workId);
        if (work == null) {
            return ResponsePojo.error(null, "作品不存在或已删除");
        }
        return ResponsePojo.success(work, "查询成功");
    }
}
