package top.playereg.pix_vision.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import top.playereg.pix_vision.enums.LogColor;
import top.playereg.pix_vision.pojo.ResponsePojo;
import top.playereg.pix_vision.pojo.SystemInfo;
import top.playereg.pix_vision.service.SystemInfoService;
import top.playereg.pix_vision.util.Annotation.PublicAccess;
import top.playereg.pix_vision.util.PixVisionLogger;

/**
 * 服务端根路由
 *
 * @author PlayerEG
 * @see ResponsePojo
 * @see top.playereg.pix_vision.service.Impl.SystemInfoServiceImpl
 */
@Controller
@RequestMapping("/")
@Tag(name = "ServerRoot")
public class RootController {
    private static final PixVisionLogger log = PixVisionLogger.create(RootController.class);
    @Autowired
    private SystemInfoService systemInfoService;

    /**
     * 后端首页
     *
     * @return String 跳转首页
     * @apiNote 为服务端添加首页
     * @author PlayerEG
     */
    @Operation(
        summary = "后端首页",
        description = """
            # 访问服务端首页（无需登录认证）

            ## 特性
            - 公开接口（无需认证）
            - HTTP 重定向
            - 静态资源访问

            ## 功能说明：
            - 重定向到静态资源首页 index.html
            - 用于快速跳转 swagger-doc 和 swagger-ui 页面

            ## 返回说明：
            - 成功：重定向到 /index.html 页面
            """
    )
    @GetMapping(value = "/")
    public String home() {
        log.info(LogColor.colorize("200 获取系统首页成功", LogColor.GREEN));
        return "redirect:/index.html";
    }

    /**
     * 服务健康检查
     *
     * @return String 跳转检测页面
     * @apiNote 通过该接口查看应用是否正常
     * @author PlayerEG
     */
    @Operation(
        summary = "服务健康检查",
        description = """
            # 检查服务是否正常运行（无需登录认证）

            ## 特性
            - 公开接口（无需认证）
            - HTTP 重定向
            - 服务状态监控页面

            ## 功能说明：
            - 通过访问该接口判断应用是否启动成功
            - 跳转到服务运行状态监控页面

            ## 返回说明：
            - 成功：重定向到 /server-status/running.html 页面
            - 可通过页面查看系统运行状态信息
            """
    )
    @PublicAccess("健康检查接口，无需认证")
    @GetMapping(value = "/health")
    public String health() {
        log.info(LogColor.colorize("200 系统服务正常", LogColor.GREEN));
        return "redirect:/server-status/running.html";
    }

    /**
     * 获取系统信息
     *
     * @return ResponsePojo 系统信息
     * @throws Exception 获取系统信息失败
     * @apiNote 获取系统信息获取接口
     * @author PlayerEG
     */
    @Operation(
        summary = "获取系统信息",
        description = """
            # 获取当前服务器的系统信息（无需登录认证）

            ## 特性
            - 公开接口（无需认证）
            - 实时系统信息采集
            - Oshi 库支持
            - 多维度系统指标（JVM/OS/CPU/内存/磁盘）

            ## 返回说明：
            - 成功：返回 **"data": {SystemInfo}** 和"获取系统信息成功"提示
            - 失败：返回 **"data": null** 和"获取系统信息失败"提示

            ## 包含信息：
            - JVM 信息（版本、厂商、运行时间等）
            - 操作系统信息（名称、版本、架构等）
            - CPU 信息（核心数、使用率等）
            - 内存信息（总量、已用、可用等）
            - 磁盘信息（总空间、已用空间、可用空间等）

            ## 注意事项：
            - 该接口不涉及敏感信息，无需认证
            - 数据实时获取，性能开销较小
            """
    )
    @GetMapping(value = "/system-info")
    @ResponseBody
    public ResponsePojo<SystemInfo> getSystemInfo()
        throws Exception {
        try {
            SystemInfo systemInfo = systemInfoService.getSystemInfo();
            return ResponsePojo.success(systemInfo, "获取系统信息成功");
        } catch (Exception e) {
            return ResponsePojo.error(null, "获取系统信息失败");
        }
    }
}
