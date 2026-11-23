package com.bdsoft.usercenter.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bdsoft.usercenter.user.po.UcUser;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户 Mapper
 */
@Mapper
public interface UcUserMapper extends BaseMapper<UcUser> {

}
