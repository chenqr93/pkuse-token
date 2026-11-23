package com.bdsoft.usercenter.log.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.bdsoft.usercenter.log.dto.LoginLogQueryDTO;
import com.bdsoft.usercenter.log.po.UcLoginLog;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 登录日志 Service
 *
 * <p> 日志表按月分表（KingbaseES 声明式分区）：父表 uc_login_log + 月分区 uc_login_log_yyyyMM， </p>
 * <p> 分区由 {@link com.bdsoft.usercenter.log.support.LogPartitionMaintainer} 在启动时与每日定时任务中自动维护； </p>
 * <p> 写入失败不影响登录流程。 </p>
 */
public interface LoginLogService extends IService<UcLoginLog> {

    /** 记录一条登录日志（包含 IP / UA / 设备指纹 等请求信息） */
    void record(String username, String userId, String loginType, String result, String msg,
                String deviceId, HttpServletRequest request);

    /**
     * 分页查询登录日志（跨月分区查询）
     *
     * @param query 查询参数（账号、结果、时间范围）
     */
    Page<UcLoginLog> pageQuery(LoginLogQueryDTO query);

}
