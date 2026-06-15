package top.playereg.pix_vision.pojo.VO.admin;

import com.baomidou.mybatisplus.annotation.TableField;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import top.playereg.pix_vision.pojo.entity.BaseComment;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@Schema(description = "管理员评论视图（含审核详情）")
public class AdminCommentVO extends BaseComment {

    @TableField(exist = false)
    @Schema(description = "评论者用户名")
    private String username;

    @TableField(exist = false)
    @Schema(description = "评论者昵称")
    private String nickname;

    @TableField(exist = false)
    @Schema(description = "作品标题")
    private String work_title;

    @TableField(exist = false)
    @Schema(description = "AI 审核判断依据")
    private String audit_reason;

    @TableField(exist = false)
    @Schema(description = "命中敏感词，JSON数组字符串")
    private String insult_words;
}
