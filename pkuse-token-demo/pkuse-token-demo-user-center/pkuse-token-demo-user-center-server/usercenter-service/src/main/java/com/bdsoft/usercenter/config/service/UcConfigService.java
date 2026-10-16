package com.bdsoft.usercenter.config.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.bdsoft.usercenter.common.config.SystemConfig;
import com.bdsoft.usercenter.config.po.UcConfig;

import java.util.List;
import java.util.Map;

/**
 * 规则配置 Service
 *
 * <p> 配置以键值对形式存储于 uc_config，读取走内存快照（{@link #get()}），写库后立即刷新快照， </p>
 * <p> 并发布 SystemConfigChangedEvent 通知相关组件（如日志分区维护器）按新配置生效。 </p>
 */
public interface UcConfigService extends IService<UcConfig> {

    /** 全部配置项（按分组与定义顺序返回，供管理界面展示） */
    List<UcConfig> listAll();

    /** 当前生效的配置快照（volatile 引用，含默认值回落，读侧无锁） */
    SystemConfig get();

    /**
     * 批量更新配置
     *
     * @param values 配置键 → 配置值（仅提交的键会被修改）
     */
    void updateValues(Map<String, String> values);

    /** 恢复全部配置项的默认值 */
    void resetToDefaults();

}
