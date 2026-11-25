package com.bdsoft.authcenter.mapper.authz;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bdsoft.authcenter.po.authz.AcMenu;
import org.apache.ibatis.annotations.Mapper;

/**
 * 菜单 Mapper（通用 CRUD 复用 MyBatis-Plus BaseMapper，无自定义 SQL）
 */
@Mapper
public interface AcMenuMapper extends BaseMapper<AcMenu> {
}
