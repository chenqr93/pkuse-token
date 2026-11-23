package com.bdsoft.usercenter.open.support;

import com.bdsoft.pkusetoken.sign.PkuseSignManager;
import com.bdsoft.pkusetoken.sign.config.PkuseSignConfig;
import com.bdsoft.pkusetoken.sign.template.PkuseSignMany;
import com.bdsoft.usercenter.client.po.UcClient;
import com.bdsoft.usercenter.client.service.UcClientService;
import jakarta.annotation.Resource;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * pkuse-token-sign 密钥装配（开放接口 @PkuseCheckSign 验签的密钥来源）
 *
 * <p> 重定义 {@link PkuseSignMany#findSaSignConfigMethod}：按请求参数 client **实时**查询 </p>
 * <p> uc_client（应用管理）解析密钥——密钥轮换/应用停用即时生效，无需重启或预注册； </p>
 * <p> 应用不存在或已停用返回 null，@PkuseCheckSign 校验即失败（未找到签名配置）。 </p>
 */
@Component
public class PkuseSignConfigInitializer implements ApplicationRunner {

    @Resource
    private UcClientService clientService;

    @Override
    public void run(ApplicationArguments args) {
        PkuseSignMany.findSaSignConfigMethod = (appid) -> {
            UcClient client = clientService.getByClientId(appid);
            if (client == null || !Integer.valueOf(UcClient.STATUS_ENABLED).equals(client.getStatus())) {
                return null;
            }
            return new PkuseSignConfig(client.getSecretKey());
        };
    }
}
