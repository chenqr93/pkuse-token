package com.bdsoft.usercenter.config.controller;

import com.bdsoft.pkusetoken.util.PkuseResult;
import com.bdsoft.usercenter.common.annotation.OperLog;
import com.bdsoft.usercenter.common.config.SystemConfig;
import com.bdsoft.usercenter.common.config.SystemConfigItems;
import com.bdsoft.usercenter.config.dto.ConfigUpdateDTO;
import com.bdsoft.usercenter.config.po.UcConfig;
import com.bdsoft.usercenter.config.service.UcConfigService;
import com.bdsoft.usercenter.config.vo.ConfigItemVO;
import com.bdsoft.usercenter.auth.vo.PasswordRuleVO;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 规则配置接口（仅系统管理员可访问）
 *
 * <pre>
 *   GET  /api/config/list    配置项列表（含名称、分组、类型、当前值、默认值、说明、可选项）
 *   PUT  /api/config         批量保存（仅提交需要修改的项）
 *   POST /api/config/reset   恢复全部默认值
 * </pre>
 *
 * <p> 入参为 DTO、出参为 VO：实体不进入接口层。 </p>
 */
@RestController
@RequestMapping("/api/config")
public class UcConfigController {

    @Resource
    private UcConfigService configService;

    /** 配置项列表（按分组返回，供配置页面渲染） */
    @GetMapping("/list")
    public PkuseResult list() {
        Map<String, List<ConfigItemVO>> groups = new LinkedHashMap<>();
        for (String group : List.of(SystemConfigItems.GROUP_PASSWORD, SystemConfigItems.GROUP_LOGIN,
                SystemConfigItems.GROUP_LOG)) {
            groups.put(group, new ArrayList<>());
        }
        for (UcConfig row : configService.listAll()) {
            groups.computeIfAbsent(row.getConfigGroup() == null ? "其他" : row.getConfigGroup(),
                    key -> new ArrayList<>()).add(ConfigItemVO.from(row));
        }
        return PkuseResult.ok().setData(groups);
    }

    /** 批量保存配置 */
    @OperLog(module = "规则配置", action = "修改规则配置")
    @PutMapping
    public PkuseResult save(@RequestBody ConfigUpdateDTO param) {
        configService.updateValues(param.getValues());
        return PkuseResult.ok("配置已保存并即时生效");
    }

    /** 恢复默认配置 */
    @OperLog(module = "规则配置", action = "恢复默认配置")
    @PostMapping("/reset")
    public PkuseResult reset() {
        configService.resetToDefaults();
        return PkuseResult.ok("已恢复默认配置");
    }

    /** 当前生效的密码规则说明（管理界面辅助展示：含系统默认密码） */
    @GetMapping("/password-rule")
    public PkuseResult passwordRule() {
        SystemConfig config = configService.get();
        PasswordRuleVO data = PasswordRuleVO.from(config);
        data.setDefaultPassword(config.getDefaultPassword());
        return PkuseResult.ok().setData(data);
    }

}
