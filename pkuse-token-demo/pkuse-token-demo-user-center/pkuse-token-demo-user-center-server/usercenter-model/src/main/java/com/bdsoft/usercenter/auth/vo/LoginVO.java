package com.bdsoft.usercenter.auth.vo;

import com.bdsoft.usercenter.user.vo.UserVO;
import java.io.Serializable;

/**
 * 登录结果视图对象（登录 / 新设备验证通过后返回给前端）
 */
public class LoginVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** token 值：前端存 localStorage，请求时放入 satoken 请求头 */
    private String token;

    /** 登录用户信息 */
    private UserVO user;

    /** 是否需要修改密码（规则配置 - 首次登录强制修改密码）：前端据此引导用户先修改密码 */
    private Boolean mustChangePassword;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public UserVO getUser() {
        return user;
    }

    public void setUser(UserVO user) {
        this.user = user;
    }

    public Boolean getMustChangePassword() {
        return mustChangePassword;
    }

    public void setMustChangePassword(Boolean mustChangePassword) {
        this.mustChangePassword = mustChangePassword;
    }

    @Override
    public String toString() {
        return "LoginVO{" +
                "token='" + token + '\'' +
                ", user=" + user +
                ", mustChangePassword=" + mustChangePassword +
                '}';
    }
}
