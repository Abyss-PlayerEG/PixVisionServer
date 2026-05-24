package top.playereg.pix_vision.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 用户数据变更锁定实体
 * <p>对应 tb_user_data_change_lock 表，用于存放待审核的用户信息变更记录</p>
 *
 * @author PlayerEG
 */
@Data
@TableName("tb_user_data_change_lock")
public class UserDataChangeLock {

    /** 主键 ID */
    @TableId(type = IdType.AUTO)
    private Integer lockId;

    /** 待审核用户 ID */
    private Integer userId;

    /** 修改类型：100-昵称、200-权限、300-头像 */
    private Integer type;

    /** 待审核昵称 */
    private String nickname;

    /** 修改的用户角色 */
    private Integer userRole;

    /** 更改的用户头像路径 */
    private String avatarUrl;

    /** 旧数据，用于回滚 */
    private String oldData;

    /** 审核状态：10-通过、20-待审核、30-未过审 */
    private Integer approvalStatus;
}
