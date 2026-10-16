package com.bdsoft.usercenter.user.dto;

/**
 * 用户新增 / 修改参数（数据传输对象）
 *
 * <p> 仅包含前端允许提交的字段：密码摘要、盐、失败计数、锁定状态、需改密标记等系统维护字段 </p>
 * <p> 由 Service 按「规则配置」决定，接收方无法直接指定。 </p>
 */
public class UserSaveDTO {

    /** 用户 ID（修改时必填；新增时忽略） */
    private String id;

    /** 登录账号（唯一） */
    private String username;

    /** 密码（仅新增时使用；为空时使用「规则配置 - 系统默认密码」） */
    private String password;

    /** 昵称 */
    private String nickname;

    /** 邮箱 */
    private String email;

    /** 手机号 */
    private String phone;

    /** 角色：admin=管理员 / user=普通用户 */
    private String role;

    /** 状态：0=禁用 / 1=启用 */
    private Integer status;

    /** 备注 */
    private String remark;

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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
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

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    @Override
    public String toString() {
        return "UserSaveDTO{" +
                "id='" + id + '\'' +
                ", username='" + username + '\'' +
                ", nickname='" + nickname + '\'' +
                ", email='" + email + '\'' +
                ", phone='" + phone + '\'' +
                ", role='" + role + '\'' +
                ", status=" + status +
                ", remark='" + remark + '\'' +
                '}';
    }
}
