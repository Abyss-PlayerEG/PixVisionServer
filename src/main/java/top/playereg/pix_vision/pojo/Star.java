package top.playereg.pix_vision.pojo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("tb_star")
@Schema(description = "收藏实体")
public class Star {

    @TableField("user_id")
    @Schema(description = "用户 ID")
    Integer user_id;

    @TableField("work_id")
    @Schema(description = "作品 ID")
    Integer work_id;

    @TableLogic
    @TableField("is_delete")
    @Schema(description = "删除标签：0 - 未删除、1 - 已删除")
    Boolean is_delete;

    @TableField("time")
    @Schema(description = "操作时间")
    LocalDateTime time;
}
