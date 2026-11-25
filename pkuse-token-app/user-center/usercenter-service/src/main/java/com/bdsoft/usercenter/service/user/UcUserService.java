package com.bdsoft.usercenter.service.user;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.bdsoft.usercenter.dto.user.UserQueryDTO;
import com.bdsoft.usercenter.dto.user.UserSaveDTO;
import com.bdsoft.usercenter.po.user.UcUser;

import java.util.Collection;
import java.util.List;

/**
 * 用户管理 Service
 *
 * <p> 默认密码、密码复杂度、首次登录强制修改密码等规则取自「规则配置」（uc_config），修改后即时生效。 </p>
 *
 * <p> 入参为 DTO、出参为实体，实体仅在 Service 层内部流转，不直接返回前端（对外由 UserVO 承载）。 </p>
 */
public interface UcUserService extends IService<UcUser> {

    /** 内置管理员账号 ID（系统账号，不出现在用户管理中，且不允许删除/禁用） */
    String BUILT_IN_ADMIN_ID = "10001";

    /**
     * 分页查询
     *
     * <p> 内置管理员账号（admin）作为系统账号，不出现在用户管理中。 </p>
     */
    Page<UcUser> pageQuery(UserQueryDTO query);

    /**
     * 统计用户数（status 为 null 时统计全部）
     *
     * <p> 与用户管理列表口径一致：不含内置管理员账号。 </p>
     */
    long countByStatus(Integer status);

    /** 根据 ID 查询（不存在时抛业务异常） */
    UcUser getByIdOrThrow(String id);

    /** 根据账号查询（不存在返回 null） */
    UcUser getByUsername(String username);

    /**
     * 按身份证号批量查询（供业务应用人员导入匹配）
     *
     * <p> 入参明文身份证号先取 SM3 摘要，再按 uc_user.id_card_hash 等值匹配
     * （库中 id_card 为 SM4 随机 IV 密文，无法直接等值查询）。 </p>
     */
    List<UcUser> listByIdCards(Collection<String> idCards);

    /** 新增用户（密码为空时使用「规则配置 - 系统默认密码」；密码需满足「密码复杂度」规则） */
    UcUser createUser(UserSaveDTO param);

    /** 修改用户（不支持通过此方法修改密码） */
    UcUser updateUser(UserSaveDTO param);

    /** 重置密码（需满足「密码复杂度」规则；开启「首次登录强制修改密码」时用户下次登录需改密） */
    void resetPassword(String id, String password);

    /** 启用/禁用用户 */
    void changeStatus(String id, Integer status, String currentLoginId);

    /** 管理员在线解锁（清除密码错误锁定状态与失败计数） */
    void unlockUser(String id);

    /** 删除用户 */
    void deleteUser(String id, String currentLoginId);

}
