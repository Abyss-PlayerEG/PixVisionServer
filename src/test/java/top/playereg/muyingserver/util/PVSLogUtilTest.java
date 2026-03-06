package top.playereg.muyingserver.util;

import junit.framework.TestCase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import top.playereg.pix_vision.enums.LogType;
import top.playereg.pix_vision.util.PVSLogUtil;

@SpringBootTest
@RunWith(SpringRunner.class)
public class PVSLogUtilTest extends TestCase {

    @Test
    public void testMuYingLog() {
        PVSLogUtil PVSLogUtil = new PVSLogUtil();
        PVSLogUtil.MuYingLog(LogType.INFO,"信息日志");
        PVSLogUtil.MuYingLog(LogType.WARN,"警告日志");
        PVSLogUtil.MuYingLog(LogType.ERROR,"报错日志");
        PVSLogUtil.MuYingLog(LogType.DEBUG,"测试日志");
    }

    public void testPrintSystemDetails() {
        PVSLogUtil PVSLogUtil = new PVSLogUtil();
        PVSLogUtil.printSystemDetails();
    }
}