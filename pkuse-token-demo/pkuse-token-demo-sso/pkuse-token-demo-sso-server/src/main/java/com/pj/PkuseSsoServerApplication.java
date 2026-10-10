package com.pj;

import com.bdsoft.pkusetoken.sso.PkuseSsoManager;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PkuseSsoServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(PkuseSsoServerApplication.class, args);

		System.out.println();
		System.out.println("---------------------- Sa-Token SSO 统一认证中心启动成功 ----------------------");
		System.out.println("配置信息：" + PkuseSsoManager.getServerConfig());
		System.out.println("统一认证登录地址：http://sa-sso-server.com:9000/sso/auth");
		System.out.println("测试前需要根据官网文档修改 hosts 文件，测试账号密码：sa / 123456");
		System.out.println();
	}
	
}