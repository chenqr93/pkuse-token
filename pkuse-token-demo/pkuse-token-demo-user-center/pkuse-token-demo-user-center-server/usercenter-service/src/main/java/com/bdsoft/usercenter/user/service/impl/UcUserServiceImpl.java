package com.bdsoft.usercenter.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bdsoft.pkusetoken.stp.StpUtil;
import com.bdsoft.usercenter.common.exception.BizException;
import com.bdsoft.usercenter.common.util.PasswordUtil;
import com.bdsoft.usercenter.common.config.SystemConfig;
import com.bdsoft.usercenter.user.dto.UserQueryDTO;
import com.bdsoft.usercenter.user.dto.UserSaveDTO;
import com.bdsoft.usercenter.user.po.UcUser;
import com.bdsoft.usercenter.user.mapper.UcUserMapper;
import com.bdsoft.usercenter.auth.service.LoginAttemptService;
import com.bdsoft.usercenter.config.service.UcConfigService;
import com.bdsoft.usercenter.authz.service.UcUserClientService;
import com.bdsoft.usercenter.device.service.UcUserDeviceService;
import com.bdsoft.usercenter.user.service.UcUserService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 用户管理 Service 实现
 *
 * <p> 默认密码、密码复杂度、首次登录强制修改密码等规则取自「规则配置」（uc_config），修改后即时生效。 </p>
 */
@Service
public class UcUserServiceImpl extends ServiceImpl<UcUserMapper, UcUser> implements UcUserService {

    @Resource
    private UcUserClientService userClientService;

    @Resource
    private UcUserDeviceService userDeviceService;

    @Resource
    private LoginAttemptService loginAttemptService;

    @Resource
    private UcConfigService configService;

    @Override
    public Page<UcUser> pageQuery(UserQueryDTO query) {
        LambdaQueryWrapper<UcUser> wrapper = new LambdaQueryWrapper<UcUser>()
                // 内置管理员账号不出现在用户管理中
                .ne(UcUser::getId, BUILT_IN_ADMIN_ID)
                .like(hasText(query.getUsername()), UcUser::getUsername, query.getUsername())
                .like(hasText(query.getNickname()), UcUser::getNickname, query.getNickname())
                .eq(query.getStatus() != null, UcUser::getStatus, query.getStatus())
                .orderByAsc(UcUser::getId);
        return this.page(new Page<>(query.currentOrDefault(), query.sizeOrDefault()), wrapper);
    }

    @Override
    public long countByStatus(Integer status) {
        return this.count(new LambdaQueryWrapper<UcUser>()
                .ne(UcUser::getId, BUILT_IN_ADMIN_ID)
                .eq(status != null, UcUser::getStatus, status));
    }

    @Override
    public UcUser getByIdOrThrow(String id) {
        UcUser user = (id == null ? null : this.getById(id));
        if (user == null) {
            throw new BizException("用户不存在");
        }
        return user;
    }

    @Override
    public UcUser getByUsername(String username) {
        if (!hasText(username)) {
            return null;
        }
        return this.getOne(new LambdaQueryWrapper<UcUser>().eq(UcUser::getUsername, username));
    }

    @Override
    public UcUser createUser(UserSaveDTO param) {
        if (!hasText(param.getUsername())) {
            throw new BizException("登录账号不能为空");
        }
        if (getByUsername(param.getUsername()) != null) {
            throw new BizException("登录账号已存在：" + param.getUsername());
        }
        SystemConfig config = configService.get();
        // 密码复杂度校验（显式设置密码时；使用默认密码时由配置项自身保证）
        if (hasText(param.getPassword())) {
            String passwordError = config.checkPassword(param.getPassword());
            if (passwordError != null) {
                throw new BizException(passwordError);
            }
        }
        // 默认密码（规则配置 - 系统默认密码；国密 SM3 + 随机盐存储）
        String password = hasText(param.getPassword()) ? param.getPassword() : config.getDefaultPassword();
        String salt = PasswordUtil.newSalt();

        UcUser user = new UcUser();
        user.setUsername(param.getUsername());
        user.setPassword(PasswordUtil.encode(password, salt));
        user.setPasswordSalt(salt);
        user.setNickname(param.getNickname());
        user.setEmail(param.getEmail());
        user.setPhone(param.getPhone());
        user.setRole(hasText(param.getRole()) ? param.getRole() : UcUser.ROLE_USER);
        user.setStatus(param.getStatus() == null ? UcUser.STATUS_ENABLED : param.getStatus());
        user.setRemark(param.getRemark());
        // 首次登录强制修改密码（规则配置）：新增用户需在下次登录时修改密码
        user.setMustChangePassword(config.isForceChangePasswordOnFirst()
                ? UcUser.MUST_CHANGE_PASSWORD_YES : UcUser.MUST_CHANGE_PASSWORD_NO);
        this.save(user);
        return user;
    }

