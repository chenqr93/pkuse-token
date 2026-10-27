package com.bdsoft.client.magic.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bdsoft.client.magic.po.MagicOpenClientPO;
import org.apache.ibatis.annotations.Mapper;

/**
 * magic-api 开放接口调用方 Mapper（表 s_open_client，通用 CRUD 由 BaseMapper 提供）
 */
@Mapper
public interface MagicOpenClientMapper extends BaseMapper<MagicOpenClientPO> {
}
