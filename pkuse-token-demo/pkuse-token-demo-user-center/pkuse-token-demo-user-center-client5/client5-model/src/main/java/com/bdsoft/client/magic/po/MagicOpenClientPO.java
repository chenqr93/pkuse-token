package com.bdsoft.client.magic.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * magic-api 开放接口调用方（PO，表 s_open_client）
 *
 * <p> 在平台登记的「应用级」调用方（法治中台内部应用 / 外部第三方应用）， </p>
 * <p> 通过管理接口（/open/clients）动态增删改，调用 POST /open/token 以签名换取 pkuse-token 令牌。 </p>
 */
@TableName("s_open_client")
public class MagicOpenClientPO {

    /** 主键（32 位 UUID 字符串） */
    @TableId(value = "id", type = IdType.INPUT)
    private String id;

    /** 调用方标识（应用级会话 loginId 为 open:{clientId}；签发令牌按此吊销） */
    private String clientId;

    /** 显示名 */
    private String clientName;

    /** 签名密钥（下发调用方妥善保管；可重置） */
    private String secretKey;

    /** 签发令牌有效期（秒，默认 7200） */
    private Long timeoutSeconds;

    /** 状态：1=启用 / 0=禁用（禁用后无法换取新令牌，已有令牌吊销） */
    private Integer status;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public Long getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(Long timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
}
