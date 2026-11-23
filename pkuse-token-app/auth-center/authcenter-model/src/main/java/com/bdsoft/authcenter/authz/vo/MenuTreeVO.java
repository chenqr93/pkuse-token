package com.bdsoft.authcenter.authz.vo;

import com.bdsoft.authcenter.authz.po.AcMenu;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 菜单树节点（VO：管理端菜单树 / 开放接口分发菜单树共用）
 */
public class MenuTreeVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;

    private String parentId;

    /** 菜单名称 */
    private String menuName;

    /** 菜单类型：0=目录 1=页面 2=按钮 */
    private Integer menuType;

    /** 路由地址 */
    private String path;

    /** 前端组件路径 */
    private String component;

    /** 权限标识 */
    private String perms;

    /** 图标 */
    private String icon;

    /** 同级排序 */
    private Integer sort;

    /** 状态：0=启用 1=禁用（管理视角返回；分发接口不返回） */
    private Integer status;

    /** 显示状态：0=显示 1=隐藏（管理视角返回；分发接口不返回） */
    private Integer visible;

    /** 子节点 */
    private List<MenuTreeVO> children = new ArrayList<>();

    public static MenuTreeVO from(AcMenu menu) {
        if (menu == null) {
            return null;
        }
        MenuTreeVO vo = new MenuTreeVO();
        vo.id = menu.getId();
        vo.parentId = menu.getParentId();
        vo.menuName = menu.getMenuName();
        vo.menuType = menu.getMenuType();
        vo.path = menu.getPath();
        vo.component = menu.getComponent();
        vo.perms = menu.getPerms();
        vo.icon = menu.getIcon();
        vo.sort = menu.getSort();
        vo.status = menu.getStatus();
        vo.visible = menu.getVisible();
        return vo;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Integer getVisible() {
        return visible;
    }

    public void setVisible(Integer visible) {
        this.visible = visible;
    }

    public List<MenuTreeVO> getChildren() {
        return children;
    }

    public void setChildren(List<MenuTreeVO> children) {
        this.children = children;
    }
}
