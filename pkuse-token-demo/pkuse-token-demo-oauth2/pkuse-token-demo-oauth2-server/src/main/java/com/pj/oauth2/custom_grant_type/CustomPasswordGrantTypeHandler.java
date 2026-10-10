//package com.pj.oauth2.custom_grant_type;
//
//import com.bdsoft.pkusetoken.oauth2.exception.PkuseOAuth2Exception;
//import com.bdsoft.pkusetoken.oauth2.granttype.handler.PasswordGrantTypeHandler;
//import com.bdsoft.pkusetoken.oauth2.granttype.handler.model.PasswordAuthResult;
//import org.springframework.stereotype.Component;
//
///**
// * 自定义 Password Grant_Type 授权模式处理器认证过程
// *
// * @author click33
// * @since 2025/5/11
// */
//@Component
//public class CustomPasswordGrantTypeHandler extends PasswordGrantTypeHandler {
//
//    @Override
//    public PasswordAuthResult loginByUsernamePassword(String username, String password) {
//        if("sa".equals(username) && "123456".equals(password)) {
//            long userId = 10001;
//            return new PasswordAuthResult(userId);
//        } else {
//            throw new PkuseOAuth2Exception("无效账号密码");
//        }
//    }
//
//}