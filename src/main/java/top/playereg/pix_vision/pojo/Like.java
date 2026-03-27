package top.playereg.pix_vision.pojo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "赞赏实体")
public class Like {

    @Schema(description = "用户 ID")
    Integer user_id;

    @Schema(description = "作品 ID")
    Integer work_id;

    @Schema(description = "删除标签：0 - 未删除、1 - 已删除")
    Boolean is_delete;
}
