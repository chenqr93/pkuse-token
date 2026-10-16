package com.bdsoft.client;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * SSO 客户端示例4：NoSdk 模式对接（不依赖 Pkuse-Token 客户端 SDK，手动实现协议）
 *
 * <p> 本项目不引入任何 Pkuse-Token 依赖，SSO 协议全部手动实现： </p>
 * <ul>
 *   <li>拼装认证中心授权地址 /sso/auth?client=xxx&amp;redirect=xxx 并发起重定向；</li>
 *   <li>调用认证中心 /sso/pushS 完成 checkTicket（校验 ticket 换 loginId）、signout（单点注销）、
 *       userinfo（拉取用户资料）；</li>
 *   <li>参数签名：参数按 key 字典序拼接后追加 &amp;key={secretKey} 取 MD5，并携带 timestamp、nonce；</li>
 *   <li>接收认证中心推送 /sso/pushC（校验签名，处理 logoutCall 单点注销回调）；</li>
 *   <li>本地会话自行维护（内存存储 token → loginId，前端通过 satoken 请求头携带）。</li>
 * </ul>
 */
@SpringBootApplication
public class Client4Application {

    public static void main(String[] args) {
        SpringApplication.run(Client4Application.class, args);
        System.out.println("\n启动成功：客户端4（NoSdk：不依赖 SDK，手动实现协议）http://127.0.0.1:9004");
    }

}
