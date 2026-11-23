package com.bdsoft.usercenter.security;

import com.bdsoft.usercenter.common.util.SmCryptoUtil;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 身份证号存储加解密组件（国密 SM4 + SM3 摘要）
 *
 * <p> 存储方案（等值查询需求下的加密存储）： </p>
 * <ul>
 *   <li>uc_user.id_card 存 SM4 密文（随机 IV，同一身份证号每次加密结果不同）；</li>
 *   <li>uc_user.id_card_hash 存 SM3 摘要，身份证精确匹配（IN 查询）统一走摘要列，不碰密文；</li>
 *   <li>对外接口（管理端 / 开放接口）出口处由 Controller 用本组件解密回填明文。 </li>
 * </ul>
 *
 * <p> 密钥为部署级配置（user-center.crypto.id-card-sm4-key，各环境 yml 中独立的 128 位十六进制），
 * 与登录密码的 SM2 传输密钥不同：存储加密密钥必须固定（重启后仍能解密历史数据），且一经启用不可随意更换
 * （换密钥 = 历史密文不可解）。启动时校验格式，非法立即失败，避免静默写入无法解密的数据。 </p>
 */
@Component
public class IdCardCipher {

    @Value("${user-center.crypto.id-card-sm4-key:}")
    private String sm4KeyHex;

    /** SM4 密钥（16 字节） */
    private byte[] key;

    @PostConstruct
    public void init() {
        // fail-fast：密钥缺失 / 格式非法直接终止启动，防止密文写入后无法解密
        this.key = SmCryptoUtil.sm4Key(sm4KeyHex);
    }

    /** 加密（入库前调用）：null / 空白返回 null（保持「未填写」语义）；已是密文则原样返回（幂等，防双重加密） */
    public String encrypt(String plain) {
        if (plain == null || plain.isBlank()) {
            return null;
        }
        if (SmCryptoUtil.isSm4Cipher(plain)) {
            return plain;
        }
        return SmCryptoUtil.sm4Encrypt(plain.trim(), key);
    }

    /**
     * 解密（出库后调用）：null / 空白原样返回；不带密文前缀的存量明文原样返回（配合启动迁移器平滑过渡）。
     */
    public String decrypt(String stored) {
        if (stored == null || stored.isBlank()) {
            return stored;
        }
        return SmCryptoUtil.sm4Decrypt(stored, key);
    }

    /** SM3 摘要（身份证精确匹配索引列）：null / 空白返回 null */
    public String hash(String plain) {
        if (plain == null || plain.isBlank()) {
            return null;
        }
        return SmCryptoUtil.sm3(plain.trim());
    }

}
