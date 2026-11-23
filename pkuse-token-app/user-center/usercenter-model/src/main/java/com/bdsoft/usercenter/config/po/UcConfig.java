package com.bdsoft.usercenter.config.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.bdsoft.usercenter.common.pojo.AbstractAuditPO;


/**
 * 用户中心-规则配置表（uc_config）
 *
 * <p> 键值对存储平台规则（密码规则 / 登录规则 / 日志规则）；默认值由后端启动时按 </p>
 * <p> {@link com.bdsoft.usercenter.common.config.SystemConfigItems} 的定义自动补齐。 </p>
 */
@TableName("uc_config")
public class UcConfig extends AbstractAuditPO {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    /** 配置键（如 password.min-length） */
    private String configKey;

    /** 配置值（统一按字符串存储，读取时按 valueType 转换） */
    private String configValue;

    /** 配置名称（界面展示） */
    private String configName;

    /** 配置分组：密码规则 / 登录规则 / 日志规则 */
    private String configGroup;

    /** 值类型：string / int / bool / option */
    private String valueType;

    /** 默认值（恢复默认时写回） */
    private String defaultValue;

    /** 说明（界面展示） */
    private String remark;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getConfigKey() {
        return configKey;
    }

    public void setConfigKey(String configKey) {
        this.configKey = configKey;
    }

    public String getConfigValue() {
        return configValue;
    }

    public void setConfigValue(String configValue) {
        this.configValue = configValue;
    }

    public String getConfigName() {
        return configName;
    }

    public void setConfigName(String configName) {
        this.configName = configName;
    }

    public String getConfigGroup() {
        return configGroup;
    }

    public void setConfigGroup(String configGroup) {
        this.configGroup = configGroup;
    }

    public String getValueType() {
        return valueType;
    }

    public void setValueType(String valueType) {
        this.valueType = valueType;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    @Override
    public String toString() {
        return "UcConfig{" + super.toString() +
                ", id='" + id + '\'' +
                ", configKey='" + configKey + '\'' +
                ", configValue='" + configValue + '\'' +
                ", configName='" + configName + '\'' +
                ", configGroup='" + configGroup + '\'' +
                ", valueType='" + valueType + '\'' +
                ", defaultValue='" + defaultValue + '\'' +
                ", remark='" + remark + '\'' +
                '}';
    }
}
