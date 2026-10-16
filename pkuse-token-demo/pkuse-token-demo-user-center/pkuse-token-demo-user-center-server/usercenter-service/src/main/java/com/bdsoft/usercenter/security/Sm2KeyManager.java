package com.bdsoft.usercenter.security;

import com.bdsoft.usercenter.common.util.SmCryptoUtil;
import jakarta.annotation.PostConstruct;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * SM2 密钥管理（国密）
 *
 * <p> 服务启动时生成 SM2 密钥对： </p>
 * <ul>
 *   <li>公钥暴露给前端（/api/auth/public-key），前端用它对登录密码做 SM2 加密；</li>
 *   <li>私钥仅保留在服务端，用于解密登录请求中的密码密文。 </li>
 * </ul>
 *
 * <p> 演示环境为启动时随机生成（重启后旧密文失效）；生产环境建议从配置中心/密钥管理加载固定密钥对。 </p>
 */
@Component
public class Sm2KeyManager {

    private static final Logger log = LoggerFactory.getLogger(Sm2KeyManager.class);

    private AsymmetricCipherKeyPair keyPair;

    private String publicKeyHex;

    @PostConstruct
    public void init() {
        this.keyPair = SmCryptoUtil.generateKeyPair();
        this.publicKeyHex = SmCryptoUtil.publicKeyHex(keyPair);
        log.info("SM2 密钥对已生成（国密，bcprov），公钥：{}…", publicKeyHex.substring(0, 24));
    }

    /** 公钥（十六进制，04 开头的非压缩点，供前端 sm-crypto 加密使用） */
    public String getPublicKeyHex() {
        return publicKeyHex;
    }

    /** 解密前端提交的密码密文（SM2 / C1C3C2） */
    public String decrypt(String cipherHex) {
        return SmCryptoUtil.sm2Decrypt(cipherHex, keyPair);
    }

}
