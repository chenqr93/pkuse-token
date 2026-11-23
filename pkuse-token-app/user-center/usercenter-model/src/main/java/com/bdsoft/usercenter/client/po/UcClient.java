package com.bdsoft.usercenter.client.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.bdsoft.usercenter.common.pojo.AbstractAuditPO;


/**
 * 用户中心-接入应用（SSO Client）表（uc_client）
 *
 * <p> 表中的记录会在启动时/每次变更后同步到 Pkuse-Token 的 SSO-Server 全局客户端配置， </p>
 * <p> 从而让「应用管理」页面的配置即时生效。 </p>
 *
 * <p> 实体字段与表结构一一对应，仅在 DAO 层与 Service 层内部使用；对外返回由 ClientVO 承载。 </p>
 */
@TableName("uc_client")
public class UcClient extends AbstractAuditPO {

    private static final long serialVersionUID = 1L;

    /** 状态：启用 */
    public static final int STATUS_ENABLED = 1;

    /** 状态：禁用 */
    public static final int STATUS_DISABLED = 0;

    /** 是否接收消息推送：是 */
    public static final int IS_PUSH_YES = 1;

    /** 是否接收消息推送：否 */
    public static final int IS_PUSH_NO = 0;

    /** 是否参与单点注销：是 */
    public static final int IS_SLO_YES = 1;

    /** 是否参与单点注销：否 */
    public static final int IS_SLO_NO = 0;

    /** 默认消息推送地址 */
    public static final String DEFAULT_PUSH_URL = "/sso/pushC";

    /** 主键（32 位 UUID 字符串） */
    @TableId(type = IdType.ASSIGN_UUID)
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
        return "UcClient{" + super.toString() +
                ", id='" + id + '\'' +
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
