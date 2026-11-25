package com.bdsoft.authcenter.vo.authz;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 用户在各应用的角色分组视图（VO：分配角色弹窗数据）
 */
public class UserRolesVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 用户 ID */
    private String userId;

    /** 登录账号 */
    private String username;

    /** 昵称 */
    private String nickname;

    /** 按应用分组 */
    private List<Group> groups = new ArrayList<>();

    /** 应用分组：应用 + 可选角色 + 已选角色 */
    public static class Group implements Serializable {

        private static final long serialVersionUID = 1L;

        /** 应用标识 */
        private String clientId;

        /** 应用名称 */
        private String clientName;

        /** 用户是否已获该应用入口授权（用户中心 uc_user_client；未开通时前端提示） */
        private Boolean entryGranted;

        /** 该应用下当前用户已绑定的角色 ID 列表 */
        private List<String> boundRoleIds = new ArrayList<>();

        /** 该应用可选角色（启用） */
        private List<RoleVO> roles = new ArrayList<>();

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

        public Boolean getEntryGranted() {
            return entryGranted;
        }

        public void setEntryGranted(Boolean entryGranted) {
            this.entryGranted = entryGranted;
        }

        public List<String> getBoundRoleIds() {
            return boundRoleIds;
        }

        public void setBoundRoleIds(List<String> boundRoleIds) {
            this.boundRoleIds = boundRoleIds;
        }

        public List<RoleVO> getRoles() {
            return roles;
        }

        public void setRoles(List<RoleVO> roles) {
            this.roles = roles;
        }
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public List<Group> getGroups() {
        return groups;
    }

    public void setGroups(List<Group> groups) {
        this.groups = groups;
    }
}
