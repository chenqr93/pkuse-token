package com.bdsoft.usercenter.org.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.bdsoft.usercenter.common.pojo.AbstractAuditPO;

/**
 * 用户中心-组织架构表（uc_org_unit）
 *
 * <p> 省 / 市 / 县 / 乡镇街道 四级组织档案，树形邻接表存储（parent_id，顶级为 '0'）。 </p>
 * <p> org_level 为档案属性，由管理员手动维护（允许与树深度不一致）；用户经 </p>
 * <p> uc_user.org_id 逻辑外键归属组织，不建数据库约束。 </p>
 */
@TableName("uc_org_unit")
public class UcOrgUnit extends AbstractAuditPO {

    private static final long serialVersionUID = 1L;

    /** 层级：省 */
    public static final int LEVEL_PROVINCE = 1;

    /** 层级：市 */
    public static final int LEVEL_CITY = 2;

    /** 层级：县（区） */
    public static final int LEVEL_COUNTY = 3;

    /** 层级：乡镇（街道） */
    public static final int LEVEL_TOWNSHIP = 4;

    /** 顶级组织的父 ID 约定值 */
    public static final String ROOT_PARENT_ID = "0";

    /** 主键（32 位 UUID 字符串） */
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    /** 上级组织 ID（顶级为 '0'） */
    private String parentId;

    /** 层级：1=省 2=市 3=县 4=乡镇街道 */
    private Integer orgLevel;

    /** 区划代码（行政区划代码，6~12 位数字，选填） */
    private String regionCode;

    /** 区划名称 */
    private String regionName;

    /** 单位名称（同上级下唯一） */
    private String orgName;

    /** 单位简称 */
    private String shortName;

    /** 统一社会信用代码（18 位，选填） */
    private String creditCode;

    /** 同级排序（升序） */
    private Integer sort;

    /** 备注 */
    private String remark;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public Integer getOrgLevel() {
        return orgLevel;
    }

    public void setOrgLevel(Integer orgLevel) {
        this.orgLevel = orgLevel;
    }

    public String getRegionCode() {
        return regionCode;
    }

    public void setRegionCode(String regionCode) {
        this.regionCode = regionCode;
    }

    public String getRegionName() {
        return regionName;
    }

    public void setRegionName(String regionName) {
        this.regionName = regionName;
    }

    public String getOrgName() {
        return orgName;
    }

    public void setOrgName(String orgName) {
        this.orgName = orgName;
    }

    public String getShortName() {
        return shortName;
    }

    public void setShortName(String shortName) {
        this.shortName = shortName;
    }

    public String getCreditCode() {
        return creditCode;
    }

    public void setCreditCode(String creditCode) {
        this.creditCode = creditCode;
    }

    public Integer getSort() {
        return sort;
    }

    public void setSort(Integer sort) {
        this.sort = sort;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    @Override
    public String toString() {
        return "UcOrgUnit{" + super.toString() +
                ", id='" + id + '\'' +
                ", parentId='" + parentId + '\'' +
                ", orgLevel=" + orgLevel +
                ", regionCode='" + regionCode + '\'' +
                ", regionName='" + regionName + '\'' +
                ", orgName='" + orgName + '\'' +
                ", shortName='" + shortName + '\'' +
                ", creditCode='" + creditCode + '\'' +
                ", sort=" + sort +
                ", remark='" + remark + '\'' +
                '}';
    }
}
