package com.bdsoft.usercenter.open.vo;

import com.bdsoft.usercenter.user.po.UcUser;

import java.io.Serializable;

/**
 * 用户注册表条目（服务间开放接口 /open/registry/users 返回）
 *
 * <p> 供授权中心控制台查询用户（不落库，实时透传），不含密码等敏感字段。 </p>
 */
public class RegistryUserVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 用户 ID */
    private String id;

    /** 登录账号 */
    private String username;

    /** 昵称 */
    private String nickname;

    /** 平台角色：admin=系统管理员 / user=普通用户 */
    private String role;

    /** 状态：0=禁用 / 1=启用 */
    private Integer status;

    /** 所属组织 ID（uc_user.org_id，可为空表示未挂组织） */
    private String orgId;

    /** 所属组织名称（查询接口按 orgId 批量回填，非库字段） */
    private String orgName;

    /** 手机号（供业务应用同步用户副本，可为空） */
    private String phone;

    /** 邮箱（供业务应用同步用户副本，可为空） */
    private String email;

    /** 身份证号（明文，供业务应用按身份证匹配人员，可为空；库中为 SM4 密文，由 Controller 出口处解密回填） */
    private String idCard;

    public static RegistryUserVO from(UcUser user) {
        if (user == null) {
            return null;
        }
        RegistryUserVO vo = new RegistryUserVO();
        vo.id = user.getId();
        vo.username = user.getUsername();
        vo.nickname = user.getNickname();
        vo.role = user.getRole();
        vo.status = user.getStatus();
        vo.orgId = user.getOrgId();
        vo.phone = user.getPhone();
        vo.email = user.getEmail();
        // idCard 不在此处赋值：库中为 SM4 密文，由 Controller 出口处解密回填明文（防密文外泄）
        return vo;
    }

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

    public String getOrgId() {
        return orgId;
    }

    public void setOrgId(String orgId) {
        this.orgId = orgId;
    }

    public String getOrgName() {
        return orgName;
    }

    public void setOrgName(String orgName) {
        this.orgName = orgName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getIdCard() {
        return idCard;
    }

    public void setIdCard(String idCard) {
        this.idCard = idCard;
    }
}
