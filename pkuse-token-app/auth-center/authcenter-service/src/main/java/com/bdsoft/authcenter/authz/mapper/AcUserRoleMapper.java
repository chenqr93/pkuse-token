package com.bdsoft.authcenter.authz.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bdsoft.authcenter.authz.po.AcUserRole;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户-角色绑定 Mapper（通用 CRUD 复用 MyBatis-Plus BaseMapper，无自定义 SQL）
 */
@Mapper
public interface AcUserRoleMapper extends BaseMapper<AcUserRole> {
}
