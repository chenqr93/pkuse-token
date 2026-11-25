package com.bdsoft.usercenter.service.org.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bdsoft.usercenter.common.exception.BizException;
import com.bdsoft.usercenter.dto.org.OrgUnitSaveDTO;
import com.bdsoft.usercenter.mapper.org.UcOrgUnitMapper;
import com.bdsoft.usercenter.po.org.UcOrgUnit;
import com.bdsoft.usercenter.service.org.UcOrgUnitService;
import com.bdsoft.usercenter.vo.org.OrgUnitTreeVO;
import com.bdsoft.usercenter.mapper.user.UcUserMapper;
import com.bdsoft.usercenter.po.user.UcUser;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 组织架构管理 Service 实现
 *
 * <p> 建树算法沿用授权中心菜单树（一次查全量 → 按 parentId 分组 → 递归组装，sort 升序 + id 稳定）； </p>
 * <p> 用户挂载校验直接注入 UcUserMapper 计数（避免与 UcUserService 相互注入形成循环依赖）。 </p>
 */
@Service
public class UcOrgUnitServiceImpl extends ServiceImpl<UcOrgUnitMapper, UcOrgUnit> implements UcOrgUnitService {

    /** 统一社会信用代码格式（18 位：2 位登记管理部门+机构类别 + 6 位区划 + 9 位主体标识码 + 校验位，不含 I/O/S/V/Z） */
    private static final Pattern CREDIT_CODE_PATTERN =
            Pattern.compile("[0-9A-HJ-NPQRTUWXY]{2}\\d{6}[0-9A-HJ-NPQRTUWXY]{10}");

    /** 用户挂载计数（逻辑删除自动过滤；跨域 Mapper 直查，避免 Service 循环依赖） */
    @Resource
    private UcUserMapper userMapper;

    @Override
    public List<OrgUnitTreeVO> tree(String orgName, String shortName, Integer orgLevel) {
        List<UcOrgUnit> all = this.list(new LambdaQueryWrapper<UcOrgUnit>()
                .orderByAsc(UcOrgUnit::getSort)
                .orderByAsc(UcOrgUnit::getId));
        if (!hasText(orgName) && !hasText(shortName) && orgLevel == null) {
            return buildTree(all, null);
        }
        // 过滤命中节点，并向上补齐祖先（保证树形连通，与授权中心分发菜单树同套路）
        Map<String, UcOrgUnit> byId = all.stream()
                .collect(Collectors.toMap(UcOrgUnit::getId, u -> u, (a, b) -> a));
        Set<String> includeIds = new LinkedHashSet<>();
        for (UcOrgUnit unit : all) {
            if (matches(unit, orgName, shortName, orgLevel)) {
                String cursor = unit.getId();
                while (cursor != null && includeIds.add(cursor)) {
                    UcOrgUnit current = byId.get(cursor);
                    cursor = current == null ? null : current.getParentId();
                }
            }
        }
        return buildTree(all, includeIds);
    }

    @Override
    public UcOrgUnit getByIdOrThrow(String id) {
        UcOrgUnit orgUnit = hasText(id) ? this.getById(id) : null;
        if (orgUnit == null) {
            throw new BizException("组织不存在");
        }
        return orgUnit;
    }

    @Override
    public UcOrgUnit createOrg(OrgUnitSaveDTO param) {
        validateSave(param, null);
        UcOrgUnit orgUnit = new UcOrgUnit();
        copyFields(param, orgUnit);
        orgUnit.setSort(param.getSort() == null ? 0 : param.getSort());
        this.save(orgUnit);
        return orgUnit;
    }

