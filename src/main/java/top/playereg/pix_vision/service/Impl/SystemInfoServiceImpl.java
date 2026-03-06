package top.playereg.pix_vision.service.Impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.system.SystemUtil;
import org.springframework.stereotype.Service;
import top.playereg.pix_vision.pojo.SystemInfo;
import top.playereg.pix_vision.service.SystemInfoService;

import static top.playereg.pix_vision.util.ConversionUtils.convertBytes;

@Service
public class SystemInfoServiceImpl implements SystemInfoService {
    @Override
    public SystemInfo getSystemInfo() {
        SystemInfo systemInfo = new SystemInfo();

        systemInfo.setNowTime(DateUtil.now());
        systemInfo.setOsArch(SystemUtil.getOsInfo().getArch());
        systemInfo.setOsName(SystemUtil.getOsInfo().getName());
        systemInfo.setOsVersion(SystemUtil.getOsInfo().getVersion());

        systemInfo.setUserName(SystemUtil.getUserInfo().getName());
        systemInfo.setUserHome(SystemUtil.getUserInfo().getHomeDir());
        systemInfo.setUserCurrentDir(SystemUtil.getUserInfo().getCurrentDir());
        systemInfo.setUserTempDir(SystemUtil.getUserInfo().getTempDir());
        systemInfo.setUserLanguage(SystemUtil.getUserInfo().getLanguage());
        systemInfo.setUserCountry(SystemUtil.getUserInfo().getCountry());

        systemInfo.setHostName(SystemUtil.getHostInfo().getName());
        systemInfo.setHostAddress(SystemUtil.getHostInfo().getAddress());

        systemInfo.setMaxMemory(convertBytes(SystemUtil.getRuntimeInfo().getMaxMemory()));
        systemInfo.setTotalMemory(convertBytes(SystemUtil.getRuntimeInfo().getTotalMemory()));
        systemInfo.setFreeMemory(convertBytes(SystemUtil.getRuntimeInfo().getFreeMemory()));
        systemInfo.setUsableMemory(convertBytes(SystemUtil.getRuntimeInfo().getUsableMemory()));

        systemInfo.setJvmName(SystemUtil.getJvmInfo().getName());
        systemInfo.setJvmVersion(SystemUtil.getJvmInfo().getVersion());
        systemInfo.setJvmVendor(SystemUtil.getJvmInfo().getVendor());
        systemInfo.setJvmInfo(SystemUtil.getJvmInfo().getInfo());
        systemInfo.setJvmSpecName(SystemUtil.getJvmSpecInfo().getName());
        systemInfo.setJvmSpecVersion(SystemUtil.getJvmSpecInfo().getVersion());
        systemInfo.setJvmSpecVendor(SystemUtil.getJvmSpecInfo().getVendor());

        systemInfo.setJdkVersion(SystemUtil.getJavaInfo().getVersion());
        systemInfo.setJdkVendor(SystemUtil.getJavaInfo().getVendor());
        systemInfo.setJdkVendorURL(SystemUtil.getJavaInfo().getVendorURL());
        systemInfo.setJdkSpecName(SystemUtil.getJavaSpecInfo().getName());
        systemInfo.setJdkSpecVersion(SystemUtil.getJavaSpecInfo().getVersion());
        systemInfo.setJdkSpecVendor(SystemUtil.getJavaSpecInfo().getVendor());

        return systemInfo;
    }
}
