package com.bdsoft.client;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * SSO 客户端示例5：模式三对接（跨域 + 跨 Redis，client3 的第二实例）
 *
 * <p> 部署在 127.0.0.1（与认证中心 localhost 跨域），Cookie 不共享； </p>
 * <p> 使用自己的 Redis（db7，独立于认证中心 db5 与客户端3 db6），ticket 通过 HTTP 接口向认证中心校验（is-http=true）， </p>
 * <p> 单点注销通过认证中心的消息推送（/sso/pushC）通知本应用。 </p>
 */
@SpringBootApplication
public class Client5Application {

    public static void main(String[] args) {
        SpringApplication.run(Client5Application.class, args);
        System.out.println("\n启动成功：客户端5（模式三：跨域 + 跨 Redis）http://127.0.0.1:9005");
    }

}
