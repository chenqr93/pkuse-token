package com.bdsoft.usercenter.client.dto;

/**
 * 应用（SSO Client）新增 / 修改参数（数据传输对象）
 *
 * <p> 仅包含前端允许提交的字段：创建/更新时间、已授权用户数等展示字段不由前端指定。 </p>
 */
public class ClientSaveDTO {

    /** 应用 ID（修改时必填；新增时忽略） */
    private String id;

    /** 应用标识（SSO client 参数） */
    private String clientId;

    /** 应用名称 */
    private String clientName;

    /** 允许的授权回调地址，多个用逗号分隔，* 表示全部放行 */
    private String allowUrl;

    /** 应用入口地址（工作台点击进入应用时跳转的目标，通常为应用的 /sso/login?back=首页） */
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

    @Override
    public String toString() {
        return "ClientSaveDTO{" +
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
                '}';
    }
}
