package com.bdsoft.usercenter.common.config;

/**
 * 规则配置变更事件
 *
 * <p> 配置保存后由 UcConfigService 发布；日志分区维护器等组件监听该事件并立即按新配置生效 </p>
 * <p>（例如分区频率变更后立刻续建分区），避免组件之间直接相互依赖。 </p>
 */
public class SystemConfigChangedEvent {
}
