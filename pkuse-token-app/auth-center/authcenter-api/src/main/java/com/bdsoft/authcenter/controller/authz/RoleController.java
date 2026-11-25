package com.bdsoft.authcenter.controller.authz;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bdsoft.authcenter.dto.authz.RoleQueryDTO;
import com.bdsoft.authcenter.dto.authz.RoleSaveDTO;
import com.bdsoft.authcenter.po.authz.AcRole;
import com.bdsoft.authcenter.service.authz.AcRoleService;
import com.bdsoft.authcenter.vo.authz.RoleVO;
import com.bdsoft.authcenter.common.annotation.OperLog;
import com.bdsoft.authcenter.common.pojo.PageData;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 角色管理 Controller（按应用隔离）
 */
@RestController
@RequestMapping("/api/authz/role")
public class RoleController {

    @Resource
    private AcRoleService roleService;


    /** 角色分页（按应用过滤，回填已绑菜单数/已授权用户数） */
    @GetMapping("/page")
    public PkuseResult page(RoleQueryDTO query) {
        Page<AcRole> page = roleService.pageQuery(query);
        List<String> roleIds = page.getRecords().stream().map(AcRole::getId).collect(Collectors.toList());
        Map<String, Integer> menuCounts = roleService.countMenusByRoleIds(roleIds);
        Map<String, Integer> userCounts = roleService.countUsersByRoleIds(roleIds);
        List<RoleVO> records = page.getRecords().stream().map(role -> {
            RoleVO vo = RoleVO.from(role);
            vo.setMenuCount(menuCounts.getOrDefault(role.getId(), 0));
            vo.setUserCount(userCounts.getOrDefault(role.getId(), 0));
            return vo;
        }).collect(Collectors.toList());
        return PkuseResult.ok().setData(PageData.of(records, page.getTotal(), page.getCurrent(), page.getSize()));
    }

    /** 新增角色 */
    @PostMapping
    @OperLog(module = "授权中心-角色管理", action = "新增角色")
    public PkuseResult create(@RequestBody RoleSaveDTO param) {
        return PkuseResult.ok("新增成功").setData(roleService.createRole(param));
    }

    /** 修改角色 */
    @PutMapping
    @OperLog(module = "授权中心-角色管理", action = "修改角色")
    public PkuseResult update(@RequestBody RoleSaveDTO param) {
        return PkuseResult.ok("修改成功").setData(roleService.updateRole(param));
    }

    /** 删除角色（级联清理角色-菜单与用户-角色绑定） */
    @DeleteMapping("/{id}")
    @OperLog(module = "授权中心-角色管理", action = "删除角色")
    public PkuseResult delete(@PathVariable String id) {
        roleService.deleteRole(id);
        return PkuseResult.ok("删除成功");
    }

    /** 启用/禁用角色 */
    @PutMapping("/{id}/status")
    @OperLog(module = "授权中心-角色管理", action = "变更角色状态")
    public PkuseResult changeStatus(@PathVariable String id, Integer status) {
        roleService.changeStatus(id, status);
        return PkuseResult.ok(status != null && status == 0 ? "已启用" : "已禁用");
    }

    /** 角色已绑菜单 ID 列表（勾选回显） */
    @GetMapping("/{id}/menuIds")
    public PkuseResult menuIds(@PathVariable String id) {
        return PkuseResult.ok().setData(roleService.listMenuIds(id));
    }

    /** 覆盖式保存角色菜单（body: menuIds 数组） */
    @PutMapping("/{id}/menus")
    @OperLog(module = "授权中心-角色管理", action = "保存角色菜单")
    public PkuseResult bindMenus(@PathVariable String id, @RequestBody List<String> menuIds) {
        roleService.bindMenus(id, menuIds);
        return PkuseResult.ok("保存成功");
    }
}
