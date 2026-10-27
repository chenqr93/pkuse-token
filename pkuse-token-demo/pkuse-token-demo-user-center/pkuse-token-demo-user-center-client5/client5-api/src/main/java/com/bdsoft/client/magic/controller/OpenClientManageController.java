package com.bdsoft.client.magic.controller;

import com.bdsoft.client.magic.po.MagicOpenClientPO;
import com.bdsoft.client.magic.service.MagicOpenClientService;
import com.bdsoft.pkusetoken.stp.StpUtil;
import com.bdsoft.pkusetoken.util.PkuseResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

/**
 * magic-api 开放接口调用方管理（动态管理模块，表 s_open_client）
 *
 * <p> 由原 application.yml 的 magic-open.clients 静态配置提取为数据库动态管理：
 * 增删改、启禁用、重置密钥均即时生效（无需重启）；令牌签发（POST /open/token）实时读取本表校验。 </p>
 *
 * <p> 管理接口需 pkuse-token 登录（satoken 请求头）；禁用 / 删除 / 重置密钥会按
 * loginId=open:{clientId} 吊销该调用方全部已签发令牌。 </p>
 *
 * <pre>
 *   GET    /open/clients            列表（密钥脱敏展示）
 *   POST   /open/clients            新登记（自动生成密钥，明文仅本次返回）
 *   PUT    /open/clients/{id}       修改显示名 / 令牌有效期 / 备注
 *   PUT    /open/clients/{id}/status?status=0|1   启用 / 禁用
 *   PUT    /open/clients/{id}/secret             重置密钥（明文仅本次返回，并吊销已签发令牌）
 *   DELETE /open/clients/{id}       删除（并吊销已签发令牌）
 * </pre>
 */
@RestController
@RequestMapping("/open/clients")
public class OpenClientManageController {

    private final MagicOpenClientService openClientService;

    public OpenClientManageController(MagicOpenClientService openClientService) {
        this.openClientService = openClientService;
    }

    /** 调用方列表（密钥脱敏） */
    @GetMapping
    public PkuseResult list() {
        if (!StpUtil.isLogin()) {
            return PkuseResult.get(401, "未登录，无权管理开放调用方", null);
        }
        List<MagicOpenClientPO> list = openClientService.list();
        list.forEach(item -> item.setSecretKey(maskSecret(item.getSecretKey())));
        return PkuseResult.ok().setData(list);
    }

    /**
     * 新登记调用方
     *
     * <p> 密钥自动生成，明文仅在本次响应中返回一次，请及时下发给调用方妥善保管。 </p>
     */
    @PostMapping
    public PkuseResult create(String clientId, String clientName, Long timeoutSeconds, String remark) {
        if (!StpUtil.isLogin()) {
            return PkuseResult.get(401, "未登录，无权管理开放调用方", null);
        }
        try {
            MagicOpenClientPO entity = openClientService.create(clientId, clientName, timeoutSeconds, remark);
            return PkuseResult.ok("登记成功（密钥仅本次返回，请妥善保管）").setData(entity);
        } catch (IllegalArgumentException e) {
            return PkuseResult.error(e.getMessage());
        }
    }

    /** 修改显示名 / 令牌有效期 / 备注 */
    @PutMapping("/{id}")
    public PkuseResult update(@PathVariable String id, String clientName, Long timeoutSeconds, String remark) {
        if (!StpUtil.isLogin()) {
            return PkuseResult.get(401, "未登录，无权管理开放调用方", null);
        }
        MagicOpenClientPO entity = openClientService.getById(id);
        if (entity == null) {
            return PkuseResult.error("调用方不存在：id=" + id);
        }
        if (clientName != null) {
            entity.setClientName(clientName);
        }
        if (timeoutSeconds != null && timeoutSeconds > 0) {
            entity.setTimeoutSeconds(timeoutSeconds);
        }
        if (remark != null) {
            entity.setRemark(remark);
        }
        entity.setUpdateTime(LocalDateTime.now());
        openClientService.updateById(entity);
        entity.setSecretKey(maskSecret(entity.getSecretKey()));
        return PkuseResult.ok("修改成功").setData(entity);
    }

    /** 启用 / 禁用（禁用时吊销该调用方全部已签发令牌） */
    @PutMapping("/{id}/status")
    public PkuseResult updateStatus(@PathVariable String id, Integer status) {
        if (!StpUtil.isLogin()) {
            return PkuseResult.get(401, "未登录，无权管理开放调用方", null);
        }
        if (status == null || (status != 0 && status != 1)) {
            return PkuseResult.error("status 仅支持 0=禁用 / 1=启用");
        }
        MagicOpenClientPO entity = openClientService.getById(id);
        if (entity == null) {
            return PkuseResult.error("调用方不存在：id=" + id);
        }
        entity.setStatus(status);
        entity.setUpdateTime(LocalDateTime.now());
        openClientService.updateById(entity);
        if (status == 0) {
            revokeTokens(entity.getClientId());
            return PkuseResult.ok("已禁用并吊销其全部令牌");
        }
        return PkuseResult.ok("已启用");
    }

    /** 重置密钥（新密钥仅本次返回，并吊销已签发令牌） */
    @PutMapping("/{id}/secret")
    public PkuseResult resetSecret(@PathVariable String id) {
        if (!StpUtil.isLogin()) {
            return PkuseResult.get(401, "未登录，无权管理开放调用方", null);
        }
        MagicOpenClientPO entity = openClientService.getById(id);
        if (entity == null) {
            return PkuseResult.error("调用方不存在：id=" + id);
        }
        try {
            String secret = openClientService.resetSecret(id);
            revokeTokens(entity.getClientId());
            return PkuseResult.ok("密钥已重置（旧令牌已全部吊销，新密钥仅本次返回）").setData(secret);
        } catch (IllegalArgumentException e) {
            return PkuseResult.error(e.getMessage());
        }
    }

    /** 删除调用方（并吊销已签发令牌） */
    @DeleteMapping("/{id}")
    public PkuseResult delete(@PathVariable String id) {
        if (!StpUtil.isLogin()) {
            return PkuseResult.get(401, "未登录，无权管理开放调用方", null);
        }
        MagicOpenClientPO entity = openClientService.getById(id);
        if (entity == null) {
            return PkuseResult.error("调用方不存在：id=" + id);
        }
        openClientService.removeById(id);
        revokeTokens(entity.getClientId());
        return PkuseResult.ok("已删除并吊销其全部令牌");
    }

    /** 按 loginId=open:{clientId} 吊销该调用方全部已签发令牌 */
    private void revokeTokens(String clientId) {
        StpUtil.logout("open:" + clientId);
    }

    /** 密钥脱敏：保留前 4 位与后 4 位 */
    private static String maskSecret(String secret) {
        if (secret == null || secret.length() <= 8) {
            return "****";
        }
        return secret.substring(0, 4) + "****" + secret.substring(secret.length() - 4);
    }
}
