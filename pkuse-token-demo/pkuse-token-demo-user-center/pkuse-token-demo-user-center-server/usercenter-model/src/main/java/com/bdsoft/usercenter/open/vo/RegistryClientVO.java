package com.bdsoft.usercenter.open.vo;

import com.bdsoft.usercenter.client.po.UcClient;

import java.io.Serializable;

/**
 * 应用注册表条目（服务间开放接口 /open/registry/clients 返回）
 *
 * <p> 供授权中心同步应用档案（含密钥，用于其分发接口的签名校验），仅系统级调用方可见。 </p>
 */
public class RegistryClientVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 应用标识 */
    private String clientId;

    /** 应用名称 */
    private String clientName;

    /** 应用入口地址 */
    private String entryUrl;

    /** 接口密钥 */
    private String secretKey;

    /** 状态：0=禁用 / 1=启用 */
    private Integer status;

    /** 是否接收消息推送：0=否 / 1=是（授权中心授权变更推送目标） */
    private Integer isPush;

    /** 消息推送地址（is_push=1 时生效；授权中心据此推导各应用的缓存失效通知地址） */
    private String pushUrl;

    public static RegistryClientVO from(UcClient client) {
        if (client == null) {
            return null;
        }
        RegistryClientVO vo = new RegistryClientVO();
        vo.clientId = client.getClientId();
        vo.clientName = client.getClientName();
        vo.entryUrl = client.getEntryUrl();
        vo.secretKey = client.getSecretKey();
        vo.status = client.getStatus();
        vo.isPush = client.getIsPush();
        vo.pushUrl = client.getPushUrl();
        return vo;
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
}
