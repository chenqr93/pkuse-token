package com.bdsoft.usercenter.common.util;

import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.digests.SM3Digest;
import org.bouncycastle.crypto.engines.SM2Engine;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.crypto.generators.ECKeyPairGenerator;
import org.bouncycastle.crypto.params.ECKeyGenerationParameters;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.asn1.gm.GMNamedCurves;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.util.encoders.Hex;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

/**
 * 国密算法工具（bcprov）
 *
 * <pre>
 *   SM3：密码摘要（不可逆），替代 MD5
 *   SM2：非对称加密（可逆），用于登录密码的传输加密（前端公钥加密、后端私钥解密）
 * </pre>
 *
 * <p> 曲线使用国密标准曲线 sm2p256v1；SM2 密文采用 C1C3C2 排列（与前端 sm-crypto 库 mode=1 一致）。 </p>
 */
public final class SmCryptoUtil {

    private static final X9ECParameters X9 = GMNamedCurves.getByName("sm2p256v1");

    /** SM2 曲线域参数 */
    public static final ECDomainParameters DOMAIN =
            new ECDomainParameters(X9.getCurve(), X9.getG(), X9.getN(), X9.getH());

    private SmCryptoUtil() {
    }

    // ------------------------- SM3

    /** SM3 摘要（返回 64 位小写十六进制） */
    public static String sm3(String text) {
        byte[] data = (text == null ? "" : text).getBytes(StandardCharsets.UTF_8);
        SM3Digest digest = new SM3Digest();
        digest.update(data, 0, data.length);
        byte[] out = new byte[digest.getDigestSize()];
        digest.doFinal(out, 0);
        return Hex.toHexString(out);
    }

    // ------------------------- SM2 密钥

    /** 生成 SM2 密钥对 */
    public static AsymmetricCipherKeyPair generateKeyPair() {
        ECKeyPairGenerator generator = new ECKeyPairGenerator();
        generator.init(new ECKeyGenerationParameters(DOMAIN, new SecureRandom()));
        return generator.generateKeyPair();
    }

    /** 导出公钥（非压缩点编码：04 || X || Y，130 位十六进制，可直接给前端 sm-crypto 使用） */
    public static String publicKeyHex(AsymmetricCipherKeyPair keyPair) {
        ECPublicKeyParameters publicKey = (ECPublicKeyParameters) keyPair.getPublic();
        return Hex.toHexString(publicKey.getQ().getEncoded(false));
    }

    // ------------------------- SM2 加解密

    /**
     * SM2 解密（C1C3C2）
     *
     * @param cipherHex 密文（十六进制，C1 通常带 04 前缀）
     * @param keyPair   服务端密钥对
     * @return 明文
     */
    public static String sm2Decrypt(String cipherHex, AsymmetricCipherKeyPair keyPair) {
        if (cipherHex == null || cipherHex.isBlank()) {
            throw new IllegalArgumentException("SM2 密文不能为空");
        }
        String hex = cipherHex.trim();
        ECPrivateKeyParameters privateKey = (ECPrivateKeyParameters) keyPair.getPrivate();
        try {
            return doDecrypt(hex, privateKey);
        } catch (Exception first) {
            // 兼容部分前端库不带 04 前缀（或带前缀而 BC 版本不识别）的情况：互换两种编码各试一次
            String adjusted = hex.startsWith("04") ? hex.substring(2) : "04" + hex;
            try {
                return doDecrypt(adjusted, privateKey);
            } catch (Exception second) {
                throw new IllegalArgumentException("SM2 解密失败：" + first.getMessage(), first);
            }
        }
    }

    private static String doDecrypt(String hex, ECPrivateKeyParameters privateKey)
            throws InvalidCipherTextException {
        SM2Engine engine = new SM2Engine(SM2Engine.Mode.C1C3C2);
        engine.init(false, privateKey);
        byte[] cipher = Hex.decode(hex);
        byte[] plain = engine.processBlock(cipher, 0, cipher.length);
        return new String(plain, StandardCharsets.UTF_8);
    }

    /** SM2 加密（C1C3C2，主要用于本地自测） */
    public static String sm2Encrypt(String plain, AsymmetricCipherKeyPair keyPair) {
        try {
            SM2Engine engine = new SM2Engine(SM2Engine.Mode.C1C3C2);
            engine.init(true, (ECPublicKeyParameters) keyPair.getPublic());
            byte[] data = plain.getBytes(StandardCharsets.UTF_8);
            byte[] cipher = engine.processBlock(data, 0, data.length);
            return Hex.toHexString(cipher);
        } catch (InvalidCipherTextException e) {
            throw new IllegalStateException("SM2 加密失败：" + e.getMessage(), e);
        }
    }

}
