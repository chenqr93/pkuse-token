package com.bdsoft.usercenter.dashboard.vo;

import java.io.Serializable;

/**
 * SSO 认证中心配置视图对象（工作台展示用）
 */
public class SsoServerInfoVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** ticket 有效期（秒） */
    private Long ticketTimeout;

    /** 是否校验接口签名 */
    private Boolean checkSign;

    public SsoServerInfoVO() {
    }

    public SsoServerInfoVO(Long ticketTimeout, Boolean checkSign) {
        this.ticketTimeout = ticketTimeout;
        this.checkSign = checkSign;
    }

    public Long getTicketTimeout() {
        return ticketTimeout;
    }

    public void setTicketTimeout(Long ticketTimeout) {
        this.ticketTimeout = ticketTimeout;
    }

    public Boolean getCheckSign() {
        return checkSign;
    }

    public void setCheckSign(Boolean checkSign) {
        this.checkSign = checkSign;
    }

    @Override
    public String toString() {
        return "SsoServerInfoVO{" +
                "ticketTimeout=" + ticketTimeout +
                ", checkSign=" + checkSign +
                '}';
    }
}
