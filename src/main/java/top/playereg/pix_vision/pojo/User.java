package top.playereg.pix_vision.pojo;

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

    @Schema(description = "用户 ID",example = "1")
    Integer user_id;

    @Schema(description = "用户 UUID",example = "81e3aeca61ce93a0855f3d192f0c8e9157f459b6b54ad0d2238c0c1253d6432b")
    byte[] user_uuid;

    @Schema(description = "用户名",example = "li_hua")
    String username;

    @Schema(description = "用户密码",example = "passwd")
    String password;

    @Schema(description = "用户昵称",example = "李华")
    String nickname;

    @Schema(description = "用户头像路径",example = "/avatar/uuid.png")
    String avatar_url;

    @Schema(description = "绑定邮箱",example = "lihua@example.com")
    String email;

    @TableLogic
    @Schema(description = "删除标签",example = "0")
    Boolean is_delete;

    @Schema(description = "账户状态",example = "10")
    Integer status;

    @Schema(description = "更新时间",example = "2026-03-29T12:00:00.003+00:00")
    Timestamp update_time;

    @Schema(description = "更新人员 ID",example = "0")
    Integer update_user;

    @Schema(description = "创建时间",example = "2026-03-29T12:00:00.003+00:00")
    Timestamp create_time;

    @Schema(description = "创建人员 ID",example = "0")
    Integer create_user;

}
