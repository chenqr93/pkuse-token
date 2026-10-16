package com.bdsoft.usercenter.user.dto;

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

    @Override
    public String toString() {
        return "UserQueryDTO{" + super.toString() +
                ", username='" + username + '\'' +
                ", nickname='" + nickname + '\'' +
                ", status=" + status +
                '}';
    }
}
