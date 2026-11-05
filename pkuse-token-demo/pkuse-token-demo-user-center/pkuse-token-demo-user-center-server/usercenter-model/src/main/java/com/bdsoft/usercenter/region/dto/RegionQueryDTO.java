package com.bdsoft.usercenter.region.dto;

import com.bdsoft.usercenter.common.pojo.PageQueryDTO;

/**
 * 区划分页查询参数（数据传输对象）
 */
public class RegionQueryDTO extends PageQueryDTO {

    /** 关键字（任一层级名称模糊匹配，或最终区划编码前缀匹配，可空） */
    private String keyword;

    /** 省区划编码（精确匹配，可空） */
    private String provinceCode;

    /** 最终层级：1=省 2=市 3=县 4=乡镇街道（可空表示全部） */
    private Integer finalLevel;

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public String getProvinceCode() {
        return provinceCode;
    }

    public void setProvinceCode(String provinceCode) {
        this.provinceCode = provinceCode;
    }

    public Integer getFinalLevel() {
        return finalLevel;
    }

    public void setFinalLevel(Integer finalLevel) {
        this.finalLevel = finalLevel;
    }

    @Override
    public String toString() {
        return "RegionQueryDTO{" + super.toString() +
                ", keyword='" + keyword + '\'' +
                ", provinceCode='" + provinceCode + '\'' +
                ", finalLevel=" + finalLevel +
                '}';
    }
}
