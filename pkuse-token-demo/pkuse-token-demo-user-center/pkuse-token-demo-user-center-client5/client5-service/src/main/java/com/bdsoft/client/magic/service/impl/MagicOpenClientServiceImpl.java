package com.bdsoft.client.magic.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bdsoft.client.magic.mapper.MagicOpenClientMapper;
import com.bdsoft.client.magic.po.MagicOpenClientPO;
import com.bdsoft.client.magic.service.MagicOpenClientService;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * magic-api 开放接口调用方服务实现
 */
@Service
public class MagicOpenClientServiceImpl extends ServiceImpl<MagicOpenClientMapper, MagicOpenClientPO>
        implements MagicOpenClientService {

    /** 默认令牌有效期（秒）：2 小时 */
    private static final long DEFAULT_TIMEOUT_SECONDS = 7200;

    /** 密钥长度（字母数字） */
    private static final int SECRET_LENGTH = 40;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Override
    public MagicOpenClientPO findEnabled(String clientId) {
        if (clientId == null || clientId.isBlank()) {
            return null;
        }
        return lambdaQuery()
                .eq(MagicOpenClientPO::getClientId, clientId)
                .eq(MagicOpenClientPO::getStatus, 1)
                .one();
    }

    @Override
    public boolean existsByClientId(String clientId) {
        return lambdaQuery().eq(MagicOpenClientPO::getClientId, clientId).count() > 0;
    }

    @Override
    public MagicOpenClientPO create(String clientId, String clientName, Long timeoutSeconds, String remark) {
        if (clientId == null || !clientId.matches("[a-zA-Z0-9_-]{2,64}")) {
            throw new IllegalArgumentException("调用方标识须为 2~64 位字母/数字/下划线/中划线");
        }
        if (existsByClientId(clientId)) {
            throw new IllegalArgumentException("调用方标识已登记：" + clientId);
        }
        LocalDateTime now = LocalDateTime.now();
        MagicOpenClientPO entity = new MagicOpenClientPO();
        entity.setId(UUID.randomUUID().toString().replace("-", ""));
        entity.setClientId(clientId);
        entity.setClientName(clientName);
        entity.setSecretKey(generateSecret());
        entity.setTimeoutSeconds(timeoutSeconds == null || timeoutSeconds <= 0 ? DEFAULT_TIMEOUT_SECONDS : timeoutSeconds);
        entity.setStatus(1);
        entity.setRemark(remark);
        entity.setCreateTime(now);
        entity.setUpdateTime(now);
        save(entity);
        return entity;
    }

    @Override
    public String resetSecret(String id) {
        MagicOpenClientPO entity = getById(id);
        if (entity == null) {
            throw new IllegalArgumentException("调用方不存在：id=" + id);
        }
        String secret = generateSecret();
        entity.setSecretKey(secret);
        entity.setUpdateTime(LocalDateTime.now());
        updateById(entity);
        return secret;
    }

    /** 生成随机密钥（字母数字，SecureRandom） */
    private static String generateSecret() {
        String chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder(SECRET_LENGTH);
        for (int i = 0; i < SECRET_LENGTH; i++) {
            sb.append(chars.charAt(SECURE_RANDOM.nextInt(chars.length())));
        }
        return sb.toString();
    }
}
