package com.bdsoft.usercenter.region.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bdsoft.pkusetoken.util.PkuseResult;
import com.bdsoft.usercenter.common.annotation.OperLog;
import com.bdsoft.usercenter.common.pojo.PageData;
import com.bdsoft.usercenter.region.dto.RegionQueryDTO;
import com.bdsoft.usercenter.region.dto.RegionSaveDTO;
import com.bdsoft.usercenter.region.po.UcRegion;
import com.bdsoft.usercenter.region.service.UcRegionService;
import com.bdsoft.usercenter.region.vo.RegionOptionVO;
import com.bdsoft.usercenter.region.vo.RegionVO;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 区划管理接口（仅系统管理员可访问）
 *
 * <p> 扁平化四级区划字典（省/市/县/乡镇街道）；级联接口供区划 / 组织等页面的选择器懒加载。 </p>
 */
@RestController
@RequestMapping("/api/region")
public class RegionController {

    @Resource
    private UcRegionService regionService;

    /** 分页查询（keyword 任一层级名称模糊或最终编码前缀；provinceCode 精确；finalLevel 精确） */
    @GetMapping("/page")
    public PkuseResult page(RegionQueryDTO query) {
        Page<UcRegion> page = regionService.pageQuery(query);
        List<RegionVO> records = page.getRecords().stream().map(RegionVO::from).toList();
        return PkuseResult.ok().setData(PageData.of(records, page.getTotal(), page.getCurrent(), page.getSize()));
    }

    /** 级联下一级选项（parentCode 为空返回省级列表；街道级返回空列表表示叶子） */
    @GetMapping("/cascade")
    public PkuseResult cascade(@RequestParam(required = false) String parentCode) {
        List<RegionOptionVO> options = regionService.cascade(parentCode);
        return PkuseResult.ok().setData(options);
    }

    /** 新增区划（最终区划编码/名称/层级按最深层级自动推导） */
    @PostMapping
    @OperLog(module = "区划管理", action = "新增区划")
    public PkuseResult create(@RequestBody RegionSaveDTO param) {
        return PkuseResult.ok("新增成功").setData(RegionVO.from(regionService.createRegion(param)));
    }

    /** 修改区划（最终区划编码不允许变更） */
    @PutMapping
    @OperLog(module = "区划管理", action = "修改区划")
    public PkuseResult update(@RequestBody RegionSaveDTO param) {
        return PkuseResult.ok("修改成功").setData(RegionVO.from(regionService.updateRegion(param)));
    }

    /** 删除区划 */
    @DeleteMapping("/{id}")
    @OperLog(module = "区划管理", action = "删除区划")
    public PkuseResult delete(@PathVariable String id) {
        regionService.deleteRegion(id);
        return PkuseResult.ok("删除成功");
    }
}
