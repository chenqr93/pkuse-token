package com.bdsoft.usercenter.config.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bdsoft.usercenter.common.exception.BizException;
import com.bdsoft.usercenter.common.config.SystemConfig;
import com.bdsoft.usercenter.common.config.SystemConfigChangedEvent;
import com.bdsoft.usercenter.common.config.SystemConfigItems;
import com.bdsoft.usercenter.config.po.UcConfig;
import com.bdsoft.usercenter.config.mapper.UcConfigMapper;
import com.bdsoft.usercenter.config.service.UcConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 规则配置 Service 实现
 *
 * <p> 启动时：按 {@link SystemConfigItems} 补齐缺失配置项、同步配置项元数据（名称/分组/类型/说明/默认值）， </p>
 * <p> 然后加载内存快照；保存/恢复默认后立即刷新快照并发布变更事件。 </p>
 */
@Service
public class UcConfigServiceImpl extends ServiceImpl<UcConfigMapper, UcConfig> implements UcConfigService {

    private static final Logger log = LoggerFactory.getLogger(UcConfigServiceImpl.class);

    /** 当前生效配置快照（不可变对象，整体替换） */
    private volatile SystemConfig snapshot = SystemConfig.defaults();

    private final ApplicationEventPublisher eventPublisher;

    public UcConfigServiceImpl(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    /** 启动时补齐配置项并加载快照（@Order(0)：先于日志分区维护器等依赖方） */
    @Order(0)
    @Transactional(rollbackFor = Exception.class)
    @EventListener(ApplicationReadyEvent.class)
    public void initOnStartup() {
        List<UcConfig> inserts = new ArrayList<>();
        List<UcConfig> updates = new ArrayList<>();
        Map<String, UcConfig> byKey = listAll().stream()
                .collect(Collectors.toMap(UcConfig::getConfigKey, Function.identity(), (a, b) -> a, LinkedHashMap::new));
        for (SystemConfigItems.Item item : SystemConfigItems.ALL) {
            UcConfig row = byKey.get(item.key());
            if (row == null) {
                UcConfig created = new UcConfig();
                created.setConfigKey(item.key());
                created.setConfigValue(item.defaultValue());
                created.setConfigName(item.name());
                created.setConfigGroup(item.group());
                created.setValueType(item.type());
                created.setDefaultValue(item.defaultValue());
                created.setRemark(item.remark());
                inserts.add(created);
                continue;
            }
            // 元数据（名称/分组/类型/说明/默认值）以代码定义为准同步，配置值保持库中已有值
            boolean metaChanged = !Objects.equals(row.getConfigName(), item.name())
                    || !Objects.equals(row.getConfigGroup(), item.group())
                    || !Objects.equals(row.getValueType(), item.type())
                    || !Objects.equals(row.getDefaultValue(), item.defaultValue())
                    || !Objects.equals(row.getRemark(), item.remark());
            if (metaChanged) {
                row.setConfigName(item.name());
                row.setConfigGroup(item.group());
                row.setValueType(item.type());
                row.setDefaultValue(item.defaultValue());
                row.setRemark(item.remark());
                updates.add(row);
            }
        }
        if (!inserts.isEmpty()) {
            saveBatch(inserts);
        }
        if (!updates.isEmpty()) {
            updateBatchById(updates);
        }
        refresh();
        log.info("规则配置已加载（{} 项，本次新增 {} 项）：复杂度校验={}、首次登录改密={}、设备验证={}、验证码={}（阈值 {}）、"
                        + "登录日志分区={}、操作日志分区={}",
                SystemConfigItems.ALL.size(), inserts.size(),
                snapshot.isPasswordComplexityEnabled(), snapshot.isForceChangePasswordOnFirst(),
                snapshot.isDeviceVerifyEnabled(), snapshot.isCaptchaEnabled(), snapshot.getCaptchaFailThreshold(),
                snapshot.getLoginLogPartition(), snapshot.getOperLogPartition());
    }

    @Override
    public List<UcConfig> listAll() {
        List<UcConfig> rows = this.list(new LambdaQueryWrapper<UcConfig>().orderByAsc(UcConfig::getConfigKey));
        // 按定义顺序（分组顺序 + 定义顺序）返回，界面展示更稳定
        Map<String, UcConfig> byKey = rows.stream()
                .collect(Collectors.toMap(UcConfig::getConfigKey, Function.identity(), (a, b) -> a));
        List<UcConfig> ordered = new ArrayList<>(SystemConfigItems.ALL.size());
        for (SystemConfigItems.Item item : SystemConfigItems.ALL) {
            UcConfig row = byKey.get(item.key());
            if (row != null) {
                ordered.add(row);
            }
        }
        // 库中存在但代码定义中已移除的配置项：仍返回，便于管理员查看
        rows.stream().filter(row -> SystemConfigItems.find(row.getConfigKey()) == null).forEach(ordered::add);
        return ordered;
    }

    @Override
    public SystemConfig get() {
        return snapshot;
    }

    @Override
    public void updateValues(Map<String, String> values) {
        if (values == null || values.isEmpty()) {
            throw new BizException("未提交任何配置项");
        }
        Map<String, UcConfig> byKey = this.list().stream()
                .collect(Collectors.toMap(UcConfig::getConfigKey, Function.identity(), (a, b) -> a));

        // 1、逐项校验（类型 / 取值范围 / 选项）
        Map<String, String> normalized = new LinkedHashMap<>();
        values.forEach((key, value) -> {
            SystemConfigItems.Item item = SystemConfigItems.find(key);
            if (item == null) {
                throw new BizException("未知配置项：" + key);
            }
            if (byKey.get(key) == null) {
                throw new BizException("配置项不存在：" + key);
            }
            normalized.put(key, validate(item, value));
        });

        // 2、跨字段校验：默认密码需满足合并后的密码复杂度要求
        Map<String, String> merged = new LinkedHashMap<>();
        byKey.forEach((key, row) -> merged.put(key, row.getConfigValue()));
        merged.putAll(normalized);
        SystemConfig mergedConfig = SystemConfig.parse(merged);
        String passwordError = mergedConfig.checkPassword(mergedConfig.getDefaultPassword());
        if (passwordError != null) {
            throw new BizException("系统默认密码不符合密码复杂度要求：" + passwordError);
        }

        // 3、落库并刷新快照（逐条更新，不用 updateBatchById：操作日志需要逐条捕获变更，
        //    BATCH 执行器同一 SQL 只 prepare 一次，会漏掉后续语句的前后数据）
        List<UcConfig> changed = new ArrayList<>();
        normalized.forEach((key, value) -> {
            UcConfig row = byKey.get(key);
            if (!Objects.equals(row.getConfigValue(), value)) {
                row.setConfigValue(value);
                changed.add(row);
            }
        });
        if (!changed.isEmpty()) {
            changed.forEach(this::updateById);
        }
        refresh();
        eventPublisher.publishEvent(new SystemConfigChangedEvent());
        log.info("规则配置已更新：{}", normalized);
    }

    @Override
    public void resetToDefaults() {
        Map<String, String> defaults = new LinkedHashMap<>();
        SystemConfigItems.ALL.forEach(item -> defaults.put(item.key(), item.defaultValue()));
        updateValues(defaults);
    }

    /** 重新加载内存快照 */
    private void refresh() {
        Map<String, String> values = this.list().stream()
                .filter(row -> row.getConfigKey() != null)
                .collect(Collectors.toMap(UcConfig::getConfigKey,
                        row -> row.getConfigValue() == null ? "" : row.getConfigValue(), (a, b) -> a));
        this.snapshot = SystemConfig.parse(values);
    }

    /** 单值校验与规范化 */
    private static String validate(SystemConfigItems.Item item, String value) {
        String text = value == null ? "" : value.trim();
        switch (item.type()) {
            case SystemConfigItems.TYPE_BOOL -> {
                if (!"true".equals(text) && !"false".equals(text)) {
                    throw new BizException(item.name() + " 只能为 true / false");
                }
            }
            case SystemConfigItems.TYPE_INT -> {
                int number;
                try {
                    number = Integer.parseInt(text);
                } catch (NumberFormatException e) {
                    throw new BizException(item.name() + " 必须为整数");
                }
                int[] range = SystemConfigItems.INT_RANGES.get(item.key());
                if (range != null && (number < range[0] || number > range[1])) {
                    throw new BizException(item.name() + " 取值范围为 " + range[0] + " ~ " + range[1]);
                }
            }
            case SystemConfigItems.TYPE_OPTION -> {
                boolean matched = item.options().stream().anyMatch(option -> option.value().equals(text));
                if (!matched) {
                    throw new BizException(item.name() + " 取值不合法：" + text);
                }
            }
            default -> {
                if (text.isEmpty()) {
                    throw new BizException(item.name() + " 不能为空");
                }
                if (text.length() > 512) {
                    throw new BizException(item.name() + " 长度不能超过 512 字符");
                }
            }
        }
        return text;
    }

}
