package com.bdsoft.usercenter.common.util;

import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.digests.SM3Digest;
import org.bouncycastle.crypto.engines.SM2Engine;
import org.bouncycastle.crypto.engines.SM4Engine;
import org.bouncycastle.crypto.modes.CBCBlockCipher;
import org.bouncycastle.crypto.paddings.PKCS7Padding;
import org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.crypto.generators.ECKeyPairGenerator;
import org.bouncycastle.crypto.params.ECKeyGenerationParameters;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;
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
 *   SM4：对称加密（可逆），用于敏感字段的存储加密（如身份证号，随机 IV + CBC + PKCS7）
 * </pre>
 *
 * <p> 曲线使用国密标准曲线 sm2p256v1；SM2 密文采用 C1C3C2 排列（与前端 sm-crypto 库 mode=1 一致）。 </p>
 * <p> SM4 存储密文格式为 {@code SM4: + hex(IV + 密文)}：带随机 IV（同一明文每次密文不同），
 * 因此事先无法做等值查询，等值匹配场景应配合 SM3 摘要索引列使用。 </p>
 */
public final class SmCryptoUtil {

    private static final X9ECParameters X9 = GMNamedCurves.getByName("sm2p256v1");

    /** SM2 曲线域参数 */
    public static final ECDomainParameters DOMAIN =
            new ECDomainParameters(X9.getCurve(), X9.getG(), X9.getN(), X9.getH());

    /** SM4 存储密文格式前缀（SM4: + hex(IV + 密文)），用于区分密文与历史明文数据 */
    public static final String SM4_CIPHER_PREFIX = "SM4:";

    /** SM4 密钥长度（字节，128 位） */
    public static final int SM4_KEY_LENGTH = 16;

    /** SM4 分组长度（字节，IV 与密文分组对齐用） */
    private static final int SM4_BLOCK_SIZE = 16;

    private static final SecureRandom RANDOM = new SecureRandom();

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

    // ------------------------- SM4 加解密（存储加密）

    /**
     * SM4 加密（CBC + PKCS7，随机 IV）
     *
     * <p> 输出格式：{@code SM4: + hex(IV + 密文)}；同一明文每次加密结果不同（随机 IV）。 </p>
     *
     * @param plain 明文（null / 空白原样返回 null）
     * @param key   密钥（16 字节 = 128 位）
     * @return 带前缀的十六进制密文
     */
    public static String sm4Encrypt(String plain, byte[] key) {
        if (plain == null || plain.isBlank()) {
            return null;
        }
        checkKey(key);
        byte[] iv = new byte[SM4_BLOCK_SIZE];
        RANDOM.nextBytes(iv);
        byte[] cipher = process(true, plain.getBytes(StandardCharsets.UTF_8), key, iv);
        byte[] out = new byte[iv.length + cipher.length];
        System.arraycopy(iv, 0, out, 0, iv.length);
        System.arraycopy(cipher, 0, out, iv.length, cipher.length);
        return SM4_CIPHER_PREFIX + Hex.toHexString(out);
    }

    /**
     * SM4 解密（CBC + PKCS7）
     *
     * <p> 兼容历史明文：入参不带 {@code SM4:} 前缀时视为未加密的存量数据，原样返回
     * （配合启动迁移器平滑过渡，密文识别失败不会误伤明文）。 </p>
     *
     * @param stored 库中存储值（null / 空白原样返回）
     * @param key    密钥（16 字节 = 128 位）
     * @return 明文
     */
    public static String sm4Decrypt(String stored, byte[] key) {
        if (stored == null || stored.isBlank()) {
            return stored;
        }
        if (!isSm4Cipher(stored)) {
            return stored;
        }
        checkKey(key);
        byte[] data;
        try {
            data = Hex.decode(stored.substring(SM4_CIPHER_PREFIX.length()).trim());
        } catch (Exception e) {
            throw new IllegalArgumentException("SM4 密文格式非法（hex 解码失败）", e);
        }
        // 密文至少包含 IV(16B) + 一个密文分组(16B)
        if (data.length < SM4_BLOCK_SIZE * 2 || (data.length - SM4_BLOCK_SIZE) % SM4_BLOCK_SIZE != 0) {
            throw new IllegalArgumentException("SM4 密文长度非法：" + data.length + " 字节");
        }
        byte[] iv = new byte[SM4_BLOCK_SIZE];
        byte[] cipher = new byte[data.length - SM4_BLOCK_SIZE];
        System.arraycopy(data, 0, iv, 0, iv.length);
        System.arraycopy(data, iv.length, cipher, 0, cipher.length);
        byte[] plain = process(false, cipher, key, iv);
        return new String(plain, StandardCharsets.UTF_8);
    }

    /** 是否为 SM4 存储密文格式（SM4: 前缀），用于识别密文 / 区分存量明文 */
    public static boolean isSm4Cipher(String value) {
        return value != null && value.startsWith(SM4_CIPHER_PREFIX);
    }

    /** SM4 密钥解析：32 位十六进制字符串 → 16 字节密钥（格式非法抛出异常，启动时用于 fail-fast） */
    public static byte[] sm4Key(String keyHex) {
        if (keyHex == null || keyHex.isBlank()) {
            throw new IllegalArgumentException("SM4 密钥未配置（user-center.crypto.id-card-sm4-key）");
        }
        String hex = keyHex.trim();
        if (hex.length() != SM4_KEY_LENGTH * 2 || !hex.matches("[0-9a-fA-F]+")) {
            throw new IllegalArgumentException("SM4 密钥格式非法：应为 32 位十六进制（128 位）");
        }
        return Hex.decode(hex);
    }

    private static byte[] process(boolean forEncryption, byte[] data, byte[] key, byte[] iv) {
        try {
            PaddedBufferedBlockCipher cipher = new PaddedBufferedBlockCipher(
                    CBCBlockCipher.newInstance(new SM4Engine()), new PKCS7Padding());
            cipher.init(forEncryption, new ParametersWithIV(new KeyParameter(key), iv));
            byte[] buffer = new byte[cipher.getOutputSize(data.length)];
            int len = cipher.processBytes(data, 0, data.length, buffer, 0);
            len += cipher.doFinal(buffer, len);
            byte[] out = new byte[len];
            System.arraycopy(buffer, 0, out, 0, len);
            return out;
        } catch (InvalidCipherTextException e) {
            throw new IllegalStateException("SM4 " + (forEncryption ? "加密" : "解密") + "失败：" + e.getMessage(), e);
        }
    }

    private static void checkKey(byte[] key) {
        if (key == null || key.length != SM4_KEY_LENGTH) {
            throw new IllegalArgumentException("SM4 密钥长度非法：应为 16 字节（128 位）");
        }
    }

}