    @Override
    public UcOrgUnit updateOrg(OrgUnitSaveDTO param) {
        if (!hasText(param.getId())) {
            throw new BizException("组织 ID 不能为空");
        }
        UcOrgUnit exists = this.getByIdOrThrow(param.getId());
        validateSave(param, exists);

        // 可选字段清空需显式 set(null)（updateById 忽略 null 字段），故全字段走 UpdateWrapper；
        // 纯 Wrapper 更新不触发 MetaObjectHandler 自动填充，updateTime 需显式设置
        String parentId = hasText(param.getParentId()) ? param.getParentId() : UcOrgUnit.ROOT_PARENT_ID;
        this.update(new LambdaUpdateWrapper<UcOrgUnit>()
                .eq(UcOrgUnit::getId, exists.getId())
                .set(UcOrgUnit::getParentId, parentId)
                .set(UcOrgUnit::getOrgLevel, param.getOrgLevel())
                .set(UcOrgUnit::getRegionCode, trimToNull(param.getRegionCode()))
                .set(UcOrgUnit::getRegionName, param.getRegionName().trim())
                .set(UcOrgUnit::getOrgName, param.getOrgName().trim())
                .set(UcOrgUnit::getShortName, trimToNull(param.getShortName()))
                .set(UcOrgUnit::getCreditCode, trimToNull(param.getCreditCode()))
                .set(UcOrgUnit::getSort, param.getSort() == null ? exists.getSort() : param.getSort())
                .set(UcOrgUnit::getRemark, trimToNull(param.getRemark()))
                .set(UcOrgUnit::getUpdateTime, LocalDateTime.now()));
        return this.getById(exists.getId());
    }

    @Override
    public void deleteOrg(String id) {
        UcOrgUnit orgUnit = this.getByIdOrThrow(id);
        long childCount = this.count(new LambdaQueryWrapper<UcOrgUnit>()
                .eq(UcOrgUnit::getParentId, orgUnit.getId()));
        if (childCount > 0) {
            throw new BizException("该组织存在下级组织，请先删除下级");
        }
        long userCount = userMapper.selectCount(new LambdaQueryWrapper<UcUser>()
                .eq(UcUser::getOrgId, orgUnit.getId()));
        if (userCount > 0) {
            throw new BizException("该组织下存在用户，请先移除用户归属");
        }
        // 审计：记录删除人与删除时间（逻辑删除 UPDATE 仅置 deleted，此二字段需显式填充）
        this.update(new LambdaUpdateWrapper<UcOrgUnit>()
                .eq(UcOrgUnit::getId, orgUnit.getId())
                .set(UcOrgUnit::getDeleteBy, currentUserId())
                .set(UcOrgUnit::getDeleteTime, LocalDateTime.now()));
        this.removeById(orgUnit.getId());
    }

    @Override
    public Map<String, String> mapNamesByIds(Collection<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyMap();
        }
        List<String> distinctIds = ids.stream().filter(Objects::nonNull).distinct().collect(Collectors.toList());
        if (distinctIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return this.listByIds(distinctIds).stream()
                .collect(Collectors.toMap(UcOrgUnit::getId, UcOrgUnit::getOrgName, (a, b) -> a));
    }

    // ------------------------- 校验 -------------------------

    /** 新增/修改统一校验（exclude 为更新场景的原记录，用于排除自身的唯一性校验） */
    private void validateSave(OrgUnitSaveDTO param, UcOrgUnit exclude) {
        if (!hasText(param.getOrgName())) {
            throw new BizException("单位名称不能为空");
        }
        Integer level = param.getOrgLevel();
        if (level == null || level < UcOrgUnit.LEVEL_PROVINCE || level > UcOrgUnit.LEVEL_TOWNSHIP) {
            throw new BizException("层级不合法（1=省 2=市 3=县 4=乡镇街道）");
        }
        if (!hasText(param.getRegionName())) {
            throw new BizException("区划名称不能为空");
        }
        String regionCode = trimToNull(param.getRegionCode());
        if (regionCode != null && !regionCode.matches("\\d{6,12}")) {
            throw new BizException("区划代码需为 6~12 位数字：" + regionCode);
        }
        String creditCode = trimToNull(param.getCreditCode());
        if (creditCode != null && !CREDIT_CODE_PATTERN.matcher(creditCode).matches()) {
            throw new BizException("统一社会信用代码格式不正确（18 位）：" + creditCode);
        }

        // 父级校验：存在；更新时不可挂到自身或其子孙下
        String parentId = hasText(param.getParentId()) ? param.getParentId() : UcOrgUnit.ROOT_PARENT_ID;
        if (!UcOrgUnit.ROOT_PARENT_ID.equals(parentId)) {
            UcOrgUnit parent = this.getById(parentId);
            if (parent == null) {
                throw new BizException("上级组织不存在");
            }
            if (exclude != null && isSelfOrDescendant(parent, exclude.getId())) {
                throw new BizException("上级组织不能选择自身或其子孙节点");
            }
        }

        // 同上级下单位名称唯一（更新场景排除自身；条件值必须显式分支，避免条件参数被急切求值）
        LambdaQueryWrapper<UcOrgUnit> nameWrapper = new LambdaQueryWrapper<UcOrgUnit>()
                .eq(UcOrgUnit::getParentId, parentId)
                .eq(UcOrgUnit::getOrgName, param.getOrgName().trim());
        if (exclude != null) {
            nameWrapper.ne(UcOrgUnit::getId, exclude.getId());
        }
        if (this.count(nameWrapper) > 0) {
            throw new BizException("同一上级下已存在同名单位：" + param.getOrgName());
        }
    }

