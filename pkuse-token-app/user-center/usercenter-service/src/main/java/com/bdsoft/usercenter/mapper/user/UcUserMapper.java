package com.bdsoft.usercenter.mapper.user;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bdsoft.usercenter.po.user.UcUser;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户 Mapper
 */
@Mapper
public interface UcUserMapper extends BaseMapper<UcUser> {

}
