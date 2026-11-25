package com.bdsoft.usercenter.dto.user;

import com.bdsoft.usercenter.common.pojo.PageQueryDTO;
/**
 * 用户分页查询参数（数据传输对象）
 */
public class UserQueryDTO extends PageQueryDTO {

    /** 登录账号（模糊匹配，可空） */
    private String username;

    /** 昵称（模糊匹配，可空） */
    private String nickname;

    /** 状态：0=禁用 / 1=启用（可空表示全部） */
    private Integer status;

    /** 所属组织 ID（可空表示全部；精确匹配该组织，不含其下级组织的用户） */
    private String orgId;

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

    @Override
    public String toString() {
        return "UserQueryDTO{" + super.toString() +
                ", username='" + username + '\'' +
                ", nickname='" + nickname + '\'' +
                ", status=" + status +
                ", orgId='" + orgId + '\'' +
                '}';
    }
}
