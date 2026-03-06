package top.playereg.pix_vision.util;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Console;
import cn.hutool.core.util.StrUtil;
import cn.hutool.system.SystemUtil;
import org.springframework.stereotype.Component;
import top.playereg.pix_vision.config.PVSLogConfig;
import top.playereg.pix_vision.enums.LogColor;
import top.playereg.pix_vision.enums.LogType;

@Component
public class PVSLogUtil {

    /**
     * 日志输出
     *
     * @param logType 日志类型（info,warn,error）
     * @param log     日志内容
     * @author PlayerEG
     */

    public static void PVSLog(
            LogType logType,
            String log
    ) {
        if (!PVSLogConfig.getIsOpen()) return; // 判断是否启用
        LogColor color;
        switch (logType) {
            case INFO:
                color = LogColor.GREEN;   // 绿色表示信息
                break;
            case WARN:
                color = LogColor.YELLOW;  // 黄色表示警告
                break;
            case ERROR:
                color = LogColor.RED;     // 红色表示错误
                break;
            case DEBUG:
                color = LogColor.CYAN;    // 青色表示调试
                break;
            default:
                color = LogColor.WHITE;   // 白色作为默认
                break;
        }

        String coloredLog = StrUtil.format(
                LogColor.colorize("┌───", LogColor.PURPLE) + " {}@{} [🕞{}]\n" +
                        LogColor.colorize("└", LogColor.PURPLE) + " {} ",
                SystemUtil.getUserInfo().getName(),
                SystemUtil.getHostInfo().getName(),
                LogColor.colorize(DateUtil.now(), LogColor.YELLOW),
                LogColor.colorize("~$", LogColor.PURPLE)
        ) + LogColor.colorize(
                StrUtil.format(
                        "{} [{}] : {}",
                        logType.getType(),
                        SystemUtil.getCurrentPID(),
                        log
                ),
                color
        );
        Console.log(coloredLog);
        return;
    }

    /**
     * 日志输出系统信息
     *
     * @author PlayerEG
     */
    public void printSystemDetails() {
        PVSLog(LogType.INFO, "打印系统信息");
        Console.log(SystemUtil.getOsInfo()); // 操作系统信息
        Console.log(SystemUtil.getUserInfo()); // 用户信息
        Console.log(SystemUtil.getHostInfo()); // 主机信息
        Console.log(SystemUtil.getRuntimeInfo()); // 运行时信息
        Console.log(SystemUtil.getJvmInfo()); // JVM信息
        Console.log(SystemUtil.getJvmSpecInfo()); // JVM信息
        Console.log(SystemUtil.getJavaInfo()); // Java信息
        Console.log(SystemUtil.getJavaSpecInfo()); // Java信息
        Console.log(SystemUtil.getJavaRuntimeInfo()); // Java运行时信息
    }
}
