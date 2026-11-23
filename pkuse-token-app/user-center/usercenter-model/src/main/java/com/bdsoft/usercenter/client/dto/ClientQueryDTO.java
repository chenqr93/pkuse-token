package com.bdsoft.usercenter.client.dto;

import com.bdsoft.usercenter.common.pojo.PageQueryDTO;
/**
 * 应用（SSO Client）分页查询参数（数据传输对象）
 */
public class ClientQueryDTO extends PageQueryDTO {

    /** 应用标识（模糊匹配，可空） */
    private String clientId;

    /** 应用名称（模糊匹配，可空） */
    private String clientName;

    /** 状态：0=禁用 / 1=启用（可空表示全部） */
    private Integer status;

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

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "ClientQueryDTO{" + super.toString() +
                ", clientId='" + clientId + '\'' +
                ", clientName='" + clientName + '\'' +
                ", status=" + status +
                '}';
    }
}
