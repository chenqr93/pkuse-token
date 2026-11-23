package com.bdsoft.usercenter.authz.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.bdsoft.usercenter.client.po.UcClient;
import com.bdsoft.usercenter.authz.po.UcUserClient;

import java.util.List;
import java.util.Map;

/**
 * 用户-应用授权绑定 Service
 *
 * <p> 维护「普通用户可访问哪些应用」：工作台只展示已授权应用入口，统一登录时校验授权。 </p>
 */
public interface UcUserClientService extends IService<UcUserClient> {

    /** 查询用户已授权的应用标识集合 */
    List<String> listClientIdsByUserId(String userId);

    /** 覆盖式保存用户的应用授权（clientIds 为空表示清空授权） */
    void bindClients(String userId, List<String> clientIds);

    /**
     * 批量授权：为多个用户授予多个应用
     *
     * @param userIds   目标用户 ID 集合
     * @param clientIds 应用标识集合
     * @param append    true=追加授权（保留原有） / false=覆盖授权（重置为本次集合）
     * @return 实际处理的用户数
     */
    int batchBindClients(List<String> userIds, List<String> clientIds, boolean append);

    /** 查询用户已授权且启用的应用（供工作台应用入口） */
    List<UcClient> listAuthorizedEnabledClients(String userId);

    /** 判断用户是否已授权指定应用 */
    boolean isAuthorized(String userId, String clientId);

    /** 批量统计：用户 ID -> 授权应用数（供用户列表展示） */
    Map<String, Integer> countByUserIds(List<String> userIds);

    /** 批量统计：应用标识 -> 已授权用户数（供应用列表展示） */
    Map<String, Integer> countByClientIds(List<String> clientIds);

    /** 用户删除时清理其授权关系 */
    void removeByUserId(String userId);

    /** 应用删除时清理其授权关系 */
    void removeByClientId(String clientId);

    /** 应用标识变更时迁移授权关系 */
    void renameClientId(String oldClientId, String newClientId);

}
