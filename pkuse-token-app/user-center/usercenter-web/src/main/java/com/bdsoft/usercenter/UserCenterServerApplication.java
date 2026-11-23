package com.bdsoft.usercenter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Pkuse-Token 用户中心示例（后端）
 *
 * <p>
 * 功能：用户管理、应用（Client）管理、统一登录认证（作为 SSO-Server 认证中心）；
 * 登录安全：登录日志（按月分表）、密码错误锁定与在线解锁、滑块验证码、设备锁与设备指纹。<br>
 * 技术栈：SpringBoot3 + KingbaseES + MyBatis-Plus + Pkuse-Token
 * </p>
 */
@SpringBootApplication
@EnableScheduling
public class UserCenterServerApplication {

    private static final Logger log = LoggerFactory.getLogger(UserCenterServerApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(UserCenterServerApplication.class, args);
        log.info("启动成功：用户中心后端 http://localhost:9000");
    }

}
