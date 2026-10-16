package com.bdsoft.client;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * SSO 客户端示例3：模式三对接（跨域 + 跨 Redis）
 *
 * <p> 部署在 127.0.0.1（与认证中心 localhost 跨域），Cookie 不共享； </p>
 * <p> 使用自己的 Redis，ticket 通过 HTTP 接口向认证中心校验（is-http=true）， </p>
 * <p> 单点注销通过认证中心的消息推送（/sso/pushC）通知本应用。 </p>
 */
@SpringBootApplication
public class Client3Application {

    public static void main(String[] args) {
        SpringApplication.run(Client3Application.class, args);
        System.out.println("\n启动成功：客户端3（模式三：跨域 + 跨 Redis）http://127.0.0.1:9003");
    }

}
