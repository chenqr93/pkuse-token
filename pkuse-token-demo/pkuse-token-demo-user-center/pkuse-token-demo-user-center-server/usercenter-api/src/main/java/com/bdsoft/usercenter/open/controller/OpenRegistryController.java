package com.bdsoft.usercenter.open.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bdsoft.pkusetoken.util.PkuseResult;
import com.bdsoft.usercenter.client.po.UcClient;
import com.bdsoft.usercenter.client.service.UcClientService;
import com.bdsoft.usercenter.open.support.OpenSignVerifier;
import com.bdsoft.usercenter.open.vo.RegistryClientVO;
import com.bdsoft.usercenter.open.vo.RegistryUserVO;
import com.bdsoft.usercenter.authz.service.UcUserClientService;
import com.bdsoft.usercenter.common.pojo.PageData;
import com.bdsoft.usercenter.user.dto.UserQueryDTO;
import com.bdsoft.usercenter.user.po.UcUser;
import com.bdsoft.usercenter.user.service.UcUserService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 服务间开放接口：应用注册表 / 用户查询（供授权中心调用）
 *
 * <p> 鉴权方式：MD5 参数签名（client + timestamp + nonce + sign），调用方凭证复用 </p>
 * <p>「应用管理」（uc_client）中的登记且需为启用状态——与 SSO 协议、client5 开放接口同一套签名规范。 </p>
 *
 * <p> 注意：本接口不走登录态（/open/** 不在 SaTokenConfigure 拦截范围内），签名即门禁； </p>
 * <p> 返回内容包含应用密钥，仅限系统级调用方（授权中心）使用，生产环境应通过网络策略进一步收敛来源。 </p>
 *
 * <pre>
 *   GET /open/registry/clients?client=auth-center&timestamp=..&nonce=..&sign=..
 *   GET /open/registry/users?username=&nickname=&status=&current=&size=&client=..&timestamp=..&nonce=..&sign=..
 *   GET /open/registry/users/{id}/clients?client=..&timestamp=..&nonce=..&sign=..
 * </pre>
 */
@RestController
@RequestMapping("/open/registry")
public class OpenRegistryController {

    @Resource
    private OpenSignVerifier signVerifier;
    @Resource
    private UcClientService clientService;
    @Resource
    private UcUserService userService;
    @Resource
    private UcUserClientService userClientService;


    /** 应用注册表：全部未删除应用（含密钥与启用状态），授权中心据此维护本地缓存 */
    @GetMapping("/clients")
    public PkuseResult clients(String client, String timestamp, String nonce, String sign) {
        if (!checkSign(client, timestamp, nonce, sign)) {
            return PkuseResult.error("签名校验失败（检查 client/timestamp/nonce/sign 与密钥）");
        }
        List<RegistryClientVO> list = clientService.list().stream()
                .map(RegistryClientVO::from)
                .collect(Collectors.toList());
        return PkuseResult.ok().setData(list);
    }

    /** 用户分页（口径同 /api/user/page：不含内置管理员；供授权中心控制台实时查询） */
    @GetMapping("/users")
    public PkuseResult users(UserQueryDTO query, String client, String timestamp, String nonce, String sign) {
        if (!checkSign(client, timestamp, nonce, sign)) {
            return PkuseResult.error("签名校验失败（检查 client/timestamp/nonce/sign 与密钥）");
        }
        Page<UcUser> page = userService.pageQuery(query);
        List<RegistryUserVO> records = page.getRecords().stream()
                .map(RegistryUserVO::from)
                .collect(Collectors.toList());
        return PkuseResult.ok().setData(PageData.of(records, page.getTotal(), page.getCurrent(), page.getSize()));
    }

    /** 用户已授权（应用入口授权）的应用标识集合：供授权中心分配角色时做边界提示 */
    @GetMapping("/users/{id}/clients")
    public PkuseResult userClients(@PathVariable String id,
                                   String client, String timestamp, String nonce, String sign) {
        if (!checkSign(client, timestamp, nonce, sign)) {
            return PkuseResult.error("签名校验失败（检查 client/timestamp/nonce/sign 与密钥）");
        }
        UcUser user = userService.getById(id);
        if (user == null) {
            return PkuseResult.error("用户不存在：" + id);
        }
        return PkuseResult.ok().setData(userClientService.listClientIdsByUserId(id));
    }

    /** 签名校验：调用方需在「应用管理」中登记且启用 */
    private boolean checkSign(String client, String timestamp, String nonce, String sign) {
        UcClient caller = clientService.getByClientId(client);
        if (caller == null || !Integer.valueOf(UcClient.STATUS_ENABLED).equals(caller.getStatus())) {
            return false;
        }
        return signVerifier.verify(client, timestamp, nonce, sign, caller.getSecretKey());
    }
}
