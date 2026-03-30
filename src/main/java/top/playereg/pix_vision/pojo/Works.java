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
@Schema(description = "作品实体")
public class Works {

    @Schema(description = "作品 ID")
    Integer work_id;

    @Schema(description = "用户 ID")
    Integer user_id;

    @Schema(description = "作品标题，16 个中文长度")
    String work_title;

    @Schema(description = "图片 URL")
    String img_url;

    @Schema(description = "系列 ID")
    Integer series_id;

    @Schema(description = "点赞数")
    Integer like_count;

    @Schema(description = "收藏数")
    Integer star_count;

    @Schema(description = "查看数")
    Integer view_count;

    @Schema(description = "是否原创：1 - 原创、0 - 转载")
    Boolean is_original_work;

    @Schema(description = "外部转载链接")
    String out_url;

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
