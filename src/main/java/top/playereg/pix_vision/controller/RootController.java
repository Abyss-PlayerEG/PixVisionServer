package top.playereg.pix_vision.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import top.playereg.pix_vision.enums.LogColor;
import top.playereg.pix_vision.pojo.ResponsePojo;
import top.playereg.pix_vision.pojo.SystemInfo;
import top.playereg.pix_vision.service.SystemInfoService;

/**
 * 服务端根路由
 * @author PlayerEG
 * */
@Controller
@RequestMapping("/")
@Tag(name = "ServerRoot")
public class RootController {
    private static final Logger log = LoggerFactory.getLogger(RootController.class);
    @Autowired
    private SystemInfoService systemInfoService;

    /**
     * 后端首页
     * @return String
     * @see RootController#home()
     * @apiNote 为服务端添加首页
     * @author PlayerEG
     */
    @Operation(
            summary = "后端首页",
            description = "后端首页"
    )
    @GetMapping(value = "/")
    public String home(){
        log.info(LogColor.colorize("200 获取系统首页成功",LogColor.GREEN));
        return "redirect:/index.html";
    }

    /**
     * 服务健康检查
     * @return String
     * @see RootController#health()
     * @apiNote 通过该接口查看应用是否正常
     * @author PlayerEG
     */
    @Operation(
            summary = "服务健康检查",
            description = "服务健康检查"
    )
    @GetMapping(value = "/health")
    public String health() {
        log.info(LogColor.colorize("200 系统服务正常",LogColor.GREEN));
        return "redirect:/server-status/running.html";
    }

    @Operation(
            summary = "获取系统信息",
            description = "获取系统信息"
    )
    @GetMapping(value = "/system-info")
    @ResponseBody
    public ResponsePojo<SystemInfo> getSystemInfo()
    throws Exception{
        try {
            SystemInfo systemInfo = systemInfoService.getSystemInfo();
            return ResponsePojo.success(systemInfo, "获取系统信息成功");
        } catch (Exception e) {
            return ResponsePojo.error(null, "获取系统信息失败");
        }
    }
}