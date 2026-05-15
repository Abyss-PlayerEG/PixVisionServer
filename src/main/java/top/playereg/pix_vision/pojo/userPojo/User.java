package top.playereg.pix_vision.pojo.userPojo;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "用户实体")
public class User {

    @TableId("user_id")
    @Schema(description = "用户 ID", example = "1")
    Integer user_id;

    @Schema(description = "用户 UUID", example = "16字节二进制uuid")
    byte[] user_uuid;

    @Schema(description = "用户名", example = "li_hua")
    String username;

    @Schema(description = "用户密码", example = "哈希256加密密码")
    String password;

    @Schema(description = "用户昵称", example = "李华")
    String nickname;

    @Schema(description = "用户角色", example = "11")
    Integer user_role;

    @Schema(description = "用户头像路径", example = "/avatar/uuid.png")
    String avatar_url;

    @Schema(description = "绑定邮箱", example = "lihua@example.com")
    String email;

    @TableLogic
    @Schema(description = "删除标签", example = "0")
    Boolean is_delete;

    @Schema(description = "账户状态", example = "10")
    Integer status;

    // V1.4 数据库 tb_user 表没有 approval_status 字段，已移除
    // @Schema(description = "审核状态：10 - 正常、20 - 待审核、30 - 未过审")
    // Integer approval_status;

    @Schema(description = "更新时间", example = "2026-03-29T12:00:00.003+00:00")
    Timestamp update_time;

    @Schema(description = "更新人员 ID", example = "0")
    Integer update_user;

    @Schema(description = "创建时间", example = "2026-03-29T12:00:00.003+00:00")
    Timestamp create_time;

    @Schema(description = "创建人员 ID", example = "0")
    Integer create_user;


    // 转换后的属性
//    @Schema(description = "用户头像 base64 字符串", example = "我是 base64")
//    private String avatar_base64;

    @Schema(description = "用户 UUID 字符串", example = "字符串类型uuid")
    private String string_user_uuid;
}
