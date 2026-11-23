package com.bdsoft.authcenter.usercenter.bo;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户中心用户条目（BO，实时接口返回，不落库）
 */
public class RegistryUser implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 用户 ID（用户中心主键） */
    private String id;

    /** 登录账号 */
    private String username;

    /** 昵称 */
    private String nickname;

    /** 平台角色：admin=系统管理员 / user=普通用户 */
    private String role;

    /** 状态：0=禁用 1=启用 */
    private Integer status;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "RegistryUser{id='" + id + "', username='" + username + "', role='" + role + "'}";
    }

    /**
     * 用户分页结果（BO）：承载用户中心分页接口的响应
     */
    public static class Page implements Serializable {

        private static final long serialVersionUID = 1L;

        /** 总条数 */
        private Long total;

        /** 当前页 */
        private Long current;

        /** 每页条数 */
        private Long size;

        /** 当前页记录 */
        private List<RegistryUser> records;

        public Long getTotal() {
            return total;
        }

        public void setTotal(Long total) {
            this.total = total;
        }

        public Long getCurrent() {
            return current;
        }

        public void setCurrent(Long current) {
            this.current = current;
        }

        public Long getSize() {
            return size;
        }

        public void setSize(Long size) {
            this.size = size;
        }

        public List<RegistryUser> getRecords() {
            return records;
        }

        public void setRecords(List<RegistryUser> records) {
            this.records = records;
        }
    }
}
