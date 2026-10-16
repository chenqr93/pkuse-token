package com.bdsoft.usercenter.authz.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bdsoft.usercenter.authz.po.UcUserClient;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户-应用授权绑定 Mapper
 */
@Mapper
public interface UcUserClientMapper extends BaseMapper<UcUserClient> {

}
