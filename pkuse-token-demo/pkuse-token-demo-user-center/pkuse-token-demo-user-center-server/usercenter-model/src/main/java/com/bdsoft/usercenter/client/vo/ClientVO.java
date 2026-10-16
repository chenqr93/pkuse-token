package com.bdsoft.usercenter.client.vo;

import com.bdsoft.usercenter.client.po.UcClient;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 应用（SSO Client）视图对象（Controller 返回给前端的响应对象）
 */
public class ClientVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键（序列化为字符串，避免前端 JS 精度丢失） */
    private String id;

    /** 应用标识（SSO client 参数） */
    private String clientId;

    /** 应用名称 */
    private String clientName;

    /** 允许的授权回调地址，多个用逗号分隔，* 表示全部放行 */
    private String allowUrl;

    /** 应用入口地址（工作台点击进入应用时跳转的目标） */
    private String entryUrl;

    /** 接口调用秘钥（SSO 参数签名/推送校验使用） */
    private String secretKey;

    /** 是否接收消息推送：0=否 / 1=是 */
    private Integer isPush;

    /** 消息推送地址（is_push=1 时生效） */
    private String pushUrl;

    /** 是否参与单点注销：0=否 / 1=是 */
    private Integer isSlo;

    /** 状态：0=禁用 / 1=启用 */
    private Integer status;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

    /** 已授权用户数（列表展示用，非数据库字段） */
    private Integer authorizedUserCount;

    /** 由实体转换为视图对象 */
    public static ClientVO from(UcClient client) {
        if (client == null) {
            return null;
        }
        ClientVO vo = new ClientVO();
        vo.setId(client.getId());
        vo.setClientId(client.getClientId());
        vo.setClientName(client.getClientName());
        vo.setAllowUrl(client.getAllowUrl());
        vo.setEntryUrl(client.getEntryUrl());
        vo.setSecretKey(client.getSecretKey());
        vo.setIsPush(client.getIsPush());
        vo.setPushUrl(client.getPushUrl());
        vo.setIsSlo(client.getIsSlo());
        vo.setStatus(client.getStatus());
        vo.setRemark(client.getRemark());
        vo.setCreateTime(client.getCreateTime());
        vo.setUpdateTime(client.getUpdateTime());
        return vo;
    }

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

    public String getAllowUrl() {
        return allowUrl;
    }

    public void setAllowUrl(String allowUrl) {
        this.allowUrl = allowUrl;
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

    public Integer getIsSlo() {
        return isSlo;
    }

    public void setIsSlo(Integer isSlo) {
        this.isSlo = isSlo;
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

    public Integer getAuthorizedUserCount() {
        return authorizedUserCount;
    }

    public void setAuthorizedUserCount(Integer authorizedUserCount) {
        this.authorizedUserCount = authorizedUserCount;
    }

    @Override
    public String toString() {
        return "ClientVO{" +
                "id='" + id + '\'' +
                ", clientId='" + clientId + '\'' +
                ", clientName='" + clientName + '\'' +
                ", allowUrl='" + allowUrl + '\'' +
                ", entryUrl='" + entryUrl + '\'' +
                ", isPush=" + isPush +
                ", pushUrl='" + pushUrl + '\'' +
                ", isSlo=" + isSlo +
                ", status=" + status +
                ", remark='" + remark + '\'' +
                ", createTime=" + createTime +
                ", updateTime=" + updateTime +
                ", authorizedUserCount=" + authorizedUserCount +
                '}';
    }
}
