package com.bdsoft.usercenter.mapper.org;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bdsoft.usercenter.po.org.UcOrgUnit;
import org.apache.ibatis.annotations.Mapper;

/**
 * 组织架构 Mapper（通用 CRUD 复用 MyBatis-Plus BaseMapper，无自定义 SQL）
 */
@Mapper
public interface UcOrgUnitMapper extends BaseMapper<UcOrgUnit> {
}
