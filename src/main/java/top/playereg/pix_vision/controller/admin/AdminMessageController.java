package top.playereg.pix_vision.controller.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import top.playereg.pix_vision.pojo.ResponsePojo;
import top.playereg.pix_vision.pojo.VO.MessageVO;
import top.playereg.pix_vision.pojo.entity.Message;
import top.playereg.pix_vision.service.MessageService;
import top.playereg.pix_vision.service.TokenWhitelistService;
import top.playereg.pix_vision.util.PageUtils;

import java.util.Map;

/**
 * 管理员消息管理控制器
 * <p>
 * 提供消息相关的管理功能，包括密钥更换等敏感操作。
 * </p>
 *
 * @author PlayerEG
 * @since V4.0
 */
@Tag(name = "系统管理员 - 消息管理", description = "消息相关的管理功能，包括密钥更换等敏感操作")
@RestController
@RequestMapping("/api/admin/message")
public class AdminMessageController extends AdminBaseController {

    @Autowired
    private MessageService messageService;

    @Autowired
    private TokenWhitelistService tokenWhitelistService;

    /**
     * 更换消息加密密钥
     * <p>
     * 更换RSA密钥对，并批量更新所有私信的加密内容。
     * 此操作需要管理员权限（角色77），且执行时间较长。
     * </p>
     *
     * @param request HTTP请求对象
     * @return 操作结果
     */
    @Operation(
        summary = "更换消息加密密钥（需要登录认证 + 角色权限[77]）",
        description = """
            ## 功能说明
            更换RSA密钥对，并批量更新所有私信的加密内容。

            ## 业务逻辑
            1. 验证管理员权限（角色77）
            2. 备份旧密钥为.bak文件
            3. 生成新的RSA密钥对
            4. 分批查询所有私信（message_type = 'private'）
            5. 对每批数据：旧密钥解密 → 新密钥加密 → 批量更新数据库
            6. 返回处理结果（成功数量/失败数量）

            ## 注意事项
            - 此操作执行时间较长，取决于私信数据量
            - 操作过程中会备份旧密钥，确保数据安全
            - 如果操作失败，可以手动恢复.bak文件
            - 建议在系统低峰期执行此操作
            """
    )
    @PostMapping("/rotate-keys")
    public ResponsePojo rotateEncryptionKeys(HttpServletRequest request) {
        // 验证管理员权限
        Integer adminId = validateToken(request, "更换消息加密密钥", tokenWhitelistService);
        if (adminId == null) {
            return ResponsePojo.error(null, "认证失败，请重新登录");
        }

        // 调用服务层执行密钥更换
        try {
            Map<String, Object> result = messageService.rotateEncryptionKeys();
            return ResponsePojo.success(result, "密钥更换完成");
        } catch (Exception e) {
            log.error("密钥更换失败，管理员ID：{}，错误：{}", adminId, e.getMessage(), e);
            return ResponsePojo.error(null, "密钥更换失败：" + e.getMessage());
        }
    }

