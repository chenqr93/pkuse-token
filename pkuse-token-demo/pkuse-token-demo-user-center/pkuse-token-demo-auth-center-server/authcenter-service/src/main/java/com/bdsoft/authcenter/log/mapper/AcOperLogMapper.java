package com.bdsoft.authcenter.log.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bdsoft.authcenter.log.po.AcOperLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 操作日志 Mapper（通用 CRUD 复用 MyBatis-Plus BaseMapper，无自定义 SQL）
 */
@Mapper
public interface AcOperLogMapper extends BaseMapper<AcOperLog> {
}
