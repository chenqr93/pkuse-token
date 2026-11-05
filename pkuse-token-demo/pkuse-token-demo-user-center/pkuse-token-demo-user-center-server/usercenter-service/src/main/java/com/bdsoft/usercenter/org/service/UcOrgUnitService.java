package com.bdsoft.usercenter.org.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.bdsoft.usercenter.org.dto.OrgUnitSaveDTO;
import com.bdsoft.usercenter.org.po.UcOrgUnit;
import com.bdsoft.usercenter.org.vo.OrgUnitTreeVO;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 组织架构管理 Service
 *
 * <p> 树形邻接表存储（parent_id，顶级为 '0'）；层级（省/市/县/乡镇街道）为档案属性， </p>
 * <p> 由管理员手动维护，不与树深度强校验。用户经 uc_user.org_id 逻辑外键归属组织。 </p>
 */
public interface UcOrgUnitService extends IService<UcOrgUnit> {

    /**
     * 组织架构树（管理视角：全部节点，按 sort 升序、同级稳定）
     *
     * <p> 过滤条件（orgName / shortName 模糊、orgLevel 精确）非空时先筛后建树，
     * 并向上补齐祖先节点保证树形连通。 </p>
     */
    List<OrgUnitTreeVO> tree(String orgName, String shortName, Integer orgLevel);

    /** 根据 ID 查询（不存在时抛业务异常） */
    UcOrgUnit getByIdOrThrow(String id);

    /** 新增组织 */
    UcOrgUnit createOrg(OrgUnitSaveDTO param);

    /** 修改组织（不支持移动子树外的级联操作；上级变更仅影响自身挂载位置） */
    UcOrgUnit updateOrg(OrgUnitSaveDTO param);

    /**
     * 删除组织
     *
     * <p> 存在下级组织或仍有用户挂载时拦截；删除为逻辑删除，显式记录删除人与时间。 </p>
     */
    void deleteOrg(String id);

    /**
     * 批量查询组织名称映射（id → orgName，供用户列表回填展示，避免逐行查询）
     */
    Map<String, String> mapNamesByIds(Collection<String> ids);

}
