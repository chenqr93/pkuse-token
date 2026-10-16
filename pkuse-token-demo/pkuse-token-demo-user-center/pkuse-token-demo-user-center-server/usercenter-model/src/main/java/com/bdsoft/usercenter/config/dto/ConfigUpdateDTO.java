package com.bdsoft.usercenter.config.dto;

import java.io.Serializable;
import java.util.Map;

/**
 * 规则配置批量保存参数（数据传输对象）
 */
public class ConfigUpdateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 配置键 → 配置值（仅提交需要修改的项） */
    private Map<String, String> values;

    public Map<String, String> getValues() {
        return values;
    }

    public void setValues(Map<String, String> values) {
        this.values = values;
    }

    @Override
    public String toString() {
        return "ConfigUpdateDTO{" +
                "values=" + values +
                '}';
    }
}
