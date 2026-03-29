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

    @Schema(description = "用户 ID")
    Integer user_id;

    @Schema(description = "用户 UUID")
    byte[] user_uuid;

    @Schema(description = "用户名")
    String username;

    @Schema(description = "用户密码")
    String password;

    @Schema(description = "用户昵称")
    String nickname;

    @Schema(description = "用户头像路径")
    String avatar_url;

    @Schema(description = "绑定邮箱")
    String email;

    @TableLogic
    @Schema(description = "删除标签")
    Boolean is_delete;

    @Schema(description = "账户状态")
    Integer status;

    @Schema(description = "更新人员")
    Timestamp update_time;

    @Schema(description = "更新人员 ID")
    Integer update_user;

    @Schema(description = "创建时间")
    Timestamp create_time;

    @Schema(description = "创建人员 ID")
    Integer create_user; //创建人员 ID

}
