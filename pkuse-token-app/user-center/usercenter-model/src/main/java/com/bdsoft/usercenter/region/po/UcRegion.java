package com.bdsoft.usercenter.region.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.bdsoft.usercenter.common.pojo.AbstractAuditPO;

/**
 * 用户中心-区划表（uc_region）
 *
 * <p> 省 / 市 / 县 / 乡镇街道 四级扁平化区划字典：每行为一条完整路径（省、市、县、街道 </p>
 * <p> 各级编码与名称 + 最终区划编码/名称/层级）。编码为 12 位国标统计口径（有效位 + 尾部补零， </p>
 * <p> 如石牌街道 440106001000）；直辖市市级按国标以「市辖区」占位；直筒子市县级列留空。 </p>
 */
@TableName("uc_region")
public class UcRegion extends AbstractAuditPO {

    private static final long serialVersionUID = 1L;

    /** 最终层级：省 */
    public static final int LEVEL_PROVINCE = 1;

    /** 最终层级：市 */
    public static final int LEVEL_CITY = 2;

    /** 最终层级：县（区） */
    public static final int LEVEL_COUNTY = 3;

    /** 最终层级：乡镇（街道） */
    public static final int LEVEL_TOWNSHIP = 4;

    /** 主键（32 位 UUID 字符串） */
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    /** 省区划编码（12 位：有效位 + 尾部补零） */
    private String provinceCode;

    /** 省区划名称 */
    private String provinceName;

    /** 市区划编码（12 位；直辖市按国标为「市辖区」） */
    private String cityCode;

    /** 市区划名称 */
    private String cityName;

    /** 县（区）区划编码（12 位；直筒子市留空） */
    private String countyCode;

    /** 县（区）区划名称 */
    private String countyName;

    /** 街道（乡镇）区划编码（12 位） */
    private String streetCode;

    /** 街道（乡镇）区划名称 */
    private String streetName;

    /** 最终区划编码（最深层级的 12 位编码，全表唯一） */
    private String finalCode;

    /** 最终区划名称（最深层级名称） */
    private String finalName;

    /** 最终层级：1=省 2=市 3=县 4=乡镇街道（由最终区划编码自动推导） */
    private Integer finalLevel;

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

    @Override
    public String toString() {
        return "UcRegion{" + super.toString() +
                ", id='" + id + '\'' +
                ", provinceCode='" + provinceCode + '\'' +
                ", provinceName='" + provinceName + '\'' +
                ", cityCode='" + cityCode + '\'' +
                ", cityName='" + cityName + '\'' +
                ", countyCode='" + countyCode + '\'' +
                ", countyName='" + countyName + '\'' +
                ", streetCode='" + streetCode + '\'' +
                ", streetName='" + streetName + '\'' +
                ", finalCode='" + finalCode + '\'' +
                ", finalName='" + finalName + '\'' +
                ", finalLevel=" + finalLevel +
                '}';
    }
}
