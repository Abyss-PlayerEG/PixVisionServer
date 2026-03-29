package top.playereg.pix_vision.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OperateLog {
    Integer sys_log_id;
    Integer user_id;
    Timestamp log_datetime;
    String log_event;
}