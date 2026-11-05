package com.bdsoft.client.controller;

import com.bdsoft.pkusetoken.stp.StpUtil;
import com.bdsoft.pkusetoken.util.PkuseResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 授权中心按钮级鉴权演示接口
 *
 * <p> 权限码由授权中心「菜单管理」维护、经角色绑定分发给本应用（starter 适配 StpInterface）； </p>
 * <p> 此处以编程式校验演示（与 @PkuseCheckPermission 等价）：无权限时抛 NotPermissionException， </p>
 * <p> 由全局异常处理转为 code=403，前端按钮同时按权限码隐藏。 </p>
 */
@RestController
@RequestMapping("/demo/authz")
public class AuthzDemoController {

    /** 受「用户删除」按钮权限码保护的演示接口（zhangsan 仅 user 角色，无 sys:user:del → 403） */
    @GetMapping("/user-delete")
    public PkuseResult userDelete() {
        StpUtil.checkPermission("sys:user:del");
        return PkuseResult.ok("删除操作已执行（当前用户拥有 sys:user:del 权限）");
    }

    /** 受「用户编辑」权限码保护的演示接口（zhangsan 的 user 角色包含 sys:user:edit → 放行） */
    @GetMapping("/user-edit")
    public PkuseResult userEdit() {
        StpUtil.checkPermission("sys:user:edit");
        return PkuseResult.ok("编辑操作已执行（当前用户拥有 sys:user:edit 权限）");
    }
}
