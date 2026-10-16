package com.bdsoft.usercenter.user.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bdsoft.pkusetoken.util.PkuseResult;
import com.bdsoft.usercenter.common.annotation.OperLog;
import com.bdsoft.usercenter.authz.dto.ClientBatchBindDTO;
import com.bdsoft.usercenter.user.dto.UserQueryDTO;
import com.bdsoft.usercenter.user.dto.UserSaveDTO;
import com.bdsoft.usercenter.user.po.UcUser;
import com.bdsoft.usercenter.auth.service.AuthService;
import com.bdsoft.usercenter.authz.service.UcUserClientService;
import com.bdsoft.usercenter.device.service.UcUserDeviceService;
import com.bdsoft.usercenter.user.service.UcUserService;
import com.bdsoft.usercenter.device.vo.DeviceVO;
import com.bdsoft.usercenter.user.vo.UserVO;
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
 * 用户管理接口（仅系统管理员可访问）
 *
 * <p> 入参为 DTO、出参为 VO：实体不进入接口层。 </p>
 */
@RestController
@RequestMapping("/api/user")
public class UcUserController {

    @Resource
    private UcUserService userService;

    @Resource
    private UcUserClientService userClientService;

    @Resource
    private UcUserDeviceService userDeviceService;

    @Resource
    private AuthService authService;

    /** 分页查询 */
    @GetMapping("/page")
    public PkuseResult page(UserQueryDTO query) {
        Page<UcUser> page = userService.pageQuery(query);
        List<UserVO> records = page.getRecords().stream().map(UserVO::from).toList();
        // 补充「已授权应用数」「已绑定设备数」（批量查询，避免逐行查询）
        if (!records.isEmpty()) {
            List<String> userIds = records.stream().map(UserVO::getId).toList();
            Map<String, Integer> clientCounts = userClientService.countByUserIds(userIds);
            Map<String, Integer> deviceCounts = userDeviceService.countByUserIds(userIds);
            records.forEach(vo -> {
                vo.setAuthClientCount(clientCounts.getOrDefault(vo.getId(), 0));
                vo.setDeviceCount(deviceCounts.getOrDefault(vo.getId(), 0));
            });
        }
        return PkuseResult.ok()
                .set("total", page.getTotal())
                .set("current", page.getCurrent())
                .set("size", page.getSize())
                .setData(records);
    }

    /** 查询单个用户 */
    @GetMapping("/{id}")
    public PkuseResult detail(@PathVariable String id) {
        return PkuseResult.ok().setData(UserVO.from(userService.getByIdOrThrow(id)));
    }

    /** 新增用户（密码为空时使用「规则配置 - 系统默认密码」） */
    @OperLog(module = "用户管理", action = "新增用户")
    @PostMapping
    public PkuseResult create(@RequestBody UserSaveDTO param) {
        return PkuseResult.ok("新增成功").setData(UserVO.from(userService.createUser(param)));
    }

    /** 修改用户 */
    @OperLog(module = "用户管理", action = "修改用户")
    @PutMapping
    public PkuseResult update(@RequestBody UserSaveDTO param) {
        return PkuseResult.ok("保存成功").setData(UserVO.from(userService.updateUser(param)));
    }

    /** 重置密码 */
    @OperLog(module = "用户管理", action = "重置密码")
    @PutMapping("/{id}/password")
    public PkuseResult resetPassword(@PathVariable String id, @RequestParam String password) {
        userService.resetPassword(id, password);
        return PkuseResult.ok("密码已重置");
    }

    /** 启用/禁用用户 */
    @OperLog(module = "用户管理", action = "启用/禁用用户")
    @PutMapping("/{id}/status")
    public PkuseResult changeStatus(@PathVariable String id, @RequestParam Integer status) {
        userService.changeStatus(id, status, authService.currentLoginId());
        return PkuseResult.ok(status == UcUser.STATUS_ENABLED ? "已启用" : "已禁用");
    }

    /** 删除用户 */
    @OperLog(module = "用户管理", action = "删除用户")
    @DeleteMapping("/{id}")
    public PkuseResult delete(@PathVariable String id) {
        userService.deleteUser(id, authService.currentLoginId());
        return PkuseResult.ok("删除成功");
    }

    /** 查询用户已授权的应用标识 */
    @GetMapping("/{id}/clients")
    public PkuseResult listClients(@PathVariable String id) {
        userService.getByIdOrThrow(id);
        return PkuseResult.ok().setData(userClientService.listClientIdsByUserId(id));
    }

    /** 保存用户的应用授权（覆盖式：传入全部已勾选的应用标识） */
    @OperLog(module = "授权管理", action = "授权应用")
    @PutMapping("/{id}/clients")
    public PkuseResult bindClients(@PathVariable String id, @RequestBody List<String> clientIds) {
        userService.getByIdOrThrow(id);
        userClientService.bindClients(id, clientIds);
        return PkuseResult.ok("授权已保存");
    }

    /** 批量授权：为多个用户授予多个应用（mode=append 追加 / cover 覆盖） */
    @OperLog(module = "授权管理", action = "批量授权")
    @PutMapping("/clients/batch")
    public PkuseResult batchBindClients(@RequestBody ClientBatchBindDTO param) {
        boolean append = ClientBatchBindDTO.MODE_APPEND.equalsIgnoreCase(param.getMode());
        int userCount = userClientService.batchBindClients(param.getUserIds(), param.getClientIds(), append);
        int clientCount = param.getClientIds() == null ? 0 : param.getClientIds().size();
        return PkuseResult.ok("批量授权完成")
                .set("userCount", userCount)
                .set("clientCount", clientCount)
                .set("mode", append ? ClientBatchBindDTO.MODE_APPEND : ClientBatchBindDTO.MODE_COVER);
    }

    /** 管理员在线解锁：清除密码错误锁定状态与失败计数 */
    @OperLog(module = "登录安全", action = "在线解锁")
    @PutMapping("/{id}/unlock")
    public PkuseResult unlock(@PathVariable String id) {
        userService.unlockUser(id);
        return PkuseResult.ok("已解锁");
    }

    /** 查询用户已绑定设备（设备锁 / 设备指纹） */
    @GetMapping("/{id}/devices")
    public PkuseResult listDevices(@PathVariable String id) {
        userService.getByIdOrThrow(id);
        List<DeviceVO> devices = userDeviceService.listByUserId(id).stream().map(DeviceVO::from).toList();
        return PkuseResult.ok().setData(devices);
    }

    /** 解绑设备（管理员在线解绑后，该设备可重新作为新设备登录） */
    @OperLog(module = "设备管理", action = "解绑设备")
    @DeleteMapping("/{id}/devices")
    public PkuseResult unbindDevice(@PathVariable String id, @RequestParam String deviceId) {
        userService.getByIdOrThrow(id);
        userDeviceService.unbind(id, deviceId);
        return PkuseResult.ok("设备已解绑");
    }

}
