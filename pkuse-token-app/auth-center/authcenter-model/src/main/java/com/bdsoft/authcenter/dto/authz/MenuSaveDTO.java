package com.bdsoft.authcenter.dto.authz;

import java.io.Serializable;

/**
 * 菜单新增/修改入参（DTO）
 */
public class MenuSaveDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键（修改时必填） */
    private String id;

    /** 归属应用标识 */
    private String clientId;

    /** 父菜单 ID（顶级为 '0'，可空默认根） */
    private String parentId;

    /** 菜单名称 */
    private String menuName;

    /** 菜单类型：0=目录 1=页面 2=按钮 */
    private Integer menuType;

    /** 路由地址（目录/页面） */
    private String path;

    /** 前端组件路径（页面） */
    private String component;

    /** 权限标识（按钮必填） */
    private String perms;

    /** 图标 */
    private String icon;

    /** 同级排序 */
    private Integer sort;

    /** 显示状态：0=显示 1=隐藏 */
    private Integer visible;

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

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public String getMenuName() {
        return menuName;
    }

    public void setMenuName(String menuName) {
        this.menuName = menuName;
    }

    public Integer getMenuType() {
        return menuType;
    }

    public void setMenuType(Integer menuType) {
        this.menuType = menuType;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getComponent() {
        return component;
    }

    public void setComponent(String component) {
        this.component = component;
    }

    public String getPerms() {
        return perms;
    }

    public void setPerms(String perms) {
        this.perms = perms;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public Integer getSort() {
        return sort;
    }

    public void setSort(Integer sort) {
        this.sort = sort;
    }

    public Integer getVisible() {
        return visible;
    }

    public void setVisible(Integer visible) {
        this.visible = visible;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
