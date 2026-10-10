package com.pj;

import com.bdsoft.pkusetoken.PkuseManager;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PkuseTokenJwtDemoApplication {
	
	public static void main(String[] args) {
		SpringApplication.run(PkuseTokenJwtDemoApplication.class, args); 
		System.out.println("\n启动成功：Sa-Token配置如下：" + PkuseManager.getConfig());
	}

}