package com.bdsoft.authcenter.authz.bo;

import java.io.Serializable;

/**
 * 应用注册表条目（BO，来自用户中心 /open/registry/clients，缓存于 AppRegistryCache）
 *
 * <p> 应用档案不落地：授权中心仅以本对象（内存 + Redis 快照）感知应用清单与密钥。 </p>
 */
public class RegistryApp implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 应用状态：禁用（同步自用户中心 uc_client.status=0） */
    public static final int STATUS_DISABLED = 0;

    /** 应用状态：启用（同步自用户中心 uc_client.status=1） */
    public static final int STATUS_ENABLED = 1;

    /** 应用标识 */
    private String clientId;

    /** 应用名称 */
    private String clientName;

    /** 应用入口地址 */
    private String entryUrl;

    /** 接口密钥（分发接口签名校验依据） */
    private String secretKey;

    /** 状态：0=禁用 1=启用 */
    private Integer status;

    /** 是否接收授权变更推送：0=否 1=是（推送目标地址由 pushUrl 推导） */
    private Integer isPush;

    /** 消息推送地址（来自用户中心应用登记，如 http://127.0.0.1:9003/sso/pushC） */
    private String pushUrl;

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

    public String getEntryUrl() {
        return entryUrl;
    }

    public void setEntryUrl(String entryUrl) {
        this.entryUrl = entryUrl;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Integer getIsPush() {
        return isPush;
    }

    public void setIsPush(Integer isPush) {
        this.isPush = isPush;
    }

    public String getPushUrl() {
        return pushUrl;
    }

    public void setPushUrl(String pushUrl) {
        this.pushUrl = pushUrl;
    }

    @Override
    public String toString() {
        return "RegistryApp{clientId='" + clientId + "', clientName='" + clientName
                + "', status=" + status + "}";
    }
}
