package top.playereg.pix_vision.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 点赞（赞赏）实体类
 *
 * @author PlayerEG
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("tb_like")
@Schema(description = "点赞实体")
public class Like {

    @TableField("user_id")
    @Schema(description = "用户 ID")
    private Integer user_id;

    @TableField("work_id")
    @Schema(description = "作品 ID")
    private Integer work_id;

    @TableLogic
    @TableField("is_delete")
    @Schema(description = "删除标签：0 - 未删除、1 - 已删除")
    private Boolean is_delete;

    @TableField("time")
    @Schema(description = "操作时间")
    private LocalDateTime time;
}
