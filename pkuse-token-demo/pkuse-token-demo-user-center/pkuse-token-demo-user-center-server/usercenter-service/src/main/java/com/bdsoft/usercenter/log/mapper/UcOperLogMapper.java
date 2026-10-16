package com.bdsoft.usercenter.log.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bdsoft.usercenter.log.po.UcOperLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 操作日志 Mapper（对应分区表 uc_oper_log）
 */
@Mapper
public interface UcOperLogMapper extends BaseMapper<UcOperLog> {

}
