package com.bdsoft.usercenter.region.vo;

import com.bdsoft.usercenter.region.po.UcRegion;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 区划视图对象（扁平化一行完整路径）
 */
public class RegionVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;

    /** 省区划编码 */
    private String provinceCode;

    /** 省区划名称 */
    private String provinceName;

    /** 市区划编码 */
    private String cityCode;

    /** 市区划名称 */
    private String cityName;

    /** 县（区）区划编码 */
    private String countyCode;

    /** 县（区）区划名称 */
    private String countyName;

    /** 街道（乡镇）区划编码 */
    private String streetCode;

    /** 街道（乡镇）区划名称 */
    private String streetName;

    /** 最终区划编码 */
    private String finalCode;

    /** 最终区划名称 */
    private String finalName;

    /** 最终层级：1=省 2=市 3=县 4=乡镇街道 */
    private Integer finalLevel;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

    public static RegionVO from(UcRegion region) {
        if (region == null) {
            return null;
        }
        RegionVO vo = new RegionVO();
        vo.setId(region.getId());
        vo.setProvinceCode(region.getProvinceCode());
        vo.setProvinceName(region.getProvinceName());
        vo.setCityCode(region.getCityCode());
        vo.setCityName(region.getCityName());
        vo.setCountyCode(region.getCountyCode());
        vo.setCountyName(region.getCountyName());
        vo.setStreetCode(region.getStreetCode());
        vo.setStreetName(region.getStreetName());
        vo.setFinalCode(region.getFinalCode());
        vo.setFinalName(region.getFinalName());
        vo.setFinalLevel(region.getFinalLevel());
        vo.setCreateTime(region.getCreateTime());
        vo.setUpdateTime(region.getUpdateTime());
        return vo;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getProvinceCode() {
        return provinceCode;
    }

    public void setProvinceCode(String provinceCode) {
        this.provinceCode = provinceCode;
    }

    public String getProvinceName() {
        return provinceName;
    }

    public void setProvinceName(String provinceName) {
        this.provinceName = provinceName;
    }

    public String getCityCode() {
        return cityCode;
    }

    public void setCityCode(String cityCode) {
        this.cityCode = cityCode;
    }

    public String getCityName() {
        return cityName;
    }

    public void setCityName(String cityName) {
        this.cityName = cityName;
    }

    public String getCountyCode() {
        return countyCode;
    }

    public void setCountyCode(String countyCode) {
        this.countyCode = countyCode;
    }

    public String getCountyName() {
        return countyName;
    }

    public void setCountyName(String countyName) {
        this.countyName = countyName;
    }

    public String getStreetCode() {
        return streetCode;
    }

    public void setStreetCode(String streetCode) {
        this.streetCode = streetCode;
    }

    public String getStreetName() {
        return streetName;
    }

    public void setStreetName(String streetName) {
        this.streetName = streetName;
    }

    public String getFinalCode() {
        return finalCode;
    }

    public void setFinalCode(String finalCode) {
        this.finalCode = finalCode;
    }

    public String getFinalName() {
        return finalName;
    }

    public void setFinalName(String finalName) {
        this.finalName = finalName;
    }

    public Integer getFinalLevel() {
        return finalLevel;
    }

    public void setFinalLevel(Integer finalLevel) {
        this.finalLevel = finalLevel;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
}
