package com.bdsoft.usercenter.region.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bdsoft.pkusetoken.stp.StpUtil;
import com.bdsoft.usercenter.common.exception.BizException;
import com.bdsoft.usercenter.region.dto.RegionQueryDTO;
import com.bdsoft.usercenter.region.dto.RegionSaveDTO;
import com.bdsoft.usercenter.region.mapper.UcRegionMapper;
import com.bdsoft.usercenter.region.po.UcRegion;
import com.bdsoft.usercenter.region.service.UcRegionService;
import com.bdsoft.usercenter.region.vo.RegionOptionVO;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * 区划管理 Service 实现
 *
 * <p> 校验核心：各级编码须为 12 位数字且符合「有效位 + 尾部补零」的层级形状；子级编码的有效位 </p>
 * <p> 必须以上级编码的有效位为前缀（缺级时向上跳级比对）；最终区划编码取最深层级编码并全表唯一。 </p>
 */
@Service
public class UcRegionServiceImpl extends ServiceImpl<UcRegionMapper, UcRegion> implements UcRegionService {

    /** 各级编码形状：有效位数 + 尾部补零个数 */
    private static final String PATTERN_PROVINCE = "^\\d{2}0{10}$";
    private static final String PATTERN_CITY = "^\\d{4}0{8}$";
    private static final String PATTERN_COUNTY = "^\\d{6}0{6}$";
    private static final String PATTERN_STREET = "^\\d{9}0{3}$";

    /** 各级编码的有效位长度（前缀比对用） */
    private static final int SIGNIFICANT_PROVINCE = 2;
    private static final int SIGNIFICANT_CITY = 4;
    private static final int SIGNIFICANT_COUNTY = 6;

    @Override
    public Page<UcRegion> pageQuery(RegionQueryDTO query) {
        LambdaQueryWrapper<UcRegion> wrapper = new LambdaQueryWrapper<UcRegion>();
        String keyword = trimToNull(query.getKeyword());
        if (keyword != null) {
            // 任一层级名称模糊命中，或最终区划编码前缀命中
            wrapper.and(w -> w.like(UcRegion::getFinalName, keyword)
                    .or().like(UcRegion::getProvinceName, keyword)
                    .or().like(UcRegion::getCityName, keyword)
                    .or().like(UcRegion::getCountyName, keyword)
                    .or().like(UcRegion::getStreetName, keyword)
                    .or().likeRight(UcRegion::getFinalCode, keyword));
        }
        wrapper.eq(hasText(query.getProvinceCode()), UcRegion::getProvinceCode, query.getProvinceCode())
                .eq(query.getFinalLevel() != null, UcRegion::getFinalLevel, query.getFinalLevel())
                .orderByAsc(UcRegion::getProvinceCode)
                .orderByAsc(UcRegion::getCityCode)
                .orderByAsc(UcRegion::getCountyCode)
                .orderByAsc(UcRegion::getStreetCode)
                .orderByAsc(UcRegion::getId);
        return this.page(new Page<>(query.currentOrDefault(), query.sizeOrDefault()), wrapper);
    }

    @Override
    public UcRegion createRegion(RegionSaveDTO param) {
        UcRegion region = validateAndBuild(param, null);
        this.save(region);
        return region;
    }

    @Override
    public UcRegion updateRegion(RegionSaveDTO param) {
        if (!hasText(param.getId())) {
            throw new BizException("区划 ID 不能为空");
        }
        UcRegion exists = getByIdOrThrow(param.getId());
        UcRegion region = validateAndBuild(param, exists);
        // 最终区划编码是外部引用锚点（组织、用户归属），不允许变更
        if (!region.getFinalCode().equals(exists.getFinalCode())) {
            throw new BizException("最终区划编码不允许变更（当前：" + exists.getFinalCode() + "）");
        }
        // 可选层级清空需显式 set(null)（updateById 忽略 null 字段）；纯 Wrapper 更新不触发自动填充，updateTime 显式设置
        this.update(new LambdaUpdateWrapper<UcRegion>()
                .eq(UcRegion::getId, exists.getId())
                .set(UcRegion::getProvinceCode, region.getProvinceCode())
                .set(UcRegion::getProvinceName, region.getProvinceName())
                .set(UcRegion::getCityCode, region.getCityCode())
                .set(UcRegion::getCityName, region.getCityName())
                .set(UcRegion::getCountyCode, region.getCountyCode())
                .set(UcRegion::getCountyName, region.getCountyName())
                .set(UcRegion::getStreetCode, region.getStreetCode())
                .set(UcRegion::getStreetName, region.getStreetName())
                .set(UcRegion::getFinalCode, region.getFinalCode())
                .set(UcRegion::getFinalName, region.getFinalName())
                .set(UcRegion::getFinalLevel, region.getFinalLevel())
                .set(UcRegion::getUpdateTime, LocalDateTime.now()));
        return this.getById(exists.getId());
    }

