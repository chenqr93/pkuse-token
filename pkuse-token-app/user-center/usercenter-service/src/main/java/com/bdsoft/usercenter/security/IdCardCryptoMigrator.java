package com.bdsoft.usercenter.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.bdsoft.usercenter.common.util.SmCryptoUtil;
import com.bdsoft.usercenter.user.mapper.UcUserMapper;
import com.bdsoft.usercenter.user.po.UcUser;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 身份证号加密存储迁移器（国密 SM4，对应升级脚本 upgrade-v16）
 *
 * <p> 应用启动时自动把 uc_user.id_card 中的存量明文迁移为 SM4 密文，并补写 SM3 摘要列： </p>
 * <ul>
 *   <li>识别规则：id_card 非空且不带 {@code SM4:} 密文前缀即视为明文（幂等，已迁移行自动跳过）；</li>
 *   <li>逐行更新密文与摘要两列（LambdaUpdateWrapper 精准 set，不触碰其他字段）；</li>
 *   <li>迁移在登录安全策略装配后、对外提供服务前执行；单行失败仅记日志继续，不阻断启动
 *       （失败行保持明文，下次启动重试；读取侧对明文原样返回，业务不受影响）。 </li>
 * </ul>
 */
@Component
public class IdCardCryptoMigrator implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(IdCardCryptoMigrator.class);

    @Resource
    private UcUserMapper userMapper;

    @Resource
    private IdCardCipher idCardCipher;

    @Override
    public void run(ApplicationArguments args) {
        try {
            List<UcUser> plainRows = userMapper.selectList(new LambdaQueryWrapper<UcUser>()
                    .isNotNull(UcUser::getIdCard)
                    .ne(UcUser::getIdCard, "")
                    .notLikeRight(UcUser::getIdCard, SmCryptoUtil.SM4_CIPHER_PREFIX));
            if (plainRows.isEmpty()) {
                return;
            }
            log.info("检测到 {} 条明文身份证号，开始 SM4 加密迁移", plainRows.size());
            int migrated = 0;
            for (UcUser row : plainRows) {
                try {
                    String plain = row.getIdCard().trim();
                    userMapper.update(null, new LambdaUpdateWrapper<UcUser>()
                            .eq(UcUser::getId, row.getId())
                            .set(UcUser::getIdCard, idCardCipher.encrypt(plain))
                            .set(UcUser::getIdCardHash, idCardCipher.hash(plain))
                            .set(UcUser::getUpdateTime, LocalDateTime.now()));
                    migrated++;
                } catch (Exception e) {
                    // 单行失败不中断整体迁移：该行保持明文，读取侧兼容明文，下次启动重试
                    log.error("身份证号加密迁移失败（id={}）：{}", row.getId(), e.getMessage(), e);
                }
            }
            log.info("身份证号 SM4 加密迁移完成：成功 {} / 共 {} 条", migrated, plainRows.size());
        } catch (Exception e) {
            // 迁移器自身异常不阻断启动：明文行由读取侧兼容（原样返回），下次启动自动重试
            log.error("身份证号加密迁移执行异常：{}", e.getMessage(), e);
        }
    }

}
