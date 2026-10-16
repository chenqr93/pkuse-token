package com.bdsoft.usercenter.client.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bdsoft.pkusetoken.util.PkuseResult;
import com.bdsoft.usercenter.common.annotation.OperLog;
import com.bdsoft.usercenter.client.dto.ClientQueryDTO;
import com.bdsoft.usercenter.client.dto.ClientSaveDTO;
import com.bdsoft.usercenter.client.po.UcClient;
import com.bdsoft.usercenter.client.service.UcClientService;
import com.bdsoft.usercenter.authz.service.UcUserClientService;
import com.bdsoft.usercenter.client.vo.ClientVO;
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
import java.util.Map;

/**
 * 应用（Client）管理接口（仅系统管理员可访问）
 *
 * <p> 新增/修改/启用/禁用/删除后会自动同步到 SSO 客户端配置并即时生效。 </p>
 *
 * <p> 入参为 DTO、出参为 VO：实体不进入接口层。 </p>
 */
@RestController
@RequestMapping("/api/client")
public class UcClientController {

    @Resource
    private UcClientService clientService;

    @Resource
    private UcUserClientService userClientService;

    /** 分页查询 */
    @GetMapping("/page")
    public PkuseResult page(ClientQueryDTO query) {
        Page<UcClient> page = clientService.pageQuery(query);
        List<ClientVO> records = page.getRecords().stream().map(ClientVO::from).toList();
        // 补充「已授权用户数」（批量查询，避免逐行查询）
        fillAuthorizedUserCount(records);
        return PkuseResult.ok()
                .set("total", page.getTotal())
                .set("current", page.getCurrent())
                .set("size", page.getSize())
                .setData(records);
    }

    /** 查询全部应用 */
    @GetMapping("/list")
    public PkuseResult list() {
        List<ClientVO> records = clientService.listAll().stream().map(ClientVO::from).toList();
        fillAuthorizedUserCount(records);
        return PkuseResult.ok().setData(records);
    }

    /** 查询单个应用 */
    @GetMapping("/{id}")
    public PkuseResult detail(@PathVariable String id) {
        return PkuseResult.ok().setData(ClientVO.from(clientService.getByIdOrThrow(id)));
    }

    /** 新增应用 */
    @OperLog(module = "应用管理", action = "新增应用")
    @PostMapping
    public PkuseResult create(@RequestBody ClientSaveDTO param) {
        return PkuseResult.ok("新增成功").setData(ClientVO.from(clientService.createClient(param)));
    }

    /** 修改应用 */
    @OperLog(module = "应用管理", action = "修改应用")
    @PutMapping
    public PkuseResult update(@RequestBody ClientSaveDTO param) {
        return PkuseResult.ok("保存成功").setData(ClientVO.from(clientService.updateClient(param)));
    }

    /** 启用/禁用应用 */
    @OperLog(module = "应用管理", action = "启用/禁用应用")
    @PutMapping("/{id}/status")
    public PkuseResult changeStatus(@PathVariable String id, @RequestParam Integer status) {
        clientService.changeStatus(id, status);
        return PkuseResult.ok(status == UcClient.STATUS_ENABLED ? "已启用" : "已禁用");
    }

    /** 删除应用 */
    @OperLog(module = "应用管理", action = "删除应用")
    @DeleteMapping("/{id}")
    public PkuseResult delete(@PathVariable String id) {
        clientService.deleteClient(id);
        return PkuseResult.ok("删除成功");
    }

    /** 批量补充「已授权用户数」（展示字段，非数据库字段） */
    private void fillAuthorizedUserCount(List<ClientVO> records) {
        if (records.isEmpty()) {
            return;
        }
        Map<String, Integer> counts = userClientService.countByClientIds(
                records.stream().map(ClientVO::getClientId).toList());
        records.forEach(vo -> vo.setAuthorizedUserCount(counts.getOrDefault(vo.getClientId(), 0)));
    }

}
