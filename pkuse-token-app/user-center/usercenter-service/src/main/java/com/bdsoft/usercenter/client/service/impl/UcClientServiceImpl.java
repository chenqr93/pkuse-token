package com.bdsoft.usercenter.client.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bdsoft.usercenter.common.exception.BizException;
import com.bdsoft.usercenter.client.dto.ClientQueryDTO;
import com.bdsoft.usercenter.client.dto.ClientSaveDTO;
import com.bdsoft.usercenter.client.po.UcClient;
import com.bdsoft.usercenter.client.mapper.UcClientMapper;
import com.bdsoft.usercenter.client.service.SsoClientRegistry;
import com.bdsoft.usercenter.client.service.UcClientService;
import com.bdsoft.usercenter.authz.service.UcUserClientService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 应用（SSO Client）管理 Service 实现
 *
 * <p> 所有写操作完成后都会调用 {@link SsoClientRegistry#sync()}，让 SSO 配置即时生效。 </p>
 */
@Service
public class UcClientServiceImpl extends ServiceImpl<UcClientMapper, UcClient> implements UcClientService {

    @Resource
    private SsoClientRegistry ssoClientRegistry;

    @Resource
    private UcUserClientService userClientService;

    @Override
    public Page<UcClient> pageQuery(ClientQueryDTO query) {
        LambdaQueryWrapper<UcClient> wrapper = new LambdaQueryWrapper<UcClient>()
                .like(hasText(query.getClientId()), UcClient::getClientId, query.getClientId())
                .like(hasText(query.getClientName()), UcClient::getClientName, query.getClientName())
                .eq(query.getStatus() != null, UcClient::getStatus, query.getStatus())
                .orderByAsc(UcClient::getId);
        return this.page(new Page<>(query.currentOrDefault(), query.sizeOrDefault()), wrapper);
    }

    @Override
    public List<UcClient> listAll() {
        return this.list(new LambdaQueryWrapper<UcClient>().orderByAsc(UcClient::getId));
    }

    @Override
    public long countByStatus(Integer status) {
        return this.count(new LambdaQueryWrapper<UcClient>().eq(status != null, UcClient::getStatus, status));
    }

    @Override
    public UcClient getByIdOrThrow(String id) {
        UcClient client = (id == null ? null : this.getById(id));
        if (client == null) {
            throw new BizException("应用不存在");
        }
        return client;
    }

    @Override
    public UcClient getByClientId(String clientId) {
        if (!hasText(clientId)) {
            return null;
        }
        return this.getOne(new LambdaQueryWrapper<UcClient>().eq(UcClient::getClientId, clientId));
    }

    @Override
    public UcClient createClient(ClientSaveDTO param) {
        UcClient client = toEntity(param);
        ssoClientRegistry.validate(client);
        if (getByClientId(client.getClientId()) != null) {
            throw new BizException("应用标识已存在：" + client.getClientId());
        }
        // 新增时忽略提交的主键：由数据库（雪花算法）生成
        client.setId(null);
        if (client.getIsPush() == null) {
            client.setIsPush(UcClient.IS_PUSH_NO);
        }
        if (client.getIsSlo() == null) {
            client.setIsSlo(UcClient.IS_SLO_YES);
        }
        if (client.getStatus() == null) {
            client.setStatus(UcClient.STATUS_ENABLED);
        }
        if (!hasText(client.getPushUrl())) {
            client.setPushUrl(UcClient.DEFAULT_PUSH_URL);
        }
        this.save(client);
        ssoClientRegistry.sync();
        return client;
    }

    @Override
    public UcClient updateClient(ClientSaveDTO param) {
        UcClient db = getByIdOrThrow(param.getId());
        ssoClientRegistry.validate(toEntity(param));
        UcClient sameClientId = getByClientId(param.getClientId());
        if (sameClientId != null && !sameClientId.getId().equals(db.getId())) {
            throw new BizException("应用标识已存在：" + param.getClientId());
        }
        String oldClientId = db.getClientId();
        db.setClientId(param.getClientId());
        db.setClientName(param.getClientName());
        db.setAllowUrl(param.getAllowUrl());
        db.setEntryUrl(param.getEntryUrl());
        db.setSecretKey(param.getSecretKey());
        db.setIsPush(param.getIsPush() == null ? db.getIsPush() : param.getIsPush());
        db.setPushUrl(hasText(param.getPushUrl()) ? param.getPushUrl() : db.getPushUrl());
        db.setIsSlo(param.getIsSlo() == null ? db.getIsSlo() : param.getIsSlo());
        db.setStatus(param.getStatus() == null ? db.getStatus() : param.getStatus());
        db.setRemark(param.getRemark());
        this.updateById(db);
        // 应用标识变更时迁移用户授权关系
        if (!db.getClientId().equals(oldClientId)) {
            userClientService.renameClientId(oldClientId, db.getClientId());
        }
        ssoClientRegistry.sync();
        return db;
    }

    @Override
    public void changeStatus(String id, Integer status) {
        UcClient db = getByIdOrThrow(id);
        if (status == null || (status != UcClient.STATUS_DISABLED && status != UcClient.STATUS_ENABLED)) {
            throw new BizException("状态值不合法，0=禁用 / 1=启用");
        }
        db.setStatus(status);
        this.updateById(db);
        ssoClientRegistry.sync();
    }

    @Override
    public void deleteClient(String id) {
        UcClient db = getByIdOrThrow(id);
        this.removeById(id);
        // 同步清理该应用的用户授权关系
        userClientService.removeByClientId(db.getClientId());
        ssoClientRegistry.sync();
    }

    private static boolean hasText(String text) {
        return text != null && !text.isBlank();
    }

    /** 新增/修改参数 → 实体（仅映射前端可提交的字段，创建时间等由本方法外统一维护） */
    private static UcClient toEntity(ClientSaveDTO param) {
        UcClient client = new UcClient();
        client.setId(param.getId());
        client.setClientId(param.getClientId());
        client.setClientName(param.getClientName());
        client.setAllowUrl(param.getAllowUrl());
        client.setEntryUrl(param.getEntryUrl());
        client.setSecretKey(param.getSecretKey());
        client.setIsPush(param.getIsPush());
        client.setPushUrl(param.getPushUrl());
        client.setIsSlo(param.getIsSlo());
        client.setStatus(param.getStatus());
        client.setRemark(param.getRemark());
        return client;
    }

}
