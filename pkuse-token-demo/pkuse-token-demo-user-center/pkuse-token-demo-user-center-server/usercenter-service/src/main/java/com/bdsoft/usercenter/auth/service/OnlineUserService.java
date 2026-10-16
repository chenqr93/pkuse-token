package com.bdsoft.usercenter.auth.service;

import java.util.List;

/**
 * 在线用户 Service
 *
 * <p> 「在线」= 在 Redis 中持有有效登录会话（Account-Session）的账号：登录时创建、退出/被踢/删除时销毁， </p>
 * <p> 按登录 ID 天然去重（同一账号多端登录只算一人）；会话由 Pkuse-Token 统一存放在共享 Redis 中， </p>
 * <p> 因此接入的 SSO 应用（模式一/二共享会话）上的登录同样计入。 </p>
 */
public interface OnlineUserService {

    /**
     * 当前在线的用户 ID 列表（已过滤掉账号已不存在的残留会话）
     */
    List<String> onlineUserIds();

    /** 当前在线人数（在线用户 ID 中仍存在于 uc_user 表的数量） */
    long onlineUserCount();

}