    @Override
    public void deleteRegion(String id) {
        UcRegion region = getByIdOrThrow(id);
        // 审计：记录删除人与删除时间（逻辑删除 UPDATE 仅置 deleted，此二字段需显式填充）
        this.update(new LambdaUpdateWrapper<UcRegion>()
                .eq(UcRegion::getId, region.getId())
                .set(UcRegion::getDeleteBy, currentUserId())
                .set(UcRegion::getDeleteTime, LocalDateTime.now()));
        this.removeById(region.getId());
    }

    @Override
    public List<RegionOptionVO> cascade(String parentCode) {
        String parent = trimToNull(parentCode);
        if (parent == null) {
            // 根级：省列表
            List<UcRegion> rows = this.list(new QueryWrapper<UcRegion>()
                    .select("DISTINCT province_code", "province_name")
                    .orderByAsc("province_code"));
            return toOptions(rows, UcRegion::getProvinceCode, UcRegion::getProvinceName, false);
        }
        Integer level = levelOf(parent);
        if (level == null) {
            throw new BizException("区划编码不符合 12 位层级规则：" + parent);
        }
        if (level >= UcRegion.LEVEL_TOWNSHIP) {
            // 街道为叶子层级
            return new ArrayList<>();
        }
        if (level == UcRegion.LEVEL_PROVINCE) {
            List<UcRegion> cityRows = this.list(new QueryWrapper<UcRegion>()
                    .select("DISTINCT city_code", "city_name")
                    .eq("province_code", parent)
                    .isNotNull("city_code")
                    .orderByAsc("city_code"));
            if (!cityRows.isEmpty()) {
                return toOptions(cityRows, UcRegion::getCityCode, UcRegion::getCityName, false);
            }
            // 省直辖县级（无市级行）
            List<UcRegion> countyRows = this.list(new QueryWrapper<UcRegion>()
                    .select("DISTINCT county_code", "county_name")
                    .eq("province_code", parent)
                    .isNull("city_code").isNotNull("county_code")
                    .orderByAsc("county_code"));
            if (!countyRows.isEmpty()) {
                return toOptions(countyRows, UcRegion::getCountyCode, UcRegion::getCountyName, false);
            }
            List<UcRegion> streetRows = this.list(new QueryWrapper<UcRegion>()
                    .select("DISTINCT street_code", "street_name")
                    .eq("province_code", parent)
                    .isNull("city_code").isNull("county_code").isNotNull("street_code")
                    .orderByAsc("street_code"));
            return toOptions(streetRows, UcRegion::getStreetCode, UcRegion::getStreetName, true);
        }
        if (level == UcRegion.LEVEL_CITY) {
            List<UcRegion> countyRows = this.list(new QueryWrapper<UcRegion>()
                    .select("DISTINCT county_code", "county_name")
                    .eq("city_code", parent)
                    .isNotNull("county_code")
                    .orderByAsc("county_code"));
            if (!countyRows.isEmpty()) {
                return toOptions(countyRows, UcRegion::getCountyCode, UcRegion::getCountyName, false);
            }
            // 直筒子市（无县级，街道直挂市下）；街道为叶子
            List<UcRegion> streetRows = this.list(new QueryWrapper<UcRegion>()
                    .select("DISTINCT street_code", "street_name")
                    .eq("city_code", parent)
                    .isNull("county_code").isNotNull("street_code")
                    .orderByAsc("street_code"));
            return toOptions(streetRows, UcRegion::getStreetCode, UcRegion::getStreetName, true);
        }
        // 县级 → 街道级（叶子）
        List<UcRegion> children = this.list(new QueryWrapper<UcRegion>()
                .select("DISTINCT street_code", "street_name")
                .eq("county_code", parent)
                .isNotNull("street_code")
                .orderByAsc("street_code"));
        return toOptions(children, UcRegion::getStreetCode, UcRegion::getStreetName, true);
    }

    // ------------------------- 校验与构建 -------------------------

