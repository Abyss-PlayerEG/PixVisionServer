package top.playereg.pix_vision.pojo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ApiModel("系统相关信息")
public class SystemInfo {
    @ApiModelProperty(value = "获取时间", required = true)
    private String nowTime;
    @ApiModelProperty(value = "操作系统架构")
    private String osArch;
    @ApiModelProperty(value = "操作系统名称")
    private String osName;
    @ApiModelProperty(value = "操作系统版本")
    private String osVersion;
    @ApiModelProperty(value = "用户名")
    private String userName;
    @ApiModelProperty(value = "用户主目录")
    private String userHome;
    @ApiModelProperty(value = "项目目录")
    private String userCurrentDir;
    @ApiModelProperty(value = "用户临时目录")
    private String userTempDir;
    @ApiModelProperty(value = "用户语言")
    private String userLanguage;
    @ApiModelProperty(value = "用户国家")
    private String userCountry;
    @ApiModelProperty(value = "主机名")
    private String hostName;
    @ApiModelProperty(value = "主机地址")
    private String hostAddress;
    @ApiModelProperty(value = "最大内存")
    private String maxMemory;
    @ApiModelProperty(value = "总内存")
    private String totalMemory;
    @ApiModelProperty(value = "空闲内存")
    private String freeMemory;
    @ApiModelProperty(value = "可用内存")
    private String usableMemory;
    @ApiModelProperty(value = "Java虚拟机名称")
    private String jvmName;
    @ApiModelProperty(value = "Java虚拟机版本")
    private String jvmVersion;
    @ApiModelProperty(value = "Java虚拟机供应商")
    private String jvmVendor;
    @ApiModelProperty(value = "Java虚拟机信息")
    private String jvmInfo;
    @ApiModelProperty(value = "Java虚拟机规范名称")
    private String jvmSpecName;
    @ApiModelProperty(value = "Java虚拟机规范版本")
    private String jvmSpecVersion;
    @ApiModelProperty(value = "Java虚拟机规范供应商")
    private String jvmSpecVendor;
    @ApiModelProperty(value = "JDK版本")
    private String jdkVersion;
    @ApiModelProperty(value = "JDK供应商")
    private String jdkVendor;
    @ApiModelProperty(value = "JDK供应商URL")
    private String jdkVendorURL;
    @ApiModelProperty(value = "JDK规范名称")
    private String jdkSpecName;
    @ApiModelProperty(value = "JDK规范版本")
    private String jdkSpecVersion;
    @ApiModelProperty(value = "JDK规范供应商")
    private String jdkSpecVendor;
}
