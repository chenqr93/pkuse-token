package com.pj;

import com.bdsoft.pkusetoken.oauth2.PkuseOAuth2Manager;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 启动：Sa-OAuth2 Server端
 * @author click33
 */
@SpringBootApplication
public class PkuseOAuth2ServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(PkuseOAuth2ServerApplication.class, args);
		System.out.println("\nSa-Token-OAuth2 Server端启动成功，配置如下：");
		System.out.println(PkuseOAuth2Manager.getServerConfig());
	}
	
}
