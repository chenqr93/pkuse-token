package com.bdsoft.apiplatform.magic.controller;

import com.bdsoft.apiplatform.magic.po.MagicApiPermPO;
import com.bdsoft.apiplatform.magic.service.MagicApiPermService;
import com.bdsoft.pkusetoken.stp.StpUtil;
import com.bdsoft.pkusetoken.util.PkuseResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * magic-api 接口权限绑定管理（V2：接口级 RBAC）
 *
 * <p> 把 magic 接口绑定到授权中心分发的权限码；拦截器在会话校验通过后按路径查绑定并逐个 </p>
 * <p> checkPermission（权限数据来自授权中心客户端 starter，按 loginId 缓存）。 </p>
 * <p> 管理接口需登录（编辑器用户或统一登录用户）。 </p>
 */
@RestController
@RequestMapping("/magic/perms")
public class MagicApiPermController {

    private final MagicApiPermService permService;

    public MagicApiPermController(MagicApiPermService permService) {
        this.permService = permService;
    }

    /** 绑定列表 */
    @GetMapping("/list")
    public PkuseResult list() {
        StpUtil.checkLogin();
        return PkuseResult.ok().setData(permService.listAll());
    }

    /** 新增/修改绑定 */
    @PostMapping
    public PkuseResult save(@RequestBody MagicApiPermPO po) {
        StpUtil.checkLogin();
        try {
            return PkuseResult.ok("保存成功").setData(permService.saveBinding(po));
        } catch (IllegalArgumentException e) {
            return PkuseResult.error(e.getMessage());
        }
    }

    /** 删除绑定 */
    @DeleteMapping("/{id}")
    public PkuseResult delete(@PathVariable String id) {
        StpUtil.checkLogin();
        permService.deleteBinding(id);
        return PkuseResult.ok("删除成功");
    }
}
