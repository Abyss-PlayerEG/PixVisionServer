package top.playereg.pix_vision.pojo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "系统相关信息")
public class SystemInfo {

    @Schema(description = "获取时间", required = true)
    private String nowTime;

    @Schema(description = "操作系统架构")
    private String osArch;

    @Schema(description = "操作系统名称")
    private String osName;

    @Schema(description = "操作系统版本")
    private String osVersion;

    @Schema(description = "用户名")
    private String userName;

    @Schema(description = "用户主目录")
    private String userHome;

    @Schema(description = "项目目录")
    private String userCurrentDir;

    @Schema(description = "用户临时目录")
    private String userTempDir;

    @Schema(description = "用户语言")
    private String userLanguage;

    @Schema(description = "用户国家")
    private String userCountry;

    @Schema(description = "主机名")
    private String hostName;

    @Schema(description = "主机地址")
    private String hostAddress;

    @Schema(description = "最大内存")
    private String maxMemory;

    @Schema(description = "总内存")
    private String totalMemory;

    @Schema(description = "空闲内存")
    private String freeMemory;

    @Schema(description = "可用内存")
    private String usableMemory;

    @Schema(description = "Java 虚拟机名称")
    private String jvmName;

    @Schema(description = "Java 虚拟机版本")
    private String jvmVersion;

    @Schema(description = "Java 虚拟机供应商")
    private String jvmVendor;

    @Schema(description = "Java 虚拟机信息")
    private String jvmInfo;

    @Schema(description = "Java 虚拟机规范名称")
    private String jvmSpecName;

    @Schema(description = "Java 虚拟机规范版本")
    private String jvmSpecVersion;

    @Schema(description = "Java 虚拟机规范供应商")
    private String jvmSpecVendor;

    @Schema(description = "JDK 版本")
    private String jdkVersion;

    @Schema(description = "JDK 供应商")
    private String jdkVendor;

    @Schema(description = "JDK 供应商 URL")
    private String jdkVendorURL;

    @Schema(description = "JDK 规范名称")
    private String jdkSpecName;

    @Schema(description = "JDK 规范版本")
    private String jdkSpecVersion;

    @Schema(description = "JDK 规范供应商")
    private String jdkSpecVendor;
}
