package com.bdsoft.usercenter.client.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bdsoft.usercenter.client.po.UcClient;
import org.apache.ibatis.annotations.Mapper;

/**
 * 应用（SSO Client）Mapper
 */
@Mapper
public interface UcClientMapper extends BaseMapper<UcClient> {

}
