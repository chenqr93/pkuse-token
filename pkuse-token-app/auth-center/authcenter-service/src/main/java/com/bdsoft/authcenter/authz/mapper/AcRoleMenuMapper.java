package com.bdsoft.authcenter.authz.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bdsoft.authcenter.authz.po.AcRoleMenu;
import org.apache.ibatis.annotations.Mapper;

/**
 * 角色-菜单绑定 Mapper（通用 CRUD 复用 MyBatis-Plus BaseMapper，无自定义 SQL）
 */
@Mapper
public interface AcRoleMenuMapper extends BaseMapper<AcRoleMenu> {
}
