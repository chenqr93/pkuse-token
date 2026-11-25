package com.bdsoft.usercenter.controller.open;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bdsoft.pkusetoken.sign.annotation.PkuseCheckSign;
import com.bdsoft.pkusetoken.util.PkuseResult;
import com.bdsoft.usercenter.po.client.UcClient;
import com.bdsoft.usercenter.service.client.UcClientService;
import com.bdsoft.usercenter.vo.open.RegistryClientVO;
import com.bdsoft.usercenter.vo.open.RegistryUserVO;
import com.bdsoft.usercenter.service.authz.UcUserClientService;
import com.bdsoft.usercenter.common.pojo.PageData;
import com.bdsoft.usercenter.service.org.UcOrgUnitService;
import com.bdsoft.usercenter.vo.org.OrgUnitTreeVO;
import com.bdsoft.usercenter.support.security.IdCardCipher;
import com.bdsoft.usercenter.dto.region.RegionQueryDTO;
import com.bdsoft.usercenter.po.region.UcRegion;
import com.bdsoft.usercenter.service.region.UcRegionService;
import com.bdsoft.usercenter.vo.region.RegionVO;
import com.bdsoft.usercenter.dto.user.UserQueryDTO;
import com.bdsoft.usercenter.po.user.UcUser;
import com.bdsoft.usercenter.service.user.UcUserService;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 服务间开放接口：应用注册表 / 用户 / 组织 / 区划查询（供授权中心及各业务应用调用）
 *
 * <p> 鉴权方式：MD5 参数签名（client + timestamp + nonce + sign），由 pkuse-token-sign 插件 </p>
 * <p> {@code @PkuseCheckSign} 统一校验（appid 从请求参数 client 动态获取，密钥按应用 </p>
 * <p> 从 uc_client 实时解析，停用应用同样拒绝——见 PkuseSignConfigInitializer），调用方凭证复用 </p>
 * <p>「应用管理」（uc_client）中的登记且需为启用状态——与 SSO 协议、client5 开放接口同一套签名规范。 </p>
 *
 * <p> 注意：本接口不走登录态（/open/** 不在 SaTokenConfigure 拦截范围内），签名即门禁； </p>
 * <p> 返回内容包含应用密钥，仅限系统级调用方使用，生产环境应通过网络策略进一步收敛来源。 </p>
 *
 * <pre>
 *   GET /open/registry/clients?client=auth-center&timestamp=..&nonce=..&sign=..
 *   GET /open/registry/users?username=&nickname=&status=&current=&size=&client=..&timestamp=..&nonce=..&sign=..
 *   GET /open/registry/users/{id}/clients?client=..&timestamp=..&nonce=..&sign=..
 *   GET /open/registry/orgs?client=..&timestamp=..&nonce=..&sign=..
 *   GET /open/registry/divisions?current=&size=&client=..&timestamp=..&nonce=..&sign=..
 *   GET /open/registry/users/by-username?username=&client=..&timestamp=..&nonce=..&sign=..
 *   GET /open/registry/users/by-ids?ids=a,b,c&client=..&timestamp=..&nonce=..&sign=..
 *   GET /open/registry/users/by-id-cards?ids=身份证号1,身份证号2&client=..&timestamp=..&nonce=..&sign=..
 * </pre>
 */
@RestController
@RequestMapping("/open/registry")
@PkuseCheckSign(appid = "#{client}")
public class OpenRegistryController {

    /** 区划字典全量同步时的默认分页大小 */
    private static final long DEFAULT_DIVISION_PAGE_SIZE = 1000L;

    @Resource
    private UcClientService clientService;
    @Resource
    private UcUserService userService;
    @Resource
    private UcUserClientService userClientService;
    @Resource
    private UcOrgUnitService orgUnitService;
    @Resource
    private UcRegionService regionService;

    /** 身份证号存储加解密（出口处解密回填明文，RegistryUserVO.from 不带密文出域） */
    @Resource
    private IdCardCipher idCardCipher;


    /** 应用注册表：全部未删除应用（含密钥与启用状态），授权中心据此维护本地缓存 */
    @GetMapping("/clients")
    public PkuseResult clients(String client, String timestamp, String nonce, String sign) {
        List<RegistryClientVO> list = clientService.list().stream()
                .map(RegistryClientVO::from)
                .collect(Collectors.toList());
        return PkuseResult.ok().setData(list);
    }

    /** 用户分页（口径同 /api/user/page：不含内置管理员；供授权中心控制台实时查询） */
    @GetMapping("/users")
    public PkuseResult users(UserQueryDTO query, String client, String timestamp, String nonce, String sign) {
        Page<UcUser> page = userService.pageQuery(query);
        List<RegistryUserVO> records = page.getRecords().stream()
                .map(this::toRegistryVO)
                .collect(Collectors.toList());
        fillOrgNames(records);
        return PkuseResult.ok().setData(PageData.of(records, page.getTotal(), page.getCurrent(), page.getSize()));
    }

