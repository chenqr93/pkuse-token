package com.bdsoft.usercenter.org.vo;

import com.bdsoft.usercenter.org.po.UcOrgUnit;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 组织架构树节点（VO：管理端组织树，含全部可编辑字段供编辑回显）
 */
public class OrgUnitTreeVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;

    /** 上级组织 ID（顶级为 '0'） */
    private String parentId;

    /** 层级：1=省 2=市 3=县 4=乡镇街道 */
    private Integer orgLevel;

    /** 区划代码 */
    private String regionCode;

    /** 区划名称 */
    private String regionName;

    /** 单位名称 */
    private String orgName;

    /** 单位简称 */
    private String shortName;

    /** 统一社会信用代码 */
    private String creditCode;

    /** 同级排序 */
    private Integer sort;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

    /** 子节点 */
    private List<OrgUnitTreeVO> children = new ArrayList<>();

    public static OrgUnitTreeVO from(UcOrgUnit orgUnit) {
        if (orgUnit == null) {
            return null;
        }
        OrgUnitTreeVO vo = new OrgUnitTreeVO();
        vo.id = orgUnit.getId();
        vo.parentId = orgUnit.getParentId();
        vo.orgLevel = orgUnit.getOrgLevel();
        vo.regionCode = orgUnit.getRegionCode();
        vo.regionName = orgUnit.getRegionName();
        vo.orgName = orgUnit.getOrgName();
        vo.shortName = orgUnit.getShortName();
        vo.creditCode = orgUnit.getCreditCode();
        vo.sort = orgUnit.getSort();
        vo.remark = orgUnit.getRemark();
        vo.createTime = orgUnit.getCreateTime();
        vo.updateTime = orgUnit.getUpdateTime();
        return vo;
    }

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

    public List<OrgUnitTreeVO> getChildren() {
        return children;
    }

    public void setChildren(List<OrgUnitTreeVO> children) {
        this.children = children;
    }
}
