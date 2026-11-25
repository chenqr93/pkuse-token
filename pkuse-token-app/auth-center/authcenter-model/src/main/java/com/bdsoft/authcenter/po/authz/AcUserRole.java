package com.bdsoft.authcenter.po.authz;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.bdsoft.authcenter.common.pojo.AbstractAuditPO;

/**
 * 授权中心-用户-角色绑定表（ac_user_role）
 *
 * <p> user_id 为用户中心用户 ID（外部引用，不建外键）：用户档案不落地，实时经接口取自用户中心。 </p>
 */
@TableName("ac_user_role")
public class AcUserRole extends AbstractAuditPO {

    private static final long serialVersionUID = 1L;

    /** 主键（32 位 UUID 字符串） */
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    /** 用户中心用户 ID（外部引用） */
    private String userId;

    /** 角色 ID（ac_role.id） */
    private String roleId;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getRoleId() {
        return roleId;
    }

    public void setRoleId(String roleId) {
        this.roleId = roleId;
    }

    @Override
    public String toString() {
        return "AcUserRole{" + super.toString() +
                ", id='" + id + '\'' +
                ", userId='" + userId + '\'' +
                ", roleId='" + roleId + '\'' +
                '}';
    }
}
