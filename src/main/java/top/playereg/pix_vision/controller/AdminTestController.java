package top.playereg.pix_vision.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.playereg.pix_vision.pojo.ResponsePojo;
import top.playereg.pix_vision.util.Annotation.RequireRole;

/**
 * 管理员测试控制器
 * <p>
 * 用于演示 @RequireRole 注解的使用方式
 * </p>
 *
 * @author PlayerEG
 * @see RequireRole
 */
@RestController
@RequestMapping("/api/admin/test")
@Tag(name = "管理员测试接口")
@RequireRole(value = {77})  // 整个控制器需要系统管理员权限
@SuppressWarnings("all")
public class AdminTestController {

    private static final Logger log = LoggerFactory.getLogger(AdminTestController.class);

    /**
     * 系统管理员专属接口
     * 只有角色代码为 77 的用户可以访问
     */
    @GetMapping("/system-only")
    @Operation(summary = "系统管理员专属接口", description = "仅系统管理员（角色77）可访问")
    public ResponsePojo<String> systemOnly() {
        log.info("系统管理员接口被调用");
        return ResponsePojo.success("系统管理员专属数据", "访问成功");
    }

    /**
     * 工单管理员或系统管理员可访问
     * allowHigher=true 表示更高级别也可以访问
     */
    @GetMapping("/ticket-management")
    @RequireRole(value = {66, 77}, allowHigher = true)
    @Operation(summary = "工单管理接口", description = "工单管理员（66）或系统管理员（77）可访问")
    public ResponsePojo<String> ticketManagement() {
        log.info("工单管理接口被调用");
        return ResponsePojo.success("工单管理数据", "访问成功");
    }

    /**
     * 审核员专属接口
     * allowHigher=false 表示仅允许指定角色，不允许更高级别
     */
    @GetMapping("/content-review")
    @RequireRole(value = {55}, allowHigher = false)
    @Operation(summary = "内容审核接口", description = "仅审核员（角色55）可访问，不允许更高级别")
    public ResponsePojo<String> contentReview() {
        log.info("内容审核接口被调用");
        return ResponsePojo.success("内容审核数据", "访问成功");
    }
}
