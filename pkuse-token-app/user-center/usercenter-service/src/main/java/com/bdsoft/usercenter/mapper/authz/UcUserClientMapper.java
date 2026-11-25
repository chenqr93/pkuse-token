package com.bdsoft.usercenter.mapper.authz;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bdsoft.usercenter.po.authz.UcUserClient;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户-应用授权绑定 Mapper
 */
@Mapper
public interface UcUserClientMapper extends BaseMapper<UcUserClient> {

}
