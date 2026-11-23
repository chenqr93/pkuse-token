package com.bdsoft.usercenter.client.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bdsoft.pkusetoken.sso.PkuseSsoManager;
import com.bdsoft.pkusetoken.sso.config.PkuseSsoClientModel;
import com.bdsoft.pkusetoken.sso.config.PkuseSsoServerConfig;
import com.bdsoft.usercenter.common.exception.BizException;
import com.bdsoft.usercenter.client.po.UcClient;
import com.bdsoft.usercenter.client.mapper.UcClientMapper;
import com.bdsoft.usercenter.client.service.SsoClientRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * SSO 客户端配置同步器 实现
 */
@Service
public class SsoClientRegistryImpl extends ServiceImpl<UcClientMapper, UcClient> implements SsoClientRegistry {

    private static final Logger log = LoggerFactory.getLogger(SsoClientRegistryImpl.class);

    /** 启动时 yml 中配置的客户端快照（数据库配置在其基础上覆盖） */
    private Map<String, PkuseSsoClientModel> ymlClientsSnapshot;

    /**
     * 应用启动完成后同步一次（必须等到 PkuseSsoBeanInject 把 yml 绑定的 SSO 配置注入到
     * PkuseSsoManager 全局配置之后，否则同步结果会被后续的配置注入覆盖）
     */
    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        int count = sync();
        log.info("SSO 客户端配置同步完成：数据库生效应用 {} 个，yml 配置应用 {} 个",
                count, ymlClientsSnapshot.size());
    }

    @Override
    public synchronized int sync() {
        PkuseSsoServerConfig config = PkuseSsoManager.getServerConfig();
        if (ymlClientsSnapshot == null) {
            // 首次同步：记录 yml 中配置的客户端，作为数据库配置的补充基座
            this.ymlClientsSnapshot = new LinkedHashMap<>(config.getClients());
        }

        // yml 配置打底，数据库配置覆盖
        Map<String, PkuseSsoClientModel> clients = new LinkedHashMap<>(ymlClientsSnapshot);

        List<UcClient> list = this.list();
        int count = 0;
        for (UcClient client : list) {
            if (client.getClientId() == null || client.getClientId().isBlank()) {
                continue;
            }
            // 禁用/未启用的应用：从客户端列表中移除（含 yml 同名应用）
            if (!Integer.valueOf(UcClient.STATUS_ENABLED).equals(client.getStatus())) {
                clients.remove(client.getClientId());
                continue;
            }
            try {
                clients.put(client.getClientId(), toModel(client));
                count++;
            } catch (Exception e) {
                log.warn("应用 [{}] 配置非法，已跳过同步：{}", client.getClientId(), e.getMessage());
            }
        }

        config.setClients(clients);
        log.info("SSO 客户端配置已同步，当前生效应用：{}", clients.keySet());
        return count;
    }

    @Override
    public void validate(UcClient client) {
        if (client.getClientId() == null || client.getClientId().isBlank()) {
            throw new BizException("应用标识（client_id）不能为空");
        }
        if (client.getClientName() == null || client.getClientName().isBlank()) {
            throw new BizException("应用名称不能为空");
        }
        if (client.getEntryUrl() != null && !client.getEntryUrl().isBlank()) {
            String entryUrl = client.getEntryUrl().trim();
            if (!entryUrl.startsWith("http://") && !entryUrl.startsWith("https://")) {
                throw new BizException("应用入口地址需以 http:// 或 https:// 开头：" + entryUrl);
            }
        }
        checkAllowUrl(client.getAllowUrl());
        try {
            toModel(client);
        } catch (Exception e) {
            throw new BizException("应用配置不合法：" + e.getMessage());
        }
    }

    @Override
    public Set<String> registeredClientIds() {
        return PkuseSsoManager.getServerConfig().getClients().keySet();
    }

    /** 回调地址格式校验：* 或 http(s):// 开头的地址，多个用逗号分隔，* 只能出现在末尾 */
    private void checkAllowUrl(String allowUrl) {
        if (allowUrl == null || allowUrl.isBlank()) {
            return;
        }
        for (String item : allowUrl.split(",")) {
            String url = item.trim();
            if (url.isEmpty()) {
                throw new BizException("回调地址不合法：存在空白地址");
            }
            if ("*".equals(url)) {
                continue;
            }
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                throw new BizException("回调地址不合法（需以 http:// 或 https:// 开头，或填写 * 放行全部）：" + url);
            }
            int starIndex = url.indexOf('*');
            if (starIndex != -1 && starIndex != url.length() - 1) {
                throw new BizException("回调地址中的 * 只能出现在地址末尾：" + url);
            }
        }
    }

    /** 数据库实体 -> SSO 客户端模型（setAllowUrl 内部会校验回调地址格式） */
    private PkuseSsoClientModel toModel(UcClient client) {
        PkuseSsoClientModel model = new PkuseSsoClientModel();
        model.setClient(client.getClientId());
        model.setAllowUrl(client.getAllowUrl() == null ? "" : client.getAllowUrl().trim());
        model.setSecretKey(client.getSecretKey());
        model.setIsPush(Integer.valueOf(UcClient.IS_PUSH_YES).equals(client.getIsPush()));
        model.setIsSlo(!Integer.valueOf(UcClient.IS_SLO_NO).equals(client.getIsSlo()));
        if (client.getPushUrl() != null && !client.getPushUrl().isBlank()) {
            model.setPushUrl(client.getPushUrl().trim());
        }
        return model;
    }

}
