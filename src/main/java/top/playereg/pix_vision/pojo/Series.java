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
@Schema(description = "作品系列实体")
public class Series {

    @Schema(description = "系列 ID")
    Integer series_id;

    @Schema(description = "用户 ID")
    Integer user_id;

    @Schema(description = "系列标题，16 个中文长度")
    String series_title;

    @Schema(description = "系列描述文本，24 个中文长度")
    String about_text;

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
