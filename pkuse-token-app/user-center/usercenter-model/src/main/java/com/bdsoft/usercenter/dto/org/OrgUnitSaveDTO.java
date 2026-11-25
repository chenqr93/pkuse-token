package com.bdsoft.usercenter.dto.org;

/**
 * 组织新增 / 修改参数（数据传输对象）
 *
 * <p> 仅包含前端允许提交的字段：审计字段由后端维护，接收方无法直接指定。 </p>
 */
public class OrgUnitSaveDTO {

    /** 组织 ID（修改时必填；新增时忽略） */
    private String id;

    /** 上级组织 ID（为空表示顶级） */
    private String parentId;

    /** 层级：1=省 2=市 3=县 4=乡镇街道 */
    private Integer orgLevel;

    /** 区划代码（6~12 位数字，选填） */
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
        return "OrgUnitSaveDTO{" +
                "id='" + id + '\'' +
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
