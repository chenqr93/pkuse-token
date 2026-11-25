package com.bdsoft.authcenter.support.usercenter;

import com.bdsoft.authcenter.dto.authz.UserQueryDTO;
import com.bdsoft.authcenter.bo.usercenter.RegistryUser;
import com.bdsoft.authcenter.bo.usercenter.UserInfo;

import java.util.List;

/**
 * 用户中心数据访问统一接口（按 SSO 对接模式装配实现）
 *
 * <ul>
 *   <li>simple 模式：{@link SessionForwardUserCenterClient} —— 透传管理员 satoken 调现有接口（零改造）；</li>
 *   <li>ticket 模式：{@link SignedOpenUserCenterClient} —— 签名调 /open/registry/* 开放接口。</li>
 * </ul>
 * <p> 业务代码只依赖本接口，通道切换对控制台完全透明。用户数据不落库。 </p>
 */
public interface UserCenterClient {

    /**
     * 取当前登录用户信息（simple=透传 GET /api/auth/me；ticket=SSO userinfo 消息）
     *
     * <p> 需在登录态上下文中调用（内部取当前会话 token / loginId）。失败抛 BizException。 </p>
     */
    UserInfo fetchMe();

    /** 用户分页查询（口径同用户中心用户管理：不含内置管理员） */
    RegistryUser.Page getUserPage(UserQueryDTO query);

    /** 用户已授权（应用入口授权）的应用标识集合 */
    List<String> getUserClients(String userId);
}
