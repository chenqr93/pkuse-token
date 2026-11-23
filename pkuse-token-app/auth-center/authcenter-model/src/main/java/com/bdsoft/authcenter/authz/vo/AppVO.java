package com.bdsoft.authcenter.authz.vo;

import com.bdsoft.authcenter.authz.bo.RegistryApp;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 应用视图（VO：应用列表 = 注册表档案 + 本地授权统计回填）
 */
public class AppVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 应用标识 */
    private String clientId;

    /** 应用名称 */
    private String clientName;

    /** 应用入口地址 */
    private String entryUrl;

    /** 状态：0=禁用 1=启用（同步自用户中心） */
    private Integer status;

    /** 菜单数（本地统计） */
    private Long menuCount;

    /** 角色数（本地统计） */
    private Long roleCount;

    /** 已授权用户数（本地统计，按角色去重） */
    private Long userCount;

    /** 注册表最近刷新时间 */
    private LocalDateTime lastRefreshTime;

    /** 本次列表数据来源：live=实时 / cache=缓存兜底（用户中心不可用时） */
    private String source;

    public static AppVO from(RegistryApp app) {
        if (app == null) {
            return null;
        }
        AppVO vo = new AppVO();
        vo.clientId = app.getClientId();
        vo.clientName = app.getClientName();
        vo.entryUrl = app.getEntryUrl();
        vo.status = app.getStatus();
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

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Long getMenuCount() {
        return menuCount;
    }

    public void setMenuCount(Long menuCount) {
        this.menuCount = menuCount;
    }

    public Long getRoleCount() {
        return roleCount;
    }

    public void setRoleCount(Long roleCount) {
        this.roleCount = roleCount;
    }

    public Long getUserCount() {
        return userCount;
    }

    public void setUserCount(Long userCount) {
        this.userCount = userCount;
    }

    public LocalDateTime getLastRefreshTime() {
        return lastRefreshTime;
    }

    public void setLastRefreshTime(LocalDateTime lastRefreshTime) {
        this.lastRefreshTime = lastRefreshTime;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }
}
