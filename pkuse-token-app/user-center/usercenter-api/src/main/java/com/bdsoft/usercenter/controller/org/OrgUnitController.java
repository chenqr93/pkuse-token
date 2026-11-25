package com.bdsoft.usercenter.controller.org;

import com.bdsoft.pkusetoken.util.PkuseResult;
import com.bdsoft.usercenter.common.annotation.OperLog;
import com.bdsoft.usercenter.dto.org.OrgUnitSaveDTO;
import com.bdsoft.usercenter.service.org.UcOrgUnitService;
import com.bdsoft.usercenter.vo.org.OrgUnitTreeVO;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 组织架构管理接口（仅系统管理员可访问）
 *
 * <p> 树形组织（省/市/县/乡镇街道），按 sort 升序返回；用户归属组织经用户管理维护。 </p>
 */
@RestController
@RequestMapping("/api/org")
public class OrgUnitController {

    @Resource
    private UcOrgUnitService orgUnitService;

    /**
     * 组织架构树（管理视角：全部节点）
     *
     * <p> 可选过滤：orgName / shortName 模糊、orgLevel 精确；命中节点向上补齐祖先保证树形连通。 </p>
     */
    @GetMapping("/tree")
    public PkuseResult tree(String orgName, String shortName, Integer orgLevel) {
        List<OrgUnitTreeVO> tree = orgUnitService.tree(orgName, shortName, orgLevel);
        // 树形数据统一收进 data：{list, count}
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("list", tree);
        data.put("count", countTree(tree));
        return PkuseResult.ok().setData(data);
    }

    /** 新增组织 */
    @PostMapping
    @OperLog(module = "组织管理", action = "新增组织")
    public PkuseResult create(@RequestBody OrgUnitSaveDTO param) {
        return PkuseResult.ok("新增成功").setData(OrgUnitTreeVO.from(orgUnitService.createOrg(param)));
    }

    /** 修改组织 */
    @PutMapping
    @OperLog(module = "组织管理", action = "修改组织")
    public PkuseResult update(@RequestBody OrgUnitSaveDTO param) {
        return PkuseResult.ok("修改成功").setData(OrgUnitTreeVO.from(orgUnitService.updateOrg(param)));
    }

    /** 删除组织（有下级组织或仍有用户挂载时拦截） */
    @DeleteMapping("/{id}")
    @OperLog(module = "组织管理", action = "删除组织")
    public PkuseResult delete(@PathVariable String id) {
        orgUnitService.deleteOrg(id);
        return PkuseResult.ok("删除成功");
    }

    /** 统计树节点总数（含子孙） */
    private static int countTree(List<OrgUnitTreeVO> tree) {
        int count = 0;
        for (OrgUnitTreeVO node : tree) {
            count += 1 + countTree(node.getChildren());
        }
        return count;
    }
}
