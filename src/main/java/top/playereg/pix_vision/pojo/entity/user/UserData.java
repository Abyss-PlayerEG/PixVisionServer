package top.playereg.pix_vision.pojo.entity.user;

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
@Schema(description = "用户数据实体")
public class UserData {

    @TableId
    @Schema(description = "数据 ID")
    Integer data_id;

    @Schema(description = "用户 ID")
    Integer user_id;

    @Schema(description = "数据项目名称（电话、邮箱、网站、微信等等）")
    String user_data_name;

    @Schema(description = "数据内容（具体的电话号码、邮箱地址、网站 url 等等）")
    String user_data;

    @TableLogic
    @Schema(description = "删除标签：0 - 未删除、1 - 已删除")
    Boolean is_delete;

    @Schema(description = "数据条目更新时间戳")
    Timestamp update_time;

    @Schema(description = "修改者 id，系统修改为 0")
    Integer update_user;

    @Schema(description = "数据条目创建时间戳")
    Timestamp create_time;

    @Schema(description = "存储创建者 id，系统创建为 0")
    Integer create_user;
}
