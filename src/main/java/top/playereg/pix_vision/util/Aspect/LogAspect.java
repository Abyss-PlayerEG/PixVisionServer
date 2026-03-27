package top.playereg.pix_vision.util.Aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import top.playereg.pix_vision.pojo.OperateLog;

import java.sql.Timestamp;

/**
 * 日志切面
 * @author blue_sky_ks
 */
@Component
@Aspect
@SuppressWarnings("unused")
public class LogAspect {

    //用于打印和输出
    private static final Logger logger = LoggerFactory.getLogger(LogAspect.class);

    /**
     * 环绕通知
     * @param pjp 切点
     * @param logRecord 注解
     * @return 返回结果
     * @throws Throwable 抛出异常
     * @author blue_sky_ks
     */
    @Around( "@annotation(logRecord)" )
    public Object record(ProceedingJoinPoint pjp, LogRecord logRecord) throws Throwable {
        OperateLog operateLog = new OperateLog(); //用于记录操作的实体类

        Object result = pjp.proceed();//返回结果

        long start = System.currentTimeMillis();
        Timestamp timestamp = new Timestamp(start);
        operateLog.setLog_datetime( timestamp );//记录操作时间

        operateLog.setLog_event( logRecord.event() );//记录操作事件

        operateLog.setUser_id(1); //记录操作人员ID --TODO

        //插入进数据库 --TODO
        /*暂时没有*/

        return result;
    }
}