    /**
     * 分页查询私信记录
     *
     * @param current      当前页码
     * @param size         每页大小
     * @param username     用户名（可选）
     * @param participants 参与者用户名（可选，格式：'user1,user2'）
     * @param keyword      关键字（可选）
     * @param startTime    开始时间（可选）
     * @param endTime      结束时间（可选）
     * @param orderBy      排序方式（可选）
     * @return 分页结果
     * @author PlayerEG
     */
    @Operation(
        summary = "分页查询聊天记录",
        description = """
            # 分页查询聊天记录（需要登录认证 + 角色权限[77]）

            ## 特性
            - 需要系统管理员角色（role=77）才能访问
            - 支持按用户ID筛选（查询该用户作为发送者或接收者的消息）
            - 支持按消息类型筛选（私信、系统通知）
            - 支持关键字模糊搜索消息内容
            - 支持按时间范围筛选
            - 支持按时间排序（最新/最早）
            - 私信内容自动解密显示

            ## 参数说明：
            - **current**: **当前页码**，Long 类型，必填，从 1 开始
            - **size**: **每页大小**，Long 类型，必填，范围 1-500
            - **username**: **用户名**，String 类型，可选，查询该用户作为发送者或接收者的消息
            - **participants**: **参与者用户名**，String 类型，可选，查看指定用户之间的对话，格式：'user1,user2'（查询user1和user2之间的对话），注意：participants优先级高于username
            - **keyword**: **关键字**，String 类型，可选，模糊搜索消息内容
            - **startTime**: **开始时间**，String 类型，可选，格式：yyyy-MM-dd HH:mm:ss
            - **endTime**: **结束时间**，String 类型，可选，格式：yyyy-MM-dd HH:mm:ss
            - **orderBy**: **排序方式**，String 类型，可选
              - 'oldest': 按最早消息排列
              - 其他值或 null: 按最新消息排列（默认）

            ## 返回说明：
            - **成功**：返回 IPage<MessageVO> 对象，包含消息列表、分页信息
            - **无数据**：返回空的分页结果（total=0, records=[]）

            ## 业务逻辑：
            1. 验证管理员权限（角色77）
            2. 校验分页参数（current>=1, 1<=size<=500）
            3. 根据可选参数构建动态SQL查询
            4. 查询结果包含发送者和接收者信息
            5. 自动解密私信内容
            6. 返回分页结果集

            ## 注意事项：
            - 所有筛选条件均为可选，可以自由组合
            - 查询结果包含完整的消息信息字段
            - 不过滤任何删除状态，管理员可查看所有消息
            - 私信内容会自动解密显示
            """
    )
    @GetMapping("/page/{current}/{size}")
    public ResponsePojo<IPage<MessageVO>> getAdminMessages(
        @Parameter(description = "HTTP 请求对象", required = true) HttpServletRequest request,
        @Parameter(description = "当前页码（从 1 开始）", required = true, example = "1") @PathVariable Long current,
        @Parameter(description = "每页大小（范围 1-500）", required = true, example = "10") @PathVariable Long size,
        @Parameter(description = "用户名（可选，查询该用户作为发送者或接收者的消息）", example = "user1", required = false) @RequestParam(required = false) String username,
        @Parameter(description = "参与者用户名（可选，查看指定用户之间的对话，格式：'user1,user2'）", example = "user1,user2", required = false) @RequestParam(required = false) String participants,
        @Parameter(description = "关键字（可选，模糊搜索消息内容）", example = "keyword", required = false) @RequestParam(required = false) String keyword,
        @Parameter(description = "开始时间（可选，格式：yyyy-MM-dd HH:mm:ss）", example = "2023-01-01 00:00:00", required = false) @RequestParam(required = false) String startTime,
        @Parameter(description = "结束时间（可选，格式：yyyy-MM-dd HH:mm:ss）", example = "2023-12-31 23:59:59", required = false) @RequestParam(required = false) String endTime,
        @Parameter(description = "排序方式（可选，'oldest'-最早, 其他值-最新）", example = "oldest", required = false) @RequestParam(required = false) String orderBy
    ) {
        // 验证管理员权限
        Integer adminId = validateToken(request, "查询聊天记录", tokenWhitelistService);
        if (adminId == null) {
            return ResponsePojo.error(null, "认证失败，请重新登录");
        }

        // 分页参数校验
        ResponsePojo<?> validateResult = PageUtils.validatePageParams(current, size);
        if (validateResult != null) {
            return (ResponsePojo<IPage<MessageVO>>) (ResponsePojo<?>) validateResult;
        }

        try {
            Page<Message> page = new Page<>(PageUtils.getValidCurrent(current), PageUtils.getValidSize(size));
            IPage<MessageVO> result = messageService.getAdminMessages(page, username, participants, keyword, startTime, endTime, orderBy);
            return ResponsePojo.success(result, "查询成功，共 " + result.getTotal() + " 条记录");
        } catch (Exception e) {
            log.error("管理员查询聊天记录异常 - 错误: {}", e.getMessage(), e);
            return ResponsePojo.error(null, "系统错误: " + e.getMessage());
        }
    }
}
