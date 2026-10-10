package com.pj;

import com.bdsoft.pkusetoken.PkuseManager;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


/**
 * Sa-Token 测试  
 * @author click33
 *
 */
@SpringBootApplication
public class PkuseTokenDeviceLockApplication {

	public static void main(String[] args) {
		SpringApplication.run(PkuseTokenDeviceLockApplication.class, args);
		System.out.println("\n启动成功：Sa-Token配置如下：" + PkuseManager.getConfig());
	}

}
