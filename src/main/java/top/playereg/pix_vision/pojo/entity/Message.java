package top.playereg.pix_vision.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 消息数据库实体类
 * <p>
 * 用于存储站内消息，支持系统通知、互动通知和私信。
 * 系统消息统一使用 from_user_id = 0 表示。
 * </p>
 *
 * <h3>消息类型</h3>
 * <ul>
 *   <li><b>system</b> - 系统通知，包括审核结果、互动通知（点赞、收藏、评论）</li>
 *   <li><b>private</b> - 私信，用户之间的私密消息</li>
 * </ul>
 *
 * <h3>使用场景</h3>
 * <ol>
 *   <li>存储用户消息</li>
 *   <li>查询未读消息</li>
 *   <li>会话列表展示</li>
 * </ol>
 *
 * @author PlayerEG
 * @see top.playereg.pix_vision.controller.MessageController 消息控制器
 * @see top.playereg.pix_vision.service.MessageService 消息服务接口
 * @since V4.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_messages")
@Schema(description = "消息数据库实体")
public class Message {

    /**
     * 消息ID，自增主键
     */
    @TableId(type = IdType.AUTO)
    @Schema(description = "消息ID", example = "1")
    private Integer message_id;

    /**
     * 消息内容
     */
    @Schema(description = "消息内容", example = "你的作品已通过审核")
    private String message;

    /**
     * 消息主题，对应 MessageProject.code
     */
    @Schema(description = "消息主题（MessageProject.code）", example = "work_audit")
    private String project;

    /**
     * 发送者用户ID，系统消息为 0
     */
    @Schema(description = "发送者用户ID（系统消息为0）", example = "0")
    private Integer from_user_id;

    /**
     * 消息类型，对应 MessageType.code
     */
    @Schema(description = "消息类型（MessageType.code）", example = "system")
    private String message_type;

    /**
     * 关联实体ID（如作品ID、评论ID等）
     */
    @Schema(description = "关联实体ID", example = "123")
    private Integer ref_id;

    /**
     * 接收者用户ID
     */
    @TableField("`to`")
    @Schema(description = "接收者用户ID", example = "1001")
    private Integer to;

    /**
     * 是否已读：0-未读、1-已读
     */
    @Schema(description = "是否已读：0-未读、1-已读", example = "false")
    private Boolean is_read;

    /**
     * 发送者删除标记：0-未删除、1-已删除
     */
    @Schema(description = "发送者删除标记：0-未删除、1-已删除", example = "false")
    private Boolean is_delete_by_sender;

    /**
     * 接收者删除标记：0-未删除、1-已删除
     */
    @Schema(description = "接收者删除标记：0-未删除、1-已删除", example = "false")
    private Boolean is_delete_by_receiver;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间", example = "2026-06-11T10:30:00")
    private LocalDateTime create_time;
}
