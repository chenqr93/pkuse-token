package com.bdsoft.usercenter.config.vo;

import com.bdsoft.usercenter.common.config.SystemConfigItems;
import com.bdsoft.usercenter.config.po.UcConfig;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 规则配置项视图对象（配置页面渲染用；含名称、类型、取值范围等展示加工内容）
 */
public class ConfigItemVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 配置键 */
    private String key;

    /** 配置名称（界面展示） */
    private String name;

    /** 配置分组：密码规则 / 登录规则 / 日志规则 */
    private String group;

    /** 值类型：string / int / bool / option */
    private String type;

    /** 当前值 */
    private String value;

    /** 默认值（恢复默认时写回） */
    private String defaultValue;

    /** 说明（界面展示） */
    private String remark;

    /** 最后修改时间 */
    private LocalDateTime updateTime;

    /** 可选值（option 类型；其余类型为空集合） */
    private List<SystemConfigItems.Option> options;

    /** 取值范围下界（int 类型；其余类型为 null） */
    private Integer min;

    /** 取值范围上界（int 类型；其余类型为 null） */
    private Integer max;

    /** 由实体与配置项定义转换为视图对象（配置项定义见 SystemConfigItems） */
    public static ConfigItemVO from(UcConfig row) {
        if (row == null) {
            return null;
        }
        ConfigItemVO vo = new ConfigItemVO();
        vo.setKey(row.getConfigKey());
        vo.setName(row.getConfigName());
        vo.setGroup(row.getConfigGroup());
        vo.setType(row.getValueType());
        vo.setValue(row.getConfigValue());
        vo.setDefaultValue(row.getDefaultValue());
        vo.setRemark(row.getRemark());
        vo.setUpdateTime(row.getUpdateTime());
        SystemConfigItems.Item item = SystemConfigItems.find(row.getConfigKey());
        vo.setOptions(item == null ? List.of() : item.options());
        int[] range = SystemConfigItems.INT_RANGES.get(row.getConfigKey());
        vo.setMin(range == null ? null : range[0]);
        vo.setMax(range == null ? null : range[1]);
        return vo;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
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

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public List<SystemConfigItems.Option> getOptions() {
        return options;
    }

    public void setOptions(List<SystemConfigItems.Option> options) {
        this.options = options;
    }

    public Integer getMin() {
        return min;
    }

    public void setMin(Integer min) {
        this.min = min;
    }

    public Integer getMax() {
        return max;
    }

    public void setMax(Integer max) {
        this.max = max;
    }

    @Override
    public String toString() {
        return "ConfigItemVO{" +
                "key='" + key + '\'' +
                ", name='" + name + '\'' +
                ", group='" + group + '\'' +
                ", type='" + type + '\'' +
                ", value='" + value + '\'' +
                ", defaultValue='" + defaultValue + '\'' +
                ", remark='" + remark + '\'' +
                ", updateTime=" + updateTime +
                ", options=" + options +
                ", min=" + min +
                ", max=" + max +
                '}';
    }
}
