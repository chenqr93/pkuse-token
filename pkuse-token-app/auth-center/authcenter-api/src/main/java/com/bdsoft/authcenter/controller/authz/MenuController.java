package com.bdsoft.authcenter.controller.authz;

import com.bdsoft.authcenter.dto.authz.MenuSaveDTO;
import com.bdsoft.authcenter.service.authz.AcMenuService;
import com.bdsoft.authcenter.support.authz.AuthzAssembler;
import com.bdsoft.authcenter.vo.authz.MenuTreeVO;
import com.bdsoft.authcenter.common.annotation.OperLog;
import com.bdsoft.pkusetoken.util.PkuseResult;
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
 * 菜单管理 Controller（按应用隔离）
 */
@RestController
@RequestMapping("/api/authz/menu")
public class MenuController {

    @Resource
    private AcMenuService menuService;
    @Resource
    private AuthzAssembler authzAssembler;


    /** 应用菜单树（管理视角：含禁用节点与按钮，按 sort 升序；data={list, count}） */
    @GetMapping("/tree")
    public PkuseResult tree(String clientId) {
        List<MenuTreeVO> tree = authzAssembler.consoleTree(clientId);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("list", tree);
        data.put("count", countTree(tree));
        return PkuseResult.ok().setData(data);
    }

    /** 新增菜单 */
    @PostMapping
    @OperLog(module = "授权中心-菜单管理", action = "新增菜单")
    public PkuseResult create(@RequestBody MenuSaveDTO param) {
        return PkuseResult.ok("新增成功").setData(menuService.createMenu(param));
    }

    /** 修改菜单 */
    @PutMapping
    @OperLog(module = "授权中心-菜单管理", action = "修改菜单")
    public PkuseResult update(@RequestBody MenuSaveDTO param) {
        return PkuseResult.ok("修改成功").setData(menuService.updateMenu(param));
    }

    /** 删除菜单（有子菜单拦截；级联清理角色绑定） */
    @DeleteMapping("/{id}")
    @OperLog(module = "授权中心-菜单管理", action = "删除菜单")
    public PkuseResult delete(@PathVariable String id) {
        menuService.deleteMenu(id);
        return PkuseResult.ok("删除成功");
    }

    /** 启用/禁用菜单 */
    @PutMapping("/{id}/status")
    @OperLog(module = "授权中心-菜单管理", action = "变更菜单状态")
    public PkuseResult changeStatus(@PathVariable String id, Integer status) {
        menuService.changeStatus(id, status);
        return PkuseResult.ok(status != null && status == 0 ? "已启用" : "已禁用");
    }

    /** 统计树节点总数（含子孙） */
    private static int countTree(List<MenuTreeVO> tree) {
        int count = 0;
        for (MenuTreeVO node : tree) {
            count += 1 + countTree(node.getChildren());
        }
        return count;
    }
}
