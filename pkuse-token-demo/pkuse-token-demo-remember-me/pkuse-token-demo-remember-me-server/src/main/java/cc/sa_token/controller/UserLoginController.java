package cc.sa_token.controller;

import com.bdsoft.pkusetoken.stp.PkuseTokenInfo;
import com.bdsoft.pkusetoken.stp.StpUtil;
import com.bdsoft.pkusetoken.util.PkuseResult;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/back/user")
public class UserLoginController {

    @RequestMapping("/login")
    public PkuseResult doLogin(String name, String pwd, Boolean remember) {
        if("zhang".equals(name) && "123456".equals(pwd)) {
            StpUtil.login(10001, remember);
            PkuseTokenInfo tokenInfo = StpUtil.getTokenInfo();
            return PkuseResult.ok()
                    .set("tokenName", tokenInfo.getTokenName())
                    .set("tokenValue", tokenInfo.getTokenValue());
        } else {
            return PkuseResult.error("登录失败");
        }
    }

    @RequestMapping("/state")
    public PkuseResult checkNowLoginState() {
        return PkuseResult.ok().setData(StpUtil.isLogin());
    }

    @RequestMapping("/logout")
    public PkuseResult doLogout() {
        StpUtil.logout();
        return PkuseResult.ok().setData(StpUtil.isLogin());
    }

}
