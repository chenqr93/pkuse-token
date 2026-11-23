package com.bdsoft.usercenter.security;

import com.bdsoft.pkusetoken.stp.StpInterface;
import com.bdsoft.usercenter.user.po.UcUser;
import com.bdsoft.usercenter.user.service.UcUserService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * Pkuse-Token 角色数据源
 *
 * <p> 角色取自 uc_user.role：admin=系统管理员（可维护用户与应用），user=普通用户。 </p>
 * <p> 供 {@code StpUtil.checkRole(UcUser.ROLE_ADMIN)} / {@code @PkuseCheckRole(UcUser.ROLE_ADMIN)} 使用。 </p>
 */
@Component
public class StpInterfaceImpl implements StpInterface {

    @Resource
    private UcUserService userService;

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        // 本示例未使用权限码，统一返回空集合
        return Collections.emptyList();
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        String userId = toStr(loginId);
        if (userId == null) {
            return Collections.emptyList();
        }
        UcUser user = userService.getById(userId);
        if (user == null || user.getRole() == null || user.getRole().isBlank()) {
            return Collections.emptyList();
        }
        return Collections.singletonList(user.getRole());
    }

    /** 登录 ID 统一转字符串（String 主键；兼容历史会话中的数字形式） */
    private static String toStr(Object loginId) {
        if (loginId == null) {
            return null;
        }
        String text = loginId.toString();
        return text.isBlank() ? null : text;
    }

}
