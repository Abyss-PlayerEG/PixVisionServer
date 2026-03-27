//   _____                                                                                                      _____
//  ( ___ )                                                                                                    ( ___ )
//   |   |~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~|   |
//   |   |                                                                                                      |   |
//   |   |                                                                                                      |   |
//   |   |    ██████╗ ██╗██╗  ██╗███████╗██╗         ██╗   ██╗███████╗██████╗ ███████╗██╗ ██████╗ ███╗   ██╗    |   |
//   |   |    ██╔══██╗██║╚██╗██╔╝██╔════╝██║         ██║   ██║██╔════╝██╔══██╗██╔════╝██║██╔═══██╗████╗  ██║    |   |
//   |   |    ██████╔╝██║ ╚███╔╝ █████╗  ██║         ██║   ██║█████╗  ██████╔╝███████╗██║██║   ██║██╔██╗ ██║    |   |
//   |   |    ██╔═══╝ ██║ ██╔██╗ ██╔══╝  ██║         ╚██╗ ██╔╝██╔══╝  ██╔══██╗╚════██║██║██║   ██║██║╚██╗██║    |   |
//   |   |    ██║     ██║██╔╝ ██╗███████╗███████╗     ╚████╔╝ ███████╗██║  ██║███████║██║╚██████╔╝██║ ╚████║    |   |
//   |   |    ╚═╝     ╚═╝╚═╝  ╚═╝╚══════╝╚══════╝      ╚═══╝  ╚══════╝╚═╝  ╚═╝╚══════╝╚═╝ ╚═════╝ ╚═╝  ╚═══╝    |   |
//   |   |                                                                                                      |   |
//   |   |                                                                                                      |   |
//   |___|~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~|___|
//  (_____)                                                                                                    (_____)

package top.playereg.pix_vision;

import cn.hutool.core.lang.Console;
import cn.hutool.core.util.StrUtil;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import top.playereg.pix_vision.config.FilePathConfig;
import top.playereg.pix_vision.enums.LogColor;
import top.playereg.pix_vision.util.IpUtil;

import java.util.List;

@SpringBootApplication
public class PixVisionApplication implements ApplicationListener<ApplicationReadyEvent> {
    private static final Logger log = LoggerFactory.getLogger(PixVisionApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(PixVisionApplication.class, args);
        Console.log("");
        log.info(LogColor.colorize("200 System is setup", LogColor.GREEN));
    }

    @Override
    public void onApplicationEvent(@NotNull ApplicationReadyEvent event) {
        Console.log(LogColor.colorize(
                """
                        \n                     (♥◠‿◠)ﾉﾞ     像素视觉启动成功     ლ(´ڡ`ლ)
                            ╔═══════════════════════════════════════════════════════════════════════╗
                            ║                                                                       ║
                            ║   ██████╗ ██╗██╗  ██╗    ██╗   ██╗██╗███████╗██╗ ██████╗ ███╗   ██╗   ║
                            ║   ██╔══██╗██║╚██╗██╔╝    ██║   ██║██║██╔════╝██║██╔═══██╗████╗  ██║   ║
                            ║   ██████╔╝██║ ╚███╔╝     ██║   ██║██║███████╗██║██║   ██║██╔██╗ ██║   ║
                            ║   ██╔═══╝ ██║ ██╔██╗     ╚██╗ ██╔╝██║╚════██║██║██║   ██║██║╚██╗██║   ║
                            ║   ██║     ██║██╔╝ ██╗     ╚████╔╝ ██║███████║██║╚██████╔╝██║ ╚████║   ║
                            ║   ╚═╝     ╚═╝╚═╝  ╚═╝      ╚═══╝  ╚═╝╚══════╝╚═╝ ╚═════╝ ╚═╝  ╚═══╝   ║
                            ║                                                                       ║
                            ╚═══════════════════════════════════════════════════════════════════════╝
                        """,
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

        // 获取 API 文档配置状态
        String springdocEnabled = event
                .getApplicationContext()
                .getEnvironment()
                .getProperty("springdoc.enabled", "on");

        String swaggerStatus = "on".equalsIgnoreCase(springdocEnabled) || "true".equalsIgnoreCase(springdocEnabled) ?
                LogColor.colorize("开启", LogColor.GREEN) : LogColor.colorize("关闭", LogColor.RED);

        Console.log("    应用访问URL: {}", LogColor.colorize(localUrl, LogColor.GREEN));
        for (String url : externalUrls) {
            String externalUrl = StrUtil.format("http://{}:{}{}", url, port, contextPath);
            Console.log("    外部访问URL: {}", LogColor.colorize(externalUrl, LogColor.GREEN));
        }
        Console.log("    API文档 (Swagger): {}", swaggerStatus);
    }
}