package com.bdsoft.apiplatform.magic.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bdsoft.apiplatform.magic.po.MagicApiPermPO;
import org.apache.ibatis.annotations.Mapper;

/**
 * magic-api 接口权限绑定 Mapper（通用 CRUD 复用 MyBatis-Plus BaseMapper）
 */
@Mapper
public interface MagicApiPermMapper extends BaseMapper<MagicApiPermPO> {
}
