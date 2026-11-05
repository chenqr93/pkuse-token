package com.bdsoft.apiplatform.magic.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.bdsoft.apiplatform.magic.po.MagicApiPermPO;

import java.util.List;

/**
 * magic-api 接口权限绑定 Service（V2：接口级 RBAC，权限码来自授权中心）
 */
public interface MagicApiPermService extends IService<MagicApiPermPO> {

    /** 按请求路径+方法查询启用的绑定（method 不匹配时回退 * 通配；不存在返回 null） */
    MagicApiPermPO findEnabled(String path, String method);

    /** 全部绑定（按路径排序） */
    List<MagicApiPermPO> listAll();

    /** 新增/修改绑定（path+method 唯一） */
    MagicApiPermPO saveBinding(MagicApiPermPO po);

    /** 删除绑定 */
    void deleteBinding(String id);
}
