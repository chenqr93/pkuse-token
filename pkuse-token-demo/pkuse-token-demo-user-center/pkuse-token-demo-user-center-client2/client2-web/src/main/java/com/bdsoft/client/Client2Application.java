package com.bdsoft.client;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * SSO 客户端示例2：模式二对接（跨域 + 同 Redis）
 *
 * <p> 部署在 127.0.0.1（与认证中心 localhost 跨域），Cookie 不共享； </p>
 * <p> 与认证中心使用同一个 Redis：ticket 直连 Redis 校验（is-http=false）。 </p>
 */
@SpringBootApplication
public class Client2Application {

    public static void main(String[] args) {
        SpringApplication.run(Client2Application.class, args);
        System.out.println("\n启动成功：客户端2（模式二：跨域 + 同 Redis）http://127.0.0.1:9002");
    }

}
