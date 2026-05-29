package top.playereg.pix_vision.pojo.VO;

import lombok.Data;

import java.sql.Timestamp;

/**
 * 用户数据变更锁定记录展示对象（管理员端分页查询用）
 * <p>仅用于待审核记录（approval_status=20）的展示，不含审核状态字段</p>
 *
 * @author PlayerEG
 */
@Data
public class UserDataChangeLockVO {

    /** 主键 */
    private Integer lock_id;

    /** 待审核用户 ID */
    private Integer user_id;

    /** 用户名（关联查询 tb_user） */
    private String username;

    /** 用户当前昵称（关联查询 tb_user） */
    private String user_current_nickname;

    /** 变更类型：100-昵称、200-权限、300-头像 */
    private Integer type;

    /** 待审核昵称（仅 type=100） */
    private String nickname;

    /** 申请的角色代码（仅 type=200） */
    private Integer user_role;

    /** 待审核头像路径（仅 type=300） */
    private String avatar_url;

    /** 旧数据（用于回滚） */
    private String old_data;

    /** 提交时间 */
    private Timestamp create_time;
}
