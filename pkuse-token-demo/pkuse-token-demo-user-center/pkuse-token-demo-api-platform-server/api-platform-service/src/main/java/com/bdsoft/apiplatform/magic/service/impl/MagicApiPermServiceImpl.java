package com.bdsoft.apiplatform.magic.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bdsoft.apiplatform.magic.mapper.MagicApiPermMapper;
import com.bdsoft.apiplatform.magic.po.MagicApiPermPO;
import com.bdsoft.apiplatform.magic.service.MagicApiPermService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * magic-api 接口权限绑定 Service 实现
 */
@Service
public class MagicApiPermServiceImpl extends ServiceImpl<MagicApiPermMapper, MagicApiPermPO>
        implements MagicApiPermService {

    @Override
    public MagicApiPermPO findEnabled(String path, String method) {
        if (path == null || path.isBlank()) {
            return null;
        }
        List<MagicApiPermPO> rows = this.list(new LambdaQueryWrapper<MagicApiPermPO>()
                .eq(MagicApiPermPO::getPath, path)
                .eq(MagicApiPermPO::getStatus, 1));
        if (rows.isEmpty()) {
            return null;
        }
        // 精确匹配请求方法优先，其次 * 通配
        return rows.stream()
                .filter(row -> method != null && method.equalsIgnoreCase(row.getMethod()))
                .findFirst()
                .orElseGet(() -> rows.stream()
                        .filter(row -> "*".equals(row.getMethod()))
                        .findFirst()
                        .orElse(null));
    }

    @Override
    public List<MagicApiPermPO> listAll() {
        return this.list(new LambdaQueryWrapper<MagicApiPermPO>().orderByAsc(MagicApiPermPO::getPath));
    }

    @Override
    public MagicApiPermPO saveBinding(MagicApiPermPO po) {
        if (po.getPath() == null || po.getPath().isBlank()) {
            throw new IllegalArgumentException("接口路径不能为空");
        }
        if (po.getPerms() == null || po.getPerms().isBlank()) {
            throw new IllegalArgumentException("权限码不能为空（如 sys:user:list，多个用逗号分隔）");
        }
        long count = this.count(new LambdaQueryWrapper<MagicApiPermPO>()
                .eq(MagicApiPermPO::getPath, po.getPath())
                .eq(MagicApiPermPO::getMethod, po.getMethod() == null ? "*" : po.getMethod())
                .ne(po.getId() != null && !po.getId().isBlank(), MagicApiPermPO::getId, po.getId()));
        if (count > 0) {
            throw new IllegalArgumentException("该路径+方法已存在绑定：" + po.getPath());
        }
        if (po.getId() == null || po.getId().isBlank()) {
            po.setId(UUID.randomUUID().toString().replace("-", ""));
            po.setCreateTime(LocalDateTime.now());
        } else {
            po.setUpdateTime(LocalDateTime.now());
        }
        this.saveOrUpdate(po);
        return po;
    }

    @Override
    public void deleteBinding(String id) {
        this.removeById(id);
    }
}
