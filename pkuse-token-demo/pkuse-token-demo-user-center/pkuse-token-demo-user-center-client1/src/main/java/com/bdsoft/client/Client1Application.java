package com.bdsoft.client;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * SSO 客户端示例1：模式一对接（同域 + 同 Redis）
 *
 * <p> 与认证中心（用户中心）同为 localhost，浏览器同域 Cookie 共享； </p>
 * <p> 且与认证中心使用同一个 Redis，会话数据直接共享，登录无需 ticket。 </p>
 */
@SpringBootApplication
public class Client1Application {

    public static void main(String[] args) {
        SpringApplication.run(Client1Application.class, args);
        System.out.println("\n启动成功：客户端1（模式一：同域 + 同 Redis）http://localhost:9001");
    }

}
