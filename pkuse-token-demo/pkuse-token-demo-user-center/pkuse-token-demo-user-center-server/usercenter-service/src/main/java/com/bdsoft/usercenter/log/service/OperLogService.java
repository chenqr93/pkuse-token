package com.bdsoft.usercenter.log.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.bdsoft.usercenter.log.dto.OperLogQueryDTO;
import com.bdsoft.usercenter.log.po.UcOperLog;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 操作日志 Service
 *
 * <p> 记录平台管理动作（新增用户、修改应用、授权、解锁、解绑设备等），日志按月分表存储； </p>
 * <p> 由 {@code @OperLog} 注解 + {@code OperLogAspect} 自动埋点；更新/新增/删除动作 </p>
 * <p> 额外记录<b>变更前后数据快照</b>（before_data / after_data，敏感字段脱敏）。 </p>
 */
public interface OperLogService extends IService<UcOperLog> {

    /** 记录一条操作日志（含请求信息、变更前后数据与耗时） */
    void record(String module, String action, String result, String msg,
                Object[] args, Object beforeData, Object afterData,
                HttpServletRequest request, long costTime);

    /**
     * 分页查询操作日志（跨月分区查询）
     *
     * @param query 查询参数（操作人、模块、动作、结果、时间范围）
     */
    Page<UcOperLog> pageQuery(OperLogQueryDTO query);

}