    /** 判断 parent 是否为 orgId 自身或其子孙节点（沿 parent.parentId 向上回溯） */
    private boolean isSelfOrDescendant(UcOrgUnit parent, String orgId) {
        String cursor = parent.getId();
        while (cursor != null && !UcOrgUnit.ROOT_PARENT_ID.equals(cursor)) {
            if (cursor.equals(orgId)) {
                return true;
            }
            UcOrgUnit current = this.getById(cursor);
            cursor = current == null ? null : current.getParentId();
        }
        return false;
    }

    /** 过滤条件匹配（orgName / shortName 模糊包含，orgLevel 精确） */
    private static boolean matches(UcOrgUnit unit, String orgName, String shortName, Integer orgLevel) {
        if (hasText(orgName) && (unit.getOrgName() == null || !unit.getOrgName().contains(orgName.trim()))) {
            return false;
        }
        if (hasText(shortName) && (unit.getShortName() == null || !unit.getShortName().contains(shortName.trim()))) {
            return false;
        }
        return orgLevel == null || orgLevel.equals(unit.getOrgLevel());
    }

    private void copyFields(OrgUnitSaveDTO param, UcOrgUnit orgUnit) {
        orgUnit.setParentId(hasText(param.getParentId()) ? param.getParentId() : UcOrgUnit.ROOT_PARENT_ID);
        orgUnit.setOrgLevel(param.getOrgLevel());
        orgUnit.setRegionCode(trimToNull(param.getRegionCode()));
        orgUnit.setRegionName(param.getRegionName().trim());
        orgUnit.setOrgName(param.getOrgName().trim());
        orgUnit.setShortName(trimToNull(param.getShortName()));
        orgUnit.setCreditCode(trimToNull(param.getCreditCode()));
        orgUnit.setRemark(trimToNull(param.getRemark()));
    }

    // ------------------------- 建树 -------------------------

    /** 构树：includeIds 为 null 表示全部节点；按 sort 升序、同级稳定 */
    private List<OrgUnitTreeVO> buildTree(List<UcOrgUnit> all, Set<String> includeIds) {
        Map<String, List<UcOrgUnit>> byParent = new HashMap<>();
        for (UcOrgUnit unit : all) {
            if (includeIds == null || includeIds.contains(unit.getId())) {
                byParent.computeIfAbsent(unit.getParentId(), k -> new ArrayList<>()).add(unit);
            }
        }
        Comparator<UcOrgUnit> bySort = Comparator
                .comparing((UcOrgUnit u) -> u.getSort() == null ? 0 : u.getSort())
                .thenComparing(UcOrgUnit::getId, Comparator.nullsLast(Comparator.naturalOrder()));
        return buildChildren(UcOrgUnit.ROOT_PARENT_ID, byParent, bySort);
    }

    private List<OrgUnitTreeVO> buildChildren(String parentId,
                                              Map<String, List<UcOrgUnit>> byParent,
                                              Comparator<UcOrgUnit> bySort) {
        List<UcOrgUnit> children = byParent.get(parentId);
        if (children == null || children.isEmpty()) {
            return Collections.emptyList();
        }
        children.sort(bySort);
        List<OrgUnitTreeVO> result = new ArrayList<>();
        for (UcOrgUnit unit : children) {
            OrgUnitTreeVO vo = OrgUnitTreeVO.from(unit);
            vo.setChildren(buildChildren(unit.getId(), byParent, bySort));
            result.add(vo);
        }
        return result;
    }

    private static String currentUserId() {
        try {
            Object loginId = com.bdsoft.pkusetoken.stp.StpUtil.getLoginIdDefaultNull();
            return loginId == null ? null : loginId.toString();
        } catch (Exception e) {
            return null;
        }
    }

    private static boolean hasText(String text) {
        return text != null && !text.isBlank();
    }

    private static String trimToNull(String text) {
        return hasText(text) ? text.trim() : null;
    }
}
