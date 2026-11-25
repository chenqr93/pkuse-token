package com.bdsoft.authcenter.dto.authz;

import com.bdsoft.authcenter.common.pojo.PageQueryDTO;

/**
 * 用户授权-用户分页查询参数（DTO；用户数据实时取自用户中心）
 */
public class UserQueryDTO extends PageQueryDTO {

    /** 登录账号（模糊匹配，可空） */
    private String username;

    /** 昵称（模糊匹配，可空） */
    private String nickname;

    /** 状态：0=禁用 / 1=启用（可空表示全部） */
    private Integer status;

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
}
