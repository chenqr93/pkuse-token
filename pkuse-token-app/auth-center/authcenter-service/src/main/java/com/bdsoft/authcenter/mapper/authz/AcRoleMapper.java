package com.bdsoft.authcenter.mapper.authz;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bdsoft.authcenter.po.authz.AcRole;
import org.apache.ibatis.annotations.Mapper;

/**
 * 角色 Mapper（通用 CRUD 复用 MyBatis-Plus BaseMapper，无自定义 SQL）
 */
@Mapper
public interface AcRoleMapper extends BaseMapper<AcRole> {
}
