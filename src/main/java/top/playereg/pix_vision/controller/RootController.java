package top.playereg.pix_vision.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import top.playereg.pix_vision.enums.LogType;
import top.playereg.pix_vision.pojo.ResponsePojo;
import top.playereg.pix_vision.pojo.SystemInfo;
import top.playereg.pix_vision.service.SystemInfoService;
import top.playereg.pix_vision.util.PVSLogUtil;

/**
 * 服务端根路由
 * @author PlayerEG
 * */
@Controller
@RequestMapping("/")
@Api(tags = "ServerRoot")
public class RootController {
    @Autowired
    private SystemInfoService systemInfoService;

    /**
     * 后端首页
     * @return String
     * @see RootController#home()
     * @apiNote 为服务端添加首页
     * @author PlayerEG
     */
    @ApiOperation(
            value = "后端首页",
            notes = "后端首页"
    )
    @GetMapping(value = "/")
    public String home(){
        PVSLogUtil.MuYingLog(LogType.INFO, "获取系统首页成功");
        return "redirect:/index.html";
    }

    /**
     * 服务健康检查
     * @return String
     * @see RootController#health()
     * @apiNote 通过该接口查看应用是否正常
     * @author PlayerEG
     */
    @ApiOperation(
            value = "服务健康检查",
            notes = "服务健康检查"
    )
    @GetMapping(value = "/health")
    public String health() {
        PVSLogUtil.MuYingLog(LogType.INFO, "系统服务正常");
        return "redirect:/server-status/running.html";
    }

    @ApiOperation(
            value = "获取系统信息",
            notes = "获取系统信息"
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