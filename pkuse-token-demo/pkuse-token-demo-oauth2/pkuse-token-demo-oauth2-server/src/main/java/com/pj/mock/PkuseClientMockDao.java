package com.pj.mock;

import com.bdsoft.pkusetoken.oauth2.consts.GrantType;
import com.bdsoft.pkusetoken.oauth2.data.model.loader.PkuseClientModel;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * PkuseClientModel 模拟查询操作
 *
 * @author click33
 * @since 2024/11/15
 */
@Component
public class PkuseClientMockDao {

    public List<PkuseClientModel> list;

    /**
     * 构造方法，添加三个模拟应用
     */
    public void init(){
        list = new ArrayList<>();

        // 模拟应用1
        PkuseClientModel client1 = new PkuseClientModel()
                .setClientId("1001")    // client id
                .setClientSecret("aaaa-bbbb-cccc-dddd-eeee")    // client 秘钥
                .addAllowRedirectUris("*")    // 所有允许授权的 url
                .addContractScopes("openid", "unionid", "userid", "userinfo", "oidc")    // 所有签约的权限
                .setSubjectId("1000001")   // 主体 id (可选)
                .addAllowGrantTypes(     // 所有允许的授权模式
                        GrantType.authorization_code, // 授权码式
                        GrantType.implicit,  // 隐藏式
                        GrantType.refresh_token,  // 刷新令牌
                        GrantType.password,  // 密码式
                        GrantType.client_credentials,  // 客户端模式
                        "phone_code"  // 自定义授权模式 手机号验证码登录
                );
        list.add(client1);

        // 模拟应用2
        PkuseClientModel client2 = new PkuseClientModel()
                .setClientId("1002")
                .setClientSecret("aaaa-bbbb-cccc-dddd-eeee")
                .addAllowRedirectUris("*")
                .addContractScopes("openid", "unionid", "userid", "userinfo", "oidc")
                .setSubjectId("1000001")   // 主体 id (可选)
                .addAllowGrantTypes(
                        GrantType.authorization_code,
                        GrantType.implicit,
                        GrantType.refresh_token,
                        GrantType.password,
                        GrantType.client_credentials
                );
        list.add(client2);

        // 模拟应用3
        PkuseClientModel client3 = new PkuseClientModel()
                .setClientId("1003")
                .setClientSecret("aaaa-bbbb-cccc-dddd-eeee")
                .addAllowRedirectUris("*")
                .addContractScopes("openid", "unionid", "userid", "userinfo", "oidc")
                .addAllowGrantTypes(
                        GrantType.authorization_code,
                        GrantType.implicit,
                        GrantType.refresh_token,
                        GrantType.password,
                        GrantType.client_credentials
                );
        list.add(client3);
    }

    /**
     * 根据应用 id 查找对应的应用，找不到则返回 null
     * @param clientId 应用 id
     * @return 应用对象
     */
    public PkuseClientModel getClientModel(String clientId) {
        if(list == null) {
            init();
        }
        return list.stream()
                .filter(e -> e.getClientId().equals(clientId))
                .findFirst()
                .orElse(null);
    }

}