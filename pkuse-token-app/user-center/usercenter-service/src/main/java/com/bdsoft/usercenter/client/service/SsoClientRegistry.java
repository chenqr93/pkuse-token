package com.bdsoft.usercenter.client.service;

import com.bdsoft.usercenter.client.po.UcClient;

import java.util.Set;

/**
 * SSO 客户端配置同步器
 *
 * <p>
 * 「应用管理」维护在数据库表 uc_client 中，本接口负责把数据库中的启用应用同步到
 * Pkuse-Token 的 SSO-Server 全局客户端配置，使应用的新增/修改/禁用/删除即时生效
 * （无需改配置文件、无需重启）。
 * </p>
 *
 * <p>
 * 同步规则：以数据库为准；数据库中禁用的应用会从客户端列表中移除；
 * yml 中通过 sa-token.sso-server.clients 配置的应用作为补充（同名时数据库覆盖）。
 * </p>
 */
public interface SsoClientRegistry {

    /**
     * 把数据库中的应用同步到 SSO-Server 全局客户端配置（应用变更后调用）
     *
     * @return 本次同步生效的应用数量
     */
    int sync();

    /** 校验应用配置是否合法（回调地址格式等），非法时抛出业务异常 */
    void validate(UcClient client);

    /** 当前已生效（已同步到 SSO）的应用标识集合，仅用于页面展示 */
    Set<String> registeredClientIds();

}