    @Override
    public UcUser updateUser(UserSaveDTO param) {
        UcUser db = getByIdOrThrow(param.getId());
        if (!hasText(param.getUsername())) {
            throw new BizException("登录账号不能为空");
        }
        // 账号唯一性校验（排除自己）
        UcUser sameName = getByUsername(param.getUsername());
        if (sameName != null && !sameName.getId().equals(db.getId())) {
            throw new BizException("登录账号已存在：" + param.getUsername());
        }
        // 内置管理员保护：不允许禁用、不允许改角色
        if (BUILT_IN_ADMIN_ID.equals(db.getId())) {
            if (Integer.valueOf(UcUser.STATUS_DISABLED).equals(param.getStatus())) {
                throw new BizException("内置管理员账号不允许禁用");
            }
            if (hasText(param.getRole()) && !UcUser.ROLE_ADMIN.equals(param.getRole())) {
                throw new BizException("内置管理员账号不允许修改角色");
            }
        }
        db.setUsername(param.getUsername());
        db.setNickname(param.getNickname());
        db.setEmail(param.getEmail());
        db.setPhone(param.getPhone());
        db.setRole(hasText(param.getRole()) ? param.getRole() : db.getRole());
        db.setStatus(param.getStatus() == null ? db.getStatus() : param.getStatus());
        db.setRemark(param.getRemark());
        this.updateById(db);
        return db;
    }

    @Override
    public void resetPassword(String id, String password) {
        UcUser db = getByIdOrThrow(id);
        if (!hasText(password)) {
            throw new BizException("新密码不能为空");
        }
        SystemConfig config = configService.get();
        // 密码复杂度校验（规则配置 - 密码规则）
        String passwordError = config.checkPassword(password);
        if (passwordError != null) {
            throw new BizException(passwordError);
        }
        String salt = PasswordUtil.newSalt();
        db.setPassword(PasswordUtil.encode(password, salt));
        db.setPasswordSalt(salt);
        // 首次登录强制修改密码（规则配置）：被重置密码的用户需在下次登录时修改密码
        db.setMustChangePassword(config.isForceChangePasswordOnFirst()
                ? UcUser.MUST_CHANGE_PASSWORD_YES : UcUser.MUST_CHANGE_PASSWORD_NO);
        this.updateById(db);
    }

    @Override
    public void changeStatus(String id, Integer status, String currentLoginId) {
        UcUser db = getByIdOrThrow(id);
        if (status == null || (status != UcUser.STATUS_DISABLED && status != UcUser.STATUS_ENABLED)) {
            throw new BizException("状态值不合法，0=禁用 / 1=启用");
        }
        if (db.getId().equals(currentLoginId) && status == UcUser.STATUS_DISABLED) {
            throw new BizException("不能禁用当前登录账号");
        }
        if (BUILT_IN_ADMIN_ID.equals(db.getId()) && status == UcUser.STATUS_DISABLED) {
            throw new BizException("内置管理员账号不允许禁用");
        }
        db.setStatus(status);
        this.updateById(db);
    }

    @Override
    public void unlockUser(String id) {
        UcUser db = getByIdOrThrow(id);
        // 注意：clear 字段需用 UpdateWrapper 显式 set(null)（updateById 会忽略 null 字段）
        this.update(new LambdaUpdateWrapper<UcUser>()
                .eq(UcUser::getId, db.getId())
                .set(UcUser::getLockedUntil, null)
                .set(UcUser::getLockReason, null)
                .set(UcUser::getLoginFailCount, 0)
                .set(UcUser::getUpdateTime, LocalDateTime.now()));
        // 同时清空失败计数（Redis）
        loginAttemptService.clearFailCount(db.getUsername());
    }

    @Override
    public void deleteUser(String id, String currentLoginId) {
        UcUser db = getByIdOrThrow(id);
        if (db.getId().equals(currentLoginId)) {
            throw new BizException("不能删除当前登录账号");
        }
        if (BUILT_IN_ADMIN_ID.equals(db.getId())) {
            throw new BizException("内置管理员账号不允许删除");
        }
        // 审计：记录删除人与删除时间（逻辑删除 UPDATE 仅置 deleted，此二字段需显式填充）
        this.update(new LambdaUpdateWrapper<UcUser>()
                .eq(UcUser::getId, id)
                .set(UcUser::getDeleteBy, currentLoginId)
                .set(UcUser::getDeleteTime, LocalDateTime.now()));
        // 先注销该账号的全部在线会话（避免已删除账号的 token 继续可用、在线统计残留）
        StpUtil.logout(id);
        this.removeById(id);
        // 同步清理该用户的应用授权关系与设备记录
        userClientService.removeByUserId(id);
        userDeviceService.removeByUserId(id);
        loginAttemptService.clearFailCount(db.getUsername());
    }

    private static boolean hasText(String text) {
        return text != null && !text.isBlank();
    }

}
