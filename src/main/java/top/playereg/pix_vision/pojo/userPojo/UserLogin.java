package top.playereg.pix_vision.pojo.userPojo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户登录响应实体
 * 
 * @author PlayerEG
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "用户登录响应实体")
public class UserLogin extends User {
    
    @Schema(description = "JWT Token", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9")
    private String token;

    @Schema(description = "用户头像 base64 字符串", example = "我是 base64")
    private String avatar_base64;

    @Schema(description = "用户 UUID 字符串", example = "字符串类型uuid")
    private String string_user_uuid;
}
