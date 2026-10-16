package com.bdsoft.usercenter.authz.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bdsoft.usercenter.common.exception.BizException;
import com.bdsoft.usercenter.client.po.UcClient;
import com.bdsoft.usercenter.authz.po.UcUserClient;
import com.bdsoft.usercenter.client.mapper.UcClientMapper;
import com.bdsoft.usercenter.authz.mapper.UcUserClientMapper;
import com.bdsoft.usercenter.authz.service.UcUserClientService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 用户-应用授权绑定 Service 实现
 *
 * <p> 绑定以「用户 ID + 应用标识（client_id）」为唯一键；查询应用侧信息时直接使用 UcClientMapper，
 * 避免与 UcClientService 形成循环依赖。 </p>
 */
@Service
public class UcUserClientServiceImpl extends ServiceImpl<UcUserClientMapper, UcUserClient> implements UcUserClientService {

    @Resource
    private UcClientMapper clientMapper;

    @Override
    public List<String> listClientIdsByUserId(String userId) {
        if (userId == null) {
            return List.of();
        }
        return this.list(new LambdaQueryWrapper<UcUserClient>().eq(UcUserClient::getUserId, userId))
                .stream()
                .map(UcUserClient::getClientId)
                .filter(Objects::nonNull)
                .toList();
    }

    @Override
    public void bindClients(String userId, List<String> clientIds) {
        if (userId == null) {
            throw new BizException("用户 ID 不能为空");
        }
        // 覆盖式保存：先清理旧授权，再写入新授权
        this.remove(new LambdaQueryWrapper<UcUserClient>().eq(UcUserClient::getUserId, userId));
        if (clientIds == null || clientIds.isEmpty()) {
            return;
        }
        List<UcUserClient> relations = new ArrayList<>();
        for (String clientId : new LinkedHashSet<>(clientIds)) {
            if (clientId == null || clientId.isBlank()) {
                continue;
            }
            UcUserClient relation = new UcUserClient();
            relation.setUserId(userId);
            relation.setClientId(clientId.trim());
            relations.add(relation);
        }
        if (!relations.isEmpty()) {
            // 逐条保存（不用 saveBatch）：操作日志需要逐条捕获变更，BATCH 执行器会合并 prepare 漏掉后续语句
            relations.forEach(this::save);
        }
    }

    @Override
    public int batchBindClients(List<String> userIds, List<String> clientIds, boolean append) {
        if (userIds == null || userIds.isEmpty()) {
            throw new BizException("请先选择用户");
        }
        // 过滤非法/重复的应用标识
        List<String> targetClientIds = new ArrayList<>(new LinkedHashSet<>(
                (clientIds == null ? List.<String>of() : clientIds).stream()
                        .filter(id -> id != null && !id.isBlank())
                        .map(String::trim)
                        .toList()));
        // 校验应用是否存在（覆盖模式下不勾选任何应用表示清空授权，仍允许）
        if (!targetClientIds.isEmpty()) {
            long exists = clientMapper.selectCount(
                    new LambdaQueryWrapper<UcClient>().in(UcClient::getClientId, targetClientIds));
            if (exists != targetClientIds.size()) {
                throw new BizException("存在无效的应用标识，请刷新后重试");
            }
        }

        List<UcUserClient> toInsert = new ArrayList<>();
        int handledUsers = 0;
        for (String userId : new LinkedHashSet<>(userIds)) {
            if (userId == null) {
                continue;
            }
            if (append) {
                // 追加授权：仅写入尚未授权的关系
                Set<String> existsIds = new HashSet<>(listClientIdsByUserId(userId));
                for (String clientId : targetClientIds) {
                    if (existsIds.contains(clientId)) {
                        continue;
                    }
                    toInsert.add(newRelation(userId, clientId));
                }
            } else {
                // 覆盖授权：先清空原有授权，再写入本次勾选集合
                this.remove(new LambdaQueryWrapper<UcUserClient>().eq(UcUserClient::getUserId, userId));
                for (String clientId : targetClientIds) {
                    toInsert.add(newRelation(userId, clientId));
                }
            }
            handledUsers++;
        }
        if (!toInsert.isEmpty()) {
            // 逐条保存（不用 saveBatch）：操作日志需要逐条捕获变更，BATCH 执行器会合并 prepare 漏掉后续语句
            toInsert.forEach(this::save);
        }
        return handledUsers;
    }

    private static UcUserClient newRelation(String userId, String clientId) {
        UcUserClient relation = new UcUserClient();
        relation.setUserId(userId);
        relation.setClientId(clientId);
        return relation;
    }

    @Override
    public List<UcClient> listAuthorizedEnabledClients(String userId) {
        List<String> clientIds = listClientIdsByUserId(userId);
        if (clientIds.isEmpty()) {
            return List.of();
        }
        return clientMapper.selectList(new LambdaQueryWrapper<UcClient>()
                .in(UcClient::getClientId, clientIds)
                .eq(UcClient::getStatus, UcClient.STATUS_ENABLED)
                .orderByAsc(UcClient::getId));
    }

    @Override
    public boolean isAuthorized(String userId, String clientId) {
        if (userId == null || clientId == null || clientId.isBlank()) {
            return false;
        }
        return this.count(new LambdaQueryWrapper<UcUserClient>()
                .eq(UcUserClient::getUserId, userId)
                .eq(UcUserClient::getClientId, clientId)) > 0;
    }

    @Override
    public Map<String, Integer> countByUserIds(List<String> userIds) {
        Map<String, Integer> result = new HashMap<>();
        if (userIds == null || userIds.isEmpty()) {
            return result;
        }
        List<UcUserClient> relations = this.list(
                new LambdaQueryWrapper<UcUserClient>().in(UcUserClient::getUserId, userIds));
        for (UcUserClient relation : relations) {
            result.merge(relation.getUserId(), 1, Integer::sum);
        }
        return result;
    }

    @Override
    public Map<String, Integer> countByClientIds(List<String> clientIds) {
        Map<String, Integer> result = new HashMap<>();
        if (clientIds == null || clientIds.isEmpty()) {
            return result;
        }
        List<UcUserClient> relations = this.list(
                new LambdaQueryWrapper<UcUserClient>().in(UcUserClient::getClientId, clientIds));
        for (UcUserClient relation : relations) {
            result.merge(relation.getClientId(), 1, Integer::sum);
        }
        return result;
    }

    @Override
    public void removeByUserId(String userId) {
        if (userId == null) {
            return;
        }
        this.remove(new LambdaQueryWrapper<UcUserClient>().eq(UcUserClient::getUserId, userId));
    }

    @Override
    public void removeByClientId(String clientId) {
        if (clientId == null || clientId.isBlank()) {
            return;
        }
        this.remove(new LambdaQueryWrapper<UcUserClient>().eq(UcUserClient::getClientId, clientId));
    }

    @Override
    public void renameClientId(String oldClientId, String newClientId) {
        if (oldClientId == null || oldClientId.isBlank() || newClientId == null || newClientId.isBlank()
                || oldClientId.equals(newClientId)) {
            return;
        }
        UcUserClient update = new UcUserClient();
        update.setClientId(newClientId);
        this.update(update, new LambdaQueryWrapper<UcUserClient>().eq(UcUserClient::getClientId, oldClientId));
    }

}
