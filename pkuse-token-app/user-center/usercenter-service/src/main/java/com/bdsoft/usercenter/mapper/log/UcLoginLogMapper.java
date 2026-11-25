package com.bdsoft.usercenter.mapper.log;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bdsoft.usercenter.po.log.UcLoginLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 登录日志 Mapper（对应分区表 uc_login_log，插入/查询由分区路由自动完成）
 */
@Mapper
public interface UcLoginLogMapper extends BaseMapper<UcLoginLog> {

}
