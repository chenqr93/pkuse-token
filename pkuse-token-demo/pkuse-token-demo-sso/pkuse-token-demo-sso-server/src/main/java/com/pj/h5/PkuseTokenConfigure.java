package com.pj.h5;

import com.bdsoft.pkusetoken.fun.strategy.PkuseCorsHandleFunction;
import com.bdsoft.pkusetoken.router.PkuseHttpMethod;
import com.bdsoft.pkusetoken.router.PkuseRouter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * [Sa-Token 权限认证] 配置类 （解决跨域问题）
 *
 * @author click33
 */
@Configuration
public class PkuseTokenConfigure {

    /**
     * CORS 跨域处理策略
     */
    @Bean
    public PkuseCorsHandleFunction corsHandle() {
        return (req, res, sto) -> {
            res.
                    // 允许指定域访问跨域资源
                            setHeader("Access-Control-Allow-Origin", "*")
                    // 允许所有请求方式
                    .setHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS, DELETE")
                    // 有效时间
                    .setHeader("Access-Control-Max-Age", "3600")
                    // 允许的header参数
                    .setHeader("Access-Control-Allow-Headers", "*");

            // 如果是预检请求，则立即返回到前端
            PkuseRouter.match(PkuseHttpMethod.OPTIONS)
                    .free(r -> System.out.println("--------OPTIONS预检请求，不做处理"))
                    .back();
        };
    }

}
