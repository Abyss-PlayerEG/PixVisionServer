package top.playereg.pix_vision.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 * 游客访问历史记录实体类
 *
 * @author PlayerEG
 */
@Data
@TableName("tb_guest_history")
public class GuestHistory implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 游客历史记录 ID
     */
    @TableId(type = IdType.AUTO)
    private Integer guest_history_id;

    /**
     * 作品 ID
     */
    private Integer work_id;

    /**
     * 访问时间
     */
    private Timestamp visit_time;
}
