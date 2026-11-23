package com.bdsoft.usercenter.auth.bo;

import com.bdsoft.usercenter.user.po.UcUser;

/**
 * 登录结果（业务对象，Service 层内部使用）
 *
 * <p> 封装「登录用户 + 是否需修改初始密码」的判定结果，由 Controller 转换为 {@code LoginVO} 返回前端。 </p>
 */
public class LoginResultBO {

    /** 登录用户（密码字段已清空） */
    private final UcUser user;

    /** 是否需要修改密码（规则配置 - 首次登录强制修改密码） */
    private final Boolean mustChangePassword;

    public LoginResultBO(UcUser user, Boolean mustChangePassword) {
        this.user = user;
        this.mustChangePassword = mustChangePassword;
    }

    public UcUser getUser() {
        return user;
    }

    public Boolean getMustChangePassword() {
        return mustChangePassword;
    }

    @Override
    public String toString() {
        return "LoginResultBO{" +
                "user=" + user +
                ", mustChangePassword=" + mustChangePassword +
                '}';
    }
}