    /** 组织架构树（全量、管理视角）：供业务应用同步本地组织副本 */
    @GetMapping("/orgs")
    public PkuseResult orgs(String client, String timestamp, String nonce, String sign) {
        List<OrgUnitTreeVO> tree = orgUnitService.tree(null, null, null);
        Map<String, Object> data = new java.util.HashMap<>();
        data.put("list", tree);
        data.put("count", countTree(tree));
        return PkuseResult.ok().setData(data);
    }

    /** 区划字典分页（扁平路径行：省/市/县/街道各级编码名称 + 最终编码/层级），供业务应用同步本地区划副本 */
    @GetMapping("/divisions")
    public PkuseResult divisions(@RequestParam(defaultValue = "1") long current,
                                 @RequestParam(defaultValue = "1000") long size,
                                 String client, String timestamp, String nonce, String sign) {
        RegionQueryDTO query = new RegionQueryDTO();
        query.setCurrent(current);
        query.setSize(Math.min(Math.max(size, 1L), 5000L));
        Page<UcRegion> page = regionService.pageQuery(query);
        List<RegionVO> records = page.getRecords().stream()
                .map(region -> {
                    RegionVO vo = new RegionVO();
                    BeanUtils.copyProperties(region, vo);
                    return vo;
                })
                .collect(Collectors.toList());
        return PkuseResult.ok().setData(PageData.of(records, page.getTotal(), page.getCurrent(), page.getSize()));
    }

    /** 按登录账号精确查询（唯一），不存在返回 data=null */
    @GetMapping("/users/by-username")
    public PkuseResult userByUsername(String username, String client, String timestamp, String nonce, String sign) {
        UcUser user = userService.getByUsername(username);
        if (user == null) {
            return PkuseResult.ok().setData(null);
        }
        RegistryUserVO vo = toRegistryVO(user);
        fillOrgNames(Collections.singletonList(vo));
        return PkuseResult.ok().setData(vo);
    }

    /** 按 ID 批量查询（ids 逗号分隔），供业务应用同步/回显用户副本 */
    @GetMapping("/users/by-ids")
    public PkuseResult usersByIds(String ids, String client, String timestamp, String nonce, String sign) {
        if (ids == null || ids.isBlank()) {
            return PkuseResult.ok().setData(Collections.emptyList());
        }
        Set<String> idSet = java.util.Arrays.stream(ids.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
        Collection<UcUser> users = userService.listByIds(idSet);
        List<RegistryUserVO> records = users.stream()
                .map(this::toRegistryVO)
                .collect(Collectors.toList());
        fillOrgNames(records);
        return PkuseResult.ok().setData(records);
    }

    /** 按身份证号批量查询（ids 逗号分隔），供业务应用人员导入按身份证匹配用户 */
    @GetMapping("/users/by-id-cards")
    public PkuseResult usersByIdCards(String ids, String client, String timestamp, String nonce, String sign) {
        if (ids == null || ids.isBlank()) {
            return PkuseResult.ok().setData(Collections.emptyList());
        }
        Set<String> idCardSet = java.util.Arrays.stream(ids.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
        List<RegistryUserVO> records = userService.listByIdCards(idCardSet).stream()
                .map(this::toRegistryVO)
                .collect(Collectors.toList());
        fillOrgNames(records);
        return PkuseResult.ok().setData(records);
    }

    /** 用户已授权（应用入口授权）的应用标识集合：供授权中心分配角色时做边界提示 */
    @GetMapping("/users/{id}/clients")
    public PkuseResult userClients(@PathVariable String id,
                                   String client, String timestamp, String nonce, String sign) {
        UcUser user = userService.getById(id);
        if (user == null) {
            return PkuseResult.error("用户不存在：" + id);
        }
        return PkuseResult.ok().setData(userClientService.listClientIdsByUserId(id));
    }

    /** 实体转 VO 并解密回填身份证号明文（库中为 SM4 密文，业务应用按明文匹配） */
    private RegistryUserVO toRegistryVO(UcUser user) {
        RegistryUserVO vo = RegistryUserVO.from(user);
        if (vo != null) {
            vo.setIdCard(idCardCipher.decrypt(user.getIdCard()));
        }
        return vo;
    }

    /** 批量回填组织名称（id → orgName，避免逐行查询） */
    private void fillOrgNames(List<RegistryUserVO> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        Set<String> orgIds = records.stream()
                .map(RegistryUserVO::getOrgId)
                .filter(orgId -> orgId != null && !orgId.isEmpty())
                .collect(Collectors.toSet());
        if (orgIds.isEmpty()) {
            return;
        }
        Map<String, String> nameMap = orgUnitService.mapNamesByIds(orgIds);
        for (RegistryUserVO vo : records) {
            if (vo.getOrgId() != null) {
                vo.setOrgName(nameMap.get(vo.getOrgId()));
            }
        }
    }

    private static int countTree(List<OrgUnitTreeVO> tree) {
        int count = 0;
        for (OrgUnitTreeVO node : tree) {
            count++;
            count += countTree(node.getChildren());
        }
        return count;
    }

}
