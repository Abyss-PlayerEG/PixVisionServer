package top.playereg.pix_vision.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OperateLog {
    @TableId
    Integer sys_log_id;
    Integer user_id;
    Timestamp log_datetime;
    String log_event;
}
