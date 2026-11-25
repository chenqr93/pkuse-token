package com.bdsoft.usercenter.mapper.region;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bdsoft.usercenter.po.region.UcRegion;
import org.apache.ibatis.annotations.Mapper;

/**
 * 区划 Mapper（通用 CRUD 复用 MyBatis-Plus BaseMapper，无自定义 SQL）
 */
@Mapper
public interface UcRegionMapper extends BaseMapper<UcRegion> {
}
