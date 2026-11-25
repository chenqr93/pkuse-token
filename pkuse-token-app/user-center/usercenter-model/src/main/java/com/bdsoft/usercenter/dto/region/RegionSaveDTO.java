package com.bdsoft.usercenter.dto.region;

/**
 * 区划新增 / 修改参数（数据传输对象）
 *
 * <p> 扁平化四级路径：省（必填）、市 / 县 / 街道（选填，缺级列留空，如省级行、直筒子市行）； </p>
 * <p> 最终区划编码 / 名称 / 层级由 Service 按最深层级自动推导，不由前端提交。 </p>
 */
public class RegionSaveDTO {

    /** 区划 ID（修改时必填；新增时忽略） */
    private String id;

    /** 省区划编码（12 位） */
    private String provinceCode;

    /** 省区划名称 */
    private String provinceName;

    /** 市区划编码（12 位，选填） */
    private String cityCode;

    /** 市区划名称（选填） */
    private String cityName;

    /** 县（区）区划编码（12 位，选填） */
    private String countyCode;

    /** 县（区）区划名称（选填） */
    private String countyName;

    /** 街道（乡镇）区划编码（12 位，选填） */
    private String streetCode;

    /** 街道（乡镇）区划名称（选填） */
    private String streetName;

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

    @Override
    public String toString() {
        return "RegionSaveDTO{" +
                "id='" + id + '\'' +
                ", provinceCode='" + provinceCode + '\'' +
                ", provinceName='" + provinceName + '\'' +
                ", cityCode='" + cityCode + '\'' +
                ", cityName='" + cityName + '\'' +
                ", countyCode='" + countyCode + '\'' +
                ", countyName='" + countyName + '\'' +
                ", streetCode='" + streetCode + '\'' +
                ", streetName='" + streetName + '\'' +
                '}';
    }
}
