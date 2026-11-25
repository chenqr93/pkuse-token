package com.bdsoft.authcenter.service.log;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.bdsoft.authcenter.dto.log.OperLogQueryDTO;
import com.bdsoft.authcenter.po.log.AcOperLog;

/**
 * 操作日志 Service（追加写入 + 分页查询；由 OperLogAspect 自动记录）
 */
public interface AcOperLogService extends IService<AcOperLog> {

    /** 分页查询（操作人/模块/动作/结果/时间范围筛选，按创建时间倒序） */
    Page<AcOperLog> pageQuery(OperLogQueryDTO query);
}