    /**
     * 新增 / 修改统一校验并构建实体（exclude 为更新场景的原记录，用于最终编码唯一性排除自身）
     */
    private UcRegion validateAndBuild(RegionSaveDTO param, UcRegion exclude) {
        // 省级必填，形状校验
        if (!hasText(param.getProvinceCode()) || !param.getProvinceCode().matches(PATTERN_PROVINCE)) {
            throw new BizException("省区划编码需为 12 位数字且后 10 位为 0（如 130000000000）");
        }
        if (!hasText(param.getProvinceName())) {
            throw new BizException("省区划名称不能为空");
        }
        // 市 / 县 / 街道：编码与名称须成对出现，形状与前缀一致性校验（缺级时向上跳级比对前缀）
        checkLevel(param.getCityCode(), param.getCityName(), PATTERN_CITY, "市",
                param.getProvinceCode(), SIGNIFICANT_PROVINCE);
        String prefixCounty = trimToNull(param.getCityCode()) != null ? param.getCityCode() : param.getProvinceCode();
        checkLevel(param.getCountyCode(), param.getCountyName(), PATTERN_COUNTY, "县",
                prefixCounty, SIGNIFICANT_CITY);
        String prefixStreet = trimToNull(param.getCountyCode()) != null ? param.getCountyCode() : prefixCounty;
        checkLevel(param.getStreetCode(), param.getStreetName(), PATTERN_STREET, "街道",
                prefixStreet, SIGNIFICANT_COUNTY);

        // 最终区划 = 最深层级
        UcRegion region = new UcRegion();
        region.setProvinceCode(param.getProvinceCode());
        region.setProvinceName(param.getProvinceName().trim());
        region.setCityCode(trimToNull(param.getCityCode()));
        region.setCityName(trimToNull(param.getCityName()));
        region.setCountyCode(trimToNull(param.getCountyCode()));
        region.setCountyName(trimToNull(param.getCountyName()));
        region.setStreetCode(trimToNull(param.getStreetCode()));
        region.setStreetName(trimToNull(param.getStreetName()));
        if (region.getStreetCode() != null) {
            region.setFinalCode(region.getStreetCode());
            region.setFinalName(region.getStreetName());
            region.setFinalLevel(UcRegion.LEVEL_TOWNSHIP);
        } else if (region.getCountyCode() != null) {
            region.setFinalCode(region.getCountyCode());
            region.setFinalName(region.getCountyName());
            region.setFinalLevel(UcRegion.LEVEL_COUNTY);
        } else if (region.getCityCode() != null) {
            region.setFinalCode(region.getCityCode());
            region.setFinalName(region.getCityName());
            region.setFinalLevel(UcRegion.LEVEL_CITY);
        } else {
            region.setFinalCode(region.getProvinceCode());
            region.setFinalName(region.getProvinceName());
            region.setFinalLevel(UcRegion.LEVEL_PROVINCE);
        }

        // 最终区划编码全表唯一（更新场景排除自身）
        LambdaQueryWrapper<UcRegion> uniqueWrapper = new LambdaQueryWrapper<UcRegion>()
                .eq(UcRegion::getFinalCode, region.getFinalCode());
        if (exclude != null) {
            uniqueWrapper.ne(UcRegion::getId, exclude.getId());
        }
        if (this.count(uniqueWrapper) > 0) {
            throw new BizException("最终区划编码已存在：" + region.getFinalCode());
        }
        return region;
    }

    /** 单级校验：编码与名称须成对出现；编码形状与上级有效位前缀一致性 */
    private void checkLevel(String code, String name, String pattern, String levelLabel,
                            String parentCode, int parentSignificant) {
        String trimmedCode = trimToNull(code);
        String trimmedName = trimToNull(name);
        if (trimmedCode == null && trimmedName == null) {
            return;
        }
        if (trimmedCode == null) {
            throw new BizException(levelLabel + "区划名称需与编码同时填写");
        }
        if (trimmedName == null) {
            throw new BizException(levelLabel + "区划编码需与名称同时填写");
        }
        if (!trimmedCode.matches(pattern)) {
            throw new BizException(levelLabel + "区划编码需为 12 位数字且符合层级补零规则：" + trimmedCode);
        }
        if (parentCode == null || !trimmedCode.startsWith(parentCode.substring(0, parentSignificant))) {
            throw new BizException(levelLabel + "区划编码需与上级区划编码前缀一致：" + trimmedCode);
        }
    }

    /** 行集转级联选项 */
    private static List<RegionOptionVO> toOptions(List<UcRegion> rows,
                                                  Function<UcRegion, String> codeGetter,
                                                  Function<UcRegion, String> nameGetter,
                                                  boolean leaf) {
        List<RegionOptionVO> options = new ArrayList<>();
        for (UcRegion row : rows) {
            options.add(new RegionOptionVO(codeGetter.apply(row), nameGetter.apply(row), leaf));
        }
        return options;
    }

    /** 按编码尾部补零规则判定层级 */
    private static Integer levelOf(String code) {
        if (code == null) {
            return null;
        }
        if (code.matches(PATTERN_PROVINCE)) {
            return UcRegion.LEVEL_PROVINCE;
        }
        if (code.matches(PATTERN_CITY)) {
            return UcRegion.LEVEL_CITY;
        }
        if (code.matches(PATTERN_COUNTY)) {
            return UcRegion.LEVEL_COUNTY;
        }
        if (code.matches(PATTERN_STREET)) {
            return UcRegion.LEVEL_TOWNSHIP;
        }
        return null;
    }

    private UcRegion getByIdOrThrow(String id) {
        UcRegion region = hasText(id) ? this.getById(id) : null;
        if (region == null) {
            throw new BizException("区划不存在");
        }
        return region;
    }

    private static String currentUserId() {
        try {
            Object loginId = StpUtil.getLoginIdDefaultNull();
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
