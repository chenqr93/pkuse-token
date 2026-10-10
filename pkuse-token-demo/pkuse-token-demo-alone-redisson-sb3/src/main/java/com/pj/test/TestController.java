package com.pj.test;

import com.bdsoft.pkusetoken.stp.StpUtil;
import com.bdsoft.pkusetoken.util.PkuseResult;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 测试专用Controller，演示 alone-redisson 在 Spring Boot 3 下权限缓存与业务缓存分离
 *
 * @author click33
 */
@RestController
@RequestMapping("/test/")
public class TestController {

	@Autowired
	RedissonClient redissonClient;

	// 测试 Sa-Token 缓存，浏览器访问： http://localhost:8087/test/login
	@RequestMapping("login")
	public PkuseResult login(@RequestParam(defaultValue = "10001") String id) {
		System.out.println("--------------- 测试Sa-Token缓存");
		StpUtil.login(id);
		return PkuseResult.ok();
	}

	// 测试业务缓存，浏览器访问： http://localhost:8087/test/test
	@RequestMapping("test")
	public PkuseResult test() {
		System.out.println("--------------- 测试业务缓存");
		redissonClient.getBucket("hello").set("Hello World");
		return PkuseResult.ok();
	}

}
