package com.bdsoft.usercenter.region.vo;

import java.io.Serializable;

/**
 * 区划级联选项（VO：级联选择器单级选项，value 为 12 位区划编码）
 */
public class RegionOptionVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 区划编码（12 位，作为级联选项值） */
    private String code;

    /** 区划名称 */
    private String name;

    /** 是否叶子（无下一级） */
    private boolean leaf;

    public RegionOptionVO() {
    }

    public RegionOptionVO(String code, String name, boolean leaf) {
        this.code = code;
        this.name = name;
        this.leaf = leaf;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isLeaf() {
        return leaf;
    }

    public void setLeaf(boolean leaf) {
        this.leaf = leaf;
    }
}
