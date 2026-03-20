package top.playereg.pix_vision.pojo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ApiModel("用户实体")
public class User {

    @ApiModelProperty( value = "用户ID" )
    Integer user_id;
    @ApiModelProperty( value = "用户UUID" )
    Byte user_uuid;
    @ApiModelProperty( value = "用户名")
    String username;
    @ApiModelProperty( value = "用户密码")
    String password;
    @ApiModelProperty( value = "用户昵称" )
    String nickname;
    @ApiModelProperty( value = "用户头像路径")
    String avatar_url; //用户头像
    @ApiModelProperty( value = "绑定邮箱" )
    String email;
    @ApiModelProperty( value = "删除标签")
    Boolean is_delete;
    @ApiModelProperty( value = "账户状态")
    Integer status;
    @ApiModelProperty( value = "更新人员")
    Timestamp update_time; //更新时间戳
    @ApiModelProperty( value = "更新人员ID" )
    Integer update_user; //更新人员ID
    @ApiModelProperty( value = "创建时间" )
    Timestamp create_time;
    @ApiModelProperty( value = "创建人员ID" )
    Integer create_user; //创建人员ID

}
