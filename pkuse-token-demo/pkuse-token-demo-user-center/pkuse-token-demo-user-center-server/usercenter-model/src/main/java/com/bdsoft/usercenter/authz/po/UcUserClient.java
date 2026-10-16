package com.bdsoft.usercenter.authz.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.bdsoft.usercenter.common.pojo.AbstractAuditPO;


/**
 * 用户中心-用户与应用授权绑定表（uc_user_client）
 *
 * <p> 普通用户只能进入已授权的应用；系统管理员（role=admin）不受限制。 </p>
 */
@TableName("uc_user_client")
public class UcUserClient extends AbstractAuditPO {

    private static final long serialVersionUID = 1L;

    /** 主键（32 位 UUID 字符串） */
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    /** 用户 ID（uc_user.id） */
    private String userId;

    /** 应用标识（uc_client.client_id） */
    private String clientId;

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

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    @Override
    public String toString() {
        return "UcUserClient{" + super.toString() +
                ", id='" + id + '\'' +
                ", userId='" + userId + '\'' +
                ", clientId='" + clientId + '\'' +
                '}';
    }
}
