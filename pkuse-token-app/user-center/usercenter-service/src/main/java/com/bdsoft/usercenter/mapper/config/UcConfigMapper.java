package com.bdsoft.usercenter.mapper.config;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bdsoft.usercenter.po.config.UcConfig;
import org.apache.ibatis.annotations.Mapper;

/**
 * 规则配置 Mapper
 */
@Mapper
public interface UcConfigMapper extends BaseMapper<UcConfig> {
}
