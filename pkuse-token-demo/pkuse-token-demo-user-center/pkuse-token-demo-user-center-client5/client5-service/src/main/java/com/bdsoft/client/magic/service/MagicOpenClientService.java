package com.bdsoft.client.magic.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.bdsoft.client.magic.po.MagicOpenClientPO;

/**
 * magic-api 开放接口调用方服务（动态管理：数据库存储，变更即时生效，无需重启）
 */
public interface MagicOpenClientService extends IService<MagicOpenClientPO> {

    /** 按调用方标识查询已启用的登记信息（令牌签发校验用） */
    MagicOpenClientPO findEnabled(String clientId);

    /** 调用方标识是否已登记 */
    boolean existsByClientId(String clientId);

    /**
     * 新登记调用方（自动生成密钥与主键）
     *
     * @return 完整登记信息（含明文密钥，仅创建响应中返回一次）
     */
    MagicOpenClientPO create(String clientId, String clientName, Long timeoutSeconds, String remark);

    /**
     * 重置密钥
     *
     * @return 新密钥（仅本次返回明文）
     */
    String resetSecret(String id);
}
