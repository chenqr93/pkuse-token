package com.bdsoft.authcenter.authz.vo;

import java.io.Serializable;

/**
 * 概览统计（VO，全部本地查询）
 */
public class OverviewVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 接入应用数（注册表启用数） */
    private Long appCount;

    /** 菜单总数 */
    private Long menuCount;

    /** 角色总数 */
    private Long roleCount;

    /** 已授权用户数（ac_user_role 去重 user_id） */
    private Long userCount;

    /** 当前 SSO 对接模式：simple / ticket */
    private String ssoMode;

    /** 注册表最近刷新时间 */
    private String lastRefreshTime;

    public Long getAppCount() {
        return appCount;
    }

    public void setAppCount(Long appCount) {
        this.appCount = appCount;
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

    public String getSsoMode() {
        return ssoMode;
    }

    public void setSsoMode(String ssoMode) {
        this.ssoMode = ssoMode;
    }

    public String getLastRefreshTime() {
        return lastRefreshTime;
    }

    public void setLastRefreshTime(String lastRefreshTime) {
        this.lastRefreshTime = lastRefreshTime;
    }
}
