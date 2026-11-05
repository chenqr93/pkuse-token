package com.bdsoft.authcenter.authz.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.bdsoft.authcenter.common.pojo.AbstractAuditPO;

/**
 * 授权中心-菜单表（ac_menu，按应用隔离）
 *
 * <p> 管理各接入应用的前端菜单与按钮档案（目录/页面/按钮三层），作为角色绑定与 </p>
 * <p> 权限分发的数据源。client_id 为外部引用（用户中心应用注册表），不建外键。 </p>
 */
@TableName("ac_menu")
public class AcMenu extends AbstractAuditPO {

    private static final long serialVersionUID = 1L;

    /** 菜单类型：目录 */
    public static final int TYPE_DIRECTORY = 0;

    /** 菜单类型：页面 */
    public static final int TYPE_PAGE = 1;

    /** 菜单类型：按钮 */
    public static final int TYPE_BUTTON = 2;

    /** 状态：启用 */
    public static final int STATUS_ENABLED = 0;

    /** 状态：禁用 */
    public static final int STATUS_DISABLED = 1;

    /** 显示状态：显示 */
    public static final int VISIBLE_YES = 0;

    /** 显示状态：隐藏（不进分发树，权限码仍参与鉴权） */
    public static final int VISIBLE_HIDDEN = 1;

    /** 顶级菜单的父 ID 约定值 */
    public static final String ROOT_PARENT_ID = "0";

    /** 主键（32 位 UUID 字符串） */
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    /** 归属应用标识（用户中心应用注册表中的 client_id） */
    private String clientId;

    /** 父菜单 ID（顶级为 '0'） */
    private String parentId;

    /** 菜单名称（应用内唯一） */
    private String menuName;

    /** 菜单类型：0=目录 1=页面 2=按钮 */
    private Integer menuType;

    /** 路由地址（目录/页面） */
    private String path;

    /** 前端组件路径（页面） */
    private String component;

    /** 权限标识（按钮必填，应用内唯一，如 user:add） */
    private String perms;

    /** 图标 */
    private String icon;

    /** 同级排序（升序） */
    private Integer sort;

    /** 状态：0=启用 1=禁用 */
    private Integer status;

    /** 显示状态：0=显示 1=隐藏（隐藏仍鉴权） */
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

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    @Override
    public String toString() {
        return "AcMenu{" + super.toString() +
                ", id='" + id + '\'' +
                ", clientId='" + clientId + '\'' +
                ", parentId='" + parentId + '\'' +
                ", menuName='" + menuName + '\'' +
                ", menuType=" + menuType +
                ", path='" + path + '\'' +
                ", component='" + component + '\'' +
                ", perms='" + perms + '\'' +
                ", sort=" + sort +
                ", status=" + status +
                ", visible=" + visible +
                '}';
    }
}
