package top.playereg.pix_vision.pojo.entity.user;

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
}
