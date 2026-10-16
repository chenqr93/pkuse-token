package com.bdsoft.usercenter.dashboard.vo;

import com.bdsoft.usercenter.user.vo.UserVO;
import java.io.Serializable;
import java.util.Set;

/**
 * 工作台总览视图对象（仅系统管理员）
 */
public class DashboardOverviewVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 用户总数（不含内置管理员账号） */
    private Long userCount;

    /** 启用状态的用户数 */
    private Long enabledUserCount;

    /** 应用总数 */
    private Long clientCount;

    /** 启用状态的应用数 */
    private Long enabledClientCount;

    /** 当前在线人数（Redis 中持有有效登录会话的账号数） */
    private Long onlineUserCount;

    /** 当前登录用户 */
    private UserVO currentUser;

    /** 服务器时间（yyyy-MM-dd HH:mm:ss） */
    private String serverTime;

    /** 当前已同步到 SSO 认证中心的应用标识 */
    private Set<String> registeredClients;

    /** SSO 相关配置 */
    private SsoServerInfoVO ssoServer;

    public Long getUserCount() {
        return userCount;
    }

    public void setUserCount(Long userCount) {
        this.userCount = userCount;
    }

    public Long getEnabledUserCount() {
        return enabledUserCount;
    }

    public void setEnabledUserCount(Long enabledUserCount) {
        this.enabledUserCount = enabledUserCount;
    }

    public Long getClientCount() {
        return clientCount;
    }

    public void setClientCount(Long clientCount) {
        this.clientCount = clientCount;
    }

    public Long getEnabledClientCount() {
        return enabledClientCount;
    }

    public void setEnabledClientCount(Long enabledClientCount) {
        this.enabledClientCount = enabledClientCount;
    }

    public Long getOnlineUserCount() {
        return onlineUserCount;
    }

    public void setOnlineUserCount(Long onlineUserCount) {
        this.onlineUserCount = onlineUserCount;
    }

    public UserVO getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(UserVO currentUser) {
        this.currentUser = currentUser;
    }

    public String getServerTime() {
        return serverTime;
    }

    public void setServerTime(String serverTime) {
        this.serverTime = serverTime;
    }

    public Set<String> getRegisteredClients() {
        return registeredClients;
    }

    public void setRegisteredClients(Set<String> registeredClients) {
        this.registeredClients = registeredClients;
    }

    public SsoServerInfoVO getSsoServer() {
        return ssoServer;
    }

    public void setSsoServer(SsoServerInfoVO ssoServer) {
        this.ssoServer = ssoServer;
    }

    @Override
    public String toString() {
        return "DashboardOverviewVO{" +
                "userCount=" + userCount +
                ", enabledUserCount=" + enabledUserCount +
                ", clientCount=" + clientCount +
                ", enabledClientCount=" + enabledClientCount +
                ", onlineUserCount=" + onlineUserCount +
                ", currentUser=" + currentUser +
                ", serverTime='" + serverTime + '\'' +
                ", registeredClients=" + registeredClients +
                ", ssoServer=" + ssoServer +
                '}';
    }
}
