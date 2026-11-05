package com.bdsoft.usercenter.region.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.bdsoft.usercenter.region.dto.RegionQueryDTO;
import com.bdsoft.usercenter.region.dto.RegionSaveDTO;
import com.bdsoft.usercenter.region.po.UcRegion;
import com.bdsoft.usercenter.region.vo.RegionOptionVO;

import java.util.List;

/**
 * 区划管理 Service
 *
 * <p> 省 / 市 / 县 / 乡镇街道 四级扁平化区划字典：每行为一条完整路径，最终区划编码（12 位国标 </p>
 * <p> 统计口径：有效位 + 尾部补零）全表唯一，层级由编码自动推导维护。 </p>
 */
public interface UcRegionService extends IService<UcRegion> {

    /**
     * 分页查询
     *
     * <p> keyword 对任一层级名称模糊匹配或按最终编码前缀匹配；provinceCode 精确；finalLevel 精确。 </p>
     */
    Page<UcRegion> pageQuery(RegionQueryDTO query);

    /** 新增区划（最终区划编码/名称/层级按最深层级自动推导） */
    UcRegion createRegion(RegionSaveDTO param);

    /** 修改区划（不支持变更最终区划编码，避免破坏组织等外部引用） */
    UcRegion updateRegion(RegionSaveDTO param);

    /**
     * 删除区划
     *
     * <p> 字典表直接逻辑删除：组织等外部引用存的是区划文本代码，删除不产生数据库约束冲突。 </p>
     */
    void deleteRegion(String id);

    /**
     * 级联下一级选项（供前端 el-cascader 懒加载）
     *
     * <p> parentCode 为空返回省级列表；否则按编码层级返回下一级去重列表， </p>
     * <p> 市下无县级（直筒子市）时回退返回街道级；街道级为叶子返回空列表。 </p>
     */
    List<RegionOptionVO> cascade(String parentCode);

}
