package com.bdsoft.authcenter.authz.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bdsoft.authcenter.authz.po.AcRoleMenu;
import com.bdsoft.authcenter.authz.mapper.AcRoleMenuMapper;
import com.bdsoft.authcenter.authz.service.AcRoleMenuService;
import com.bdsoft.pkusetoken.stp.StpUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 角色-菜单绑定 Service 实现
 */
@Service
public class AcRoleMenuServiceImpl extends ServiceImpl<AcRoleMenuMapper, AcRoleMenu> implements AcRoleMenuService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeByRoleId(String roleId) {
        markDeleted(new LambdaQueryWrapper<AcRoleMenu>().eq(AcRoleMenu::getRoleId, roleId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeByMenuId(String menuId) {
        markDeleted(new LambdaQueryWrapper<AcRoleMenu>().eq(AcRoleMenu::getMenuId, menuId));
    }

    /** 显式填删除人/删除时间后逻辑删除（规约：逻辑删除前先补审计两字段） */
    private void markDeleted(LambdaQueryWrapper<AcRoleMenu> wrapper) {
        List<AcRoleMenu> rows = this.list(wrapper);
        if (rows.isEmpty()) {
            return;
        }
        String operator = currentUserId();
        LocalDateTime now = LocalDateTime.now();
        this.update(new LambdaUpdateWrapper<AcRoleMenu>()
                .in(AcRoleMenu::getId, rows.stream().map(AcRoleMenu::getId).toList())
                .set(AcRoleMenu::getDeleteBy, operator)
                .set(AcRoleMenu::getDeleteTime, now));
        this.removeByIds(rows.stream().map(AcRoleMenu::getId).toList());
    }

    private static String currentUserId() {
        try {
            Object loginId = StpUtil.getLoginIdDefaultNull();
            return loginId == null ? null : loginId.toString();
        } catch (Exception e) {
            return null;
        }
    }
}
