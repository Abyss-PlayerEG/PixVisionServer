package top.playereg.pix_vision;

import cn.hutool.core.lang.Console;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import top.playereg.pix_vision.config.FilePathConfig;
import top.playereg.pix_vision.enums.LogColor;
import top.playereg.pix_vision.enums.LogType;
import top.playereg.pix_vision.util.IpUtil;
import top.playereg.pix_vision.util.PVSLogUtil;

import java.util.List;

@SpringBootApplication
public class PixVisionApplication implements ApplicationListener<ApplicationReadyEvent> {
    public static void main(String[] args) {
        SpringApplication.run(PixVisionApplication.class, args);
    }

    @Override
    public void onApplicationEvent(@NotNull ApplicationReadyEvent event) {
        Console.log();
        Console.log(LogColor.colorize(
                "                 (♥◠‿◠)ﾉﾞ     像素视觉启动成功     ლ(´ڡ`ლ)            \n" +
                        "╔═══════════════════════════════════════════════════════════════════════╗\n" +
                        "║                                                                       ║\n" +
                        "║   ██████╗ ██╗██╗  ██╗    ██╗   ██╗██╗███████╗██╗ ██████╗ ███╗   ██╗   ║\n" +
                        "║   ██╔══██╗██║╚██╗██╔╝    ██║   ██║██║██╔════╝██║██╔═══██╗████╗  ██║   ║\n" +
                        "║   ██████╔╝██║ ╚███╔╝     ██║   ██║██║███████╗██║██║   ██║██╔██╗ ██║   ║\n" +
                        "║   ██╔═══╝ ██║ ██╔██╗     ╚██╗ ██╔╝██║╚════██║██║██║   ██║██║╚██╗██║   ║\n" +
                        "║   ██║     ██║██╔╝ ██╗     ╚████╔╝ ██║███████║██║╚██████╔╝██║ ╚████║   ║\n" +
                        "║   ╚═╝     ╚═╝╚═╝  ╚═╝      ╚═══╝  ╚═╝╚══════╝╚═╝ ╚═════╝ ╚═╝  ╚═══╝   ║\n" +
                        "║                                                                       ║\n" +
                        "╚═══════════════════════════════════════════════════════════════════════╝",
                LogColor.BLUE
        ));
        Console.log("    服务资源PATH: {}", LogColor.colorize(FilePathConfig.RootPath, LogColor.GREEN));
        // 获取服务端口
        String port = event
                .getApplicationContext()
                .getEnvironment()
                .getProperty("server.port", "8080");
        String contextPath = event
                .getApplicationContext()
                .getEnvironment()
                .getProperty("server.servlet.context-path", "");
        String localUrl = "http://localhost:" + port + contextPath;
        List<String> externalUrls = IpUtil.getAllLocalIpAddresses();

        // 获取Swagger和Knife4j配置状态
        String swaggerEnabled = event
                .getApplicationContext()
                .getEnvironment()
                .getProperty("knife4j.enable", "true");
        String isProduction = event
                .getApplicationContext()
                .getEnvironment()
                .getProperty("knife4j.production", "false");

        String swaggerStatus = Boolean.parseBoolean(swaggerEnabled) && !Boolean.parseBoolean(isProduction) ?
                LogColor.colorize("开启", LogColor.GREEN) : LogColor.colorize("关闭", LogColor.RED);

        Console.log("    应用访问URL: {}", LogColor.colorize(localUrl, LogColor.GREEN));
        for (String url : externalUrls) {
            String externalUrl = "http://" + url + ":" + port + contextPath;
            Console.log("    外部访问URL: {}", LogColor.colorize(externalUrl, LogColor.GREEN));
        }
        Console.log("    API文档(Swagger): {}", swaggerStatus);
        System.out.println();
        PVSLogUtil.MuYingLog(LogType.INFO, "服务启动成功");
    }
}