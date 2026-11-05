package com.bdsoft.usercenter.region.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bdsoft.usercenter.region.po.UcRegion;
import org.apache.ibatis.annotations.Mapper;

/**
 * 区划 Mapper（通用 CRUD 复用 MyBatis-Plus BaseMapper，无自定义 SQL）
 */
@Mapper
public interface UcRegionMapper extends BaseMapper<UcRegion> {
}
