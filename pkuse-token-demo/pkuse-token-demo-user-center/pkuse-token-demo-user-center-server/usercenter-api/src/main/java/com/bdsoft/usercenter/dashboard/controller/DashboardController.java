package com.bdsoft.usercenter.dashboard.controller;

import com.bdsoft.pkusetoken.sso.PkuseSsoManager;
import com.bdsoft.pkusetoken.util.PkuseResult;
import com.bdsoft.usercenter.client.po.UcClient;
import com.bdsoft.usercenter.user.po.UcUser;
import com.bdsoft.usercenter.auth.service.AuthService;
import com.bdsoft.usercenter.auth.service.OnlineUserService;
import com.bdsoft.usercenter.client.service.SsoClientRegistry;
import com.bdsoft.usercenter.client.service.UcClientService;
import com.bdsoft.usercenter.authz.service.UcUserClientService;
import com.bdsoft.usercenter.user.service.UcUserService;
import com.bdsoft.usercenter.client.vo.ClientVO;
import com.bdsoft.usercenter.dashboard.vo.DashboardOverviewVO;
import com.bdsoft.usercenter.dashboard.vo.SsoServerInfoVO;
import com.bdsoft.usercenter.user.vo.UserVO;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 工作台接口
 *
 * <pre>
 *   /api/dashboard/overview  工作台总览（仅系统管理员）
 *   /api/dashboard/my-apps   我的应用入口（所有登录用户；普通用户为已授权应用）
 * </pre>
 *
 * <p> 出参为 VO：实体不进入接口层。 </p>
 */
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Resource
    private UcUserService userService;

    @Resource
    private UcClientService clientService;

    @Resource
    private UcUserClientService userClientService;

    @Resource
    private SsoClientRegistry clientRegistry;

    @Resource
    private AuthService authService;

    @Resource
    private OnlineUserService onlineUserService;

    /** 工作台总览数据（仅系统管理员） */
    @GetMapping("/overview")
    public PkuseResult overview() {
        DashboardOverviewVO data = new DashboardOverviewVO();
        data.setUserCount(userService.countByStatus(null));
        data.setEnabledUserCount(userService.countByStatus(1));
        data.setClientCount(clientService.countByStatus(null));
        data.setEnabledClientCount(clientService.countByStatus(1));
        // 当前在线人数：Redis 中持有有效登录会话的账号数（已剔除账号不存在的残留会话）
        data.setOnlineUserCount(onlineUserService.onlineUserCount());
        data.setCurrentUser(UserVO.from(authService.currentUser()));
        data.setServerTime(LocalDateTime.now().format(TIME_FORMATTER));
        // 当前已同步到 SSO 认证中心的应用标识
        data.setRegisteredClients(clientRegistry.registeredClientIds());
        // SSO 相关配置（页面展示）
        data.setSsoServer(new SsoServerInfoVO(PkuseSsoManager.getServerConfig().getTicketTimeout(),
                PkuseSsoManager.getServerConfig().getIsCheckSign()));
        return PkuseResult.ok().setData(data);
    }

    /**
     * 我的应用入口
     *
     * <p> 系统管理员：全部启用应用；普通用户：已授权且启用的应用。 </p>
     */
    @GetMapping("/my-apps")
    public PkuseResult myApps() {
        UcUser user = authService.currentUser();
        List<UcClient> list = UcUser.ROLE_ADMIN.equals(user.getRole())
                ? clientService.listAll().stream().filter(c -> Integer.valueOf(UcClient.STATUS_ENABLED).equals(c.getStatus())).toList()
                : userClientService.listAuthorizedEnabledClients(user.getId());
        return PkuseResult.ok().setData(list.stream().map(ClientVO::from).toList());
    }

}
