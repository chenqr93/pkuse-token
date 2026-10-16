package com.bdsoft.usercenter.config.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bdsoft.usercenter.config.po.UcConfig;
import org.apache.ibatis.annotations.Mapper;

/**
 * 规则配置 Mapper
 */
@Mapper
public interface UcConfigMapper extends BaseMapper<UcConfig> {
}
