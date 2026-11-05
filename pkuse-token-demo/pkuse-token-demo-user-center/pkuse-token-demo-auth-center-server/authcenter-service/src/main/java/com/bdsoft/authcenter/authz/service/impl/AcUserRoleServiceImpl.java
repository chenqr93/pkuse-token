package com.bdsoft.authcenter.authz.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bdsoft.authcenter.authz.po.AcRole;
import com.bdsoft.authcenter.authz.po.AcUserRole;
import com.bdsoft.authcenter.authz.mapper.AcRoleMapper;
import com.bdsoft.authcenter.authz.mapper.AcUserRoleMapper;
import com.bdsoft.authcenter.authz.service.AcUserRoleService;
import com.bdsoft.authcenter.common.exception.BizException;
import com.bdsoft.authcenter.authz.support.AuthzChangePusher;
import com.bdsoft.pkusetoken.stp.StpUtil;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 用户-角色绑定 Service 实现（user_id 为用户中心用户 ID 外部引用）
 */
@Service
public class AcUserRoleServiceImpl extends ServiceImpl<AcUserRoleMapper, AcUserRole> implements AcUserRoleService {

    /** 角色 Mapper（直接用 Mapper 而非 AcRoleService，避免 Service 之间循环依赖） */
    @Resource
    private AcRoleMapper roleMapper;

    @Resource
    private AuthzChangePusher changePusher;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void bindRoles(String userId, List<String> roleIds) {
        if (userId == null || userId.isBlank()) {
            throw new BizException("用户 ID 不能为空");
        }
        Set<String> target = roleIds == null ? Set.of() : new LinkedHashSet<>(roleIds);
        // 校验角色存在且启用
        if (!target.isEmpty()) {
            List<AcRole> roles = roleMapper.selectBatchIds(target);
            if (roles.size() != target.size()) {
                throw new BizException("存在无效角色，请刷新后重试");
            }
            for (AcRole role : roles) {
                if (!Integer.valueOf(AcRole.STATUS_ENABLED).equals(role.getStatus())) {
                    throw new BizException("角色已禁用，不可分配：" + role.getRoleName());
                }
            }
        }
        // 差异增删（覆盖式：全量比对当前绑定）
        List<AcUserRole> current = this.list(new LambdaQueryWrapper<AcUserRole>()
                .eq(AcUserRole::getUserId, userId));
        Set<String> currentRoleIds = current.stream().map(AcUserRole::getRoleId).collect(Collectors.toSet());

        List<AcUserRole> toAdd = target.stream()
                .filter(roleId -> !currentRoleIds.contains(roleId))
                .map(roleId -> {
                    AcUserRole binding = new AcUserRole();
                    binding.setUserId(userId);
                    binding.setRoleId(roleId);
                    return binding;
                })
                .collect(Collectors.toList());
        List<String> toRemoveIds = current.stream()
                .filter(binding -> !target.contains(binding.getRoleId()))
                .map(AcUserRole::getId)
                .collect(Collectors.toList());

        if (!toAdd.isEmpty()) {
            this.saveBatch(toAdd);
        }
        if (!toRemoveIds.isEmpty()) {
            this.update(new LambdaUpdateWrapper<AcUserRole>()
                    .in(AcUserRole::getId, toRemoveIds)
                    .set(AcUserRole::getDeleteBy, currentUserId())
                    .set(AcUserRole::getDeleteTime, LocalDateTime.now()));
            this.removeByIds(toRemoveIds);
        }
        // 用户角色变更影响面可能跨应用（全量覆盖），通知全部开启推送的应用
        changePusher.push(null, "用户授权-保存用户角色：user=" + userId);
    }

    @Override
    public List<AcUserRole> listByUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            return new ArrayList<>();
        }
        return this.list(new LambdaQueryWrapper<AcUserRole>().eq(AcUserRole::getUserId, userId));
    }

    @Override
    public List<AcUserRole> listByUserIds(List<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return new ArrayList<>();
        }
        return this.list(new LambdaQueryWrapper<AcUserRole>().in(AcUserRole::getUserId, userIds));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeByRoleId(String roleId) {
        List<AcUserRole> rows = this.list(new LambdaQueryWrapper<AcUserRole>()
                .eq(AcUserRole::getRoleId, roleId));
        if (rows.isEmpty()) {
            return;
        }
        List<String> ids = rows.stream().map(AcUserRole::getId).collect(Collectors.toList());
        this.update(new LambdaUpdateWrapper<AcUserRole>()
                .in(AcUserRole::getId, ids)
                .set(AcUserRole::getDeleteBy, currentUserId())
                .set(AcUserRole::getDeleteTime, LocalDateTime.now()));
        this.removeByIds(ids);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeByUserIds(List<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return;
        }
        List<AcUserRole> rows = this.list(new LambdaQueryWrapper<AcUserRole>()
                .in(AcUserRole::getUserId, userIds));
        if (rows.isEmpty()) {
            return;
        }
        List<String> ids = rows.stream().map(AcUserRole::getId).collect(Collectors.toList());
        this.update(new LambdaUpdateWrapper<AcUserRole>()
                .in(AcUserRole::getId, ids)
                .set(AcUserRole::getDeleteBy, currentUserId())
                .set(AcUserRole::getDeleteTime, LocalDateTime.now()));
        this.removeByIds(ids);
    }

    @Override
    public long countDistinctUsers() {
        List<AcUserRole> all = this.list();
        return all.stream().map(AcUserRole::getUserId).distinct().count();
    }

    @Override
    public long countDistinctUsersByClientId(String clientId) {
        if (clientId == null || clientId.isBlank()) {
            return 0;
        }
        List<String> roleIds = roleMapper.selectList(new LambdaQueryWrapper<AcRole>()
                        .eq(AcRole::getClientId, clientId))
                .stream().map(AcRole::getId).collect(Collectors.toList());
        if (roleIds.isEmpty()) {
            return 0;
        }
        return this.list(new LambdaQueryWrapper<AcUserRole>()
                        .in(AcUserRole::getRoleId, roleIds))
                .stream().map(AcUserRole::getUserId).distinct().count();
    }

    @Override
    public Map<String, Integer> countUsersByRoleIds(List<String> roleIds) {
        Map<String, Integer> result = new HashMap<>();
        for (String roleId : roleIds) {
            result.put(roleId, 0);
        }
        if (roleIds == null || roleIds.isEmpty()) {
            return result;
        }
        this.list(new LambdaQueryWrapper<AcUserRole>().in(AcUserRole::getRoleId, roleIds))
                .forEach(binding -> result.merge(binding.getRoleId(), 1, Integer::sum));
        return result;
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
