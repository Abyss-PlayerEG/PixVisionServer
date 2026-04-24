package top.playereg.pix_vision.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.playereg.pix_vision.pojo.ResponsePojo;
import top.playereg.pix_vision.pojo.Series;
import top.playereg.pix_vision.service.SeriesService;
import top.playereg.pix_vision.service.TokenWhitelistService;
import top.playereg.pix_vision.util.JWTUtils;

/**
 * 系列管理控制器
 *
 * @author PlayerEG
 */
@Tag(name = "作品系列管理接口")
@RestController
@RequestMapping("/api/work/series")
public class SeriesController {

    private static final Logger log = LoggerFactory.getLogger(SeriesController.class);

    private final SeriesService seriesService;
    private final TokenWhitelistService tokenWhitelistService;

    public SeriesController(SeriesService seriesService, TokenWhitelistService tokenWhitelistService) {
        this.seriesService = seriesService;
        this.tokenWhitelistService = tokenWhitelistService;
    }

    /**
     * 新增作品系列
     *
     * @param request     HTTP 请求对象，用于从 Header 或 URL 参数中获取 Token
     * @param seriesTitle 系列标题（最多 16 个中文字符）
     * @param aboutText   系列描述文本（最多 24 个中文字符）
     * @return 新增的系列信息
     * @author PlayerEG
     */
    @PostMapping("/add")
    @Operation(
        summary = "新增作品系列接口",
        description = """
            # 新增作品系列（需要登录认证）

            ## 特性
            - Token 认证（支持 Header 和 URL 参数两种方式）
            - 系列标题长度限制（≤16 个中文字符）
            - 系列描述长度限制（≤24 个中文字符）
            - 自动关联当前登录用户

            ## 参数说明：
            - Authorization: Header 中的 Token，格式为 `Bearer <token>`，或通过 URL 参数 `?token=<token>` 传递
            - seriesTitle: 系列标题，字符串类型，必填，长度不超过 16 个中文字符
            - aboutText: 系列描述文本，字符串类型，可选，长度不超过 24 个中文字符

            ## 返回说明：
            - **成功**：返回 **{"data": Series 对象}** 和提示信息
            - **Token 失效**：返回 **{"data": null}** 和 "Token 已失效" 提示
            - **标题为空**：返回 **{"data": null}** 和 "系列标题不能为空" 提示
            - **标题过长**：返回 **{"data": null}** 和 "系列标题长度不能超过 16 个字符" 提示
            - **描述过长**：返回 **{"data": null}** 和 "系列描述长度不能超过 24 个字符" 提示

            ## 业务逻辑：
            1. 从请求中提取并验证 Token
            2. 检查 Token 是否在白名单中
            3. 从 Token 中解析用户 ID
            4. 校验系列标题和描述的长度
            5. 创建系列对象并设置用户 ID、时间戳等信息
            6. 插入数据库并返回结果

            ## 注意事项：
            - 系列标题**必填**，长度不超过 16 个字符
            - 系列描述**可选**，长度不超过 24 个字符
            - 系统自动记录创建者 ID 和创建时间
            - 返回的 Series 对象包含自动生成的 series_id
            """
    )
    public ResponsePojo<Series> addSeries(
        @Parameter(description = "HTTP 请求对象，用于从 Header 或 URL 参数中获取 Token", required = true) HttpServletRequest request,
        @Parameter(description = "系列标题，最多 16 个中文字符", required = true, example = "我的作品集") @RequestParam String seriesTitle,
        @Parameter(description = "系列描述文本，最多 24 个中文字符", required = false, example = "这是一个展示我作品的系列") @RequestParam(required = false, defaultValue = "") String aboutText
    ) {
        log.debug("新增系列 - 标题: {}", seriesTitle);

        // 提取 Token
        String token = JWTUtils.extractTokenWithLog(request, "新增系列接口");

        if (token == null || token.isEmpty()) {
            log.error("新增系列失败 - Token 不存在");
            return ResponsePojo.error(null, "Token 不存在，请在 Header 中添加 Authorization: Bearer <token> 或在 URL 参数中添加 ?token=<token>");
        }

        // 检查 Token 是否在白名单中
        if (!tokenWhitelistService.isInWhitelist(token)) {
            log.warn("Token 不在白名单中，可能已过期或被移除");
            return ResponsePojo.error(null, "Token 已失效");
        }

        // 从 Token 中获取用户 ID
        Integer userId = JWTUtils.getUserIdFromToken(token);
        if (userId == null) {
            log.error("从 Token 中解析用户 ID 失败");
            return ResponsePojo.error(null, "Token 无效");
        }

        String username = JWTUtils.getUsernameFromToken(token);
        log.info("开始新增系列，用户 ID: {}, 用户名: {}, 系列标题: {}", userId, username, seriesTitle);

        // 校验系列标题参数
        if (seriesTitle == null || seriesTitle.isEmpty()) {
            log.warn("系列标题为空，用户 ID: {}", userId);
            return ResponsePojo.error(null, "系列标题不能为空");
        }

        if (seriesTitle.length() > 16) {
            log.warn("系列标题长度不符合要求，用户 ID: {}, 标题长度: {}", userId, seriesTitle.length());
            return ResponsePojo.error(null, "系列标题长度不能超过 16 个字符");
        }

        // 校验系列描述参数
        if (aboutText != null && aboutText.length() > 24) {
            log.warn("系列描述长度不符合要求，用户 ID: {}, 描述长度: {}", userId, aboutText.length());
            return ResponsePojo.error(null, "系列描述长度不能超过 24 个字符");
        }

        // 调用服务层新增系列
        Series series = seriesService.addSeries(userId, seriesTitle, aboutText);

        if (series != null) {
            log.info("系列新增成功，系列 ID: {}, 用户 ID: {}", series.getSeries_id(), userId);
            return ResponsePojo.success(series, "系列新增成功");
        } else {
            log.error("系列新增失败，用户 ID: {}", userId);
            return ResponsePojo.error(null, "系列新增失败");
        }
    }
}
