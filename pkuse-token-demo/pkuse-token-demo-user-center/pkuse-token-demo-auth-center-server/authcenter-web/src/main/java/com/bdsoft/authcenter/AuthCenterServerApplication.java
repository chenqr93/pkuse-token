package com.bdsoft.authcenter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Pkuse-Token 授权中心示例（后端）
 *
 * <p>
 * 定位：面向多应用的统一权限（RBAC）管理中心——应用菜单管理、角色管理、用户授权、
 * 授权数据分发（签名开放接口 /open/authz/*）；应用档案与用户档案不落地，
 * 实时经接口取自用户中心（登录亦走用户中心 SSO，双模式 simple/ticket 可配）。<br>
 * 技术栈：SpringBoot3 + KingbaseES（独立库）+ MyBatis-Plus + Pkuse-Token(SSO-Client)
 * </p>
 */
@SpringBootApplication
@EnableScheduling
public class AuthCenterServerApplication {

    private static final Logger log = LoggerFactory.getLogger(AuthCenterServerApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(AuthCenterServerApplication.class, args);
        log.info("启动成功：授权中心后端 http://localhost:9010");
    }
}
