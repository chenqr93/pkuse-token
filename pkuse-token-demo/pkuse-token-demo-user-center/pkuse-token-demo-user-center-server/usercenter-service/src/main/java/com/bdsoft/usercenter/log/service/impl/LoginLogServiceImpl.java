package com.bdsoft.usercenter.log.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bdsoft.usercenter.log.support.LogPartitionMaintainer;
import com.bdsoft.usercenter.common.util.RequestInfoUtil;
import com.bdsoft.usercenter.log.dto.LoginLogQueryDTO;
import com.bdsoft.usercenter.log.po.UcLoginLog;
import com.bdsoft.usercenter.log.mapper.UcLoginLogMapper;
import com.bdsoft.usercenter.log.service.LoginLogService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 登录日志 Service 实现
 *
 * <p> 按月分表：父表 uc_login_log 按 create_time 做范围分区（KingbaseES 声明式分区）， </p>
 * <p> 月分区表名为 uc_login_log_yyyyMM，插入父表时由数据库自动路由到对应月份分表； </p>
 * <p> 分区维护（启动 + 每日定时 + 写入兜底，频率可配置）见 {@link LogPartitionMaintainer}。 </p>
 */
@Service
public class LoginLogServiceImpl extends ServiceImpl<UcLoginLogMapper, UcLoginLog> implements LoginLogService {

    private static final Logger log = LoggerFactory.getLogger(LoginLogServiceImpl.class);

    private final LogPartitionMaintainer partitionMaintainer;

    public LoginLogServiceImpl(LogPartitionMaintainer partitionMaintainer) {
        this.partitionMaintainer = partitionMaintainer;
    }

    @Override
    public void record(String username, String userId, String loginType, String result, String msg,
                       String deviceId, HttpServletRequest request) {
        // 请求信息必须在当前（同步）线程内提取，避免异步/请求回收后取不到
        String userAgent = RequestInfoUtil.getUserAgent(request);
        UcLoginLog loginLog = new UcLoginLog();
        loginLog.setUsername(username);
        loginLog.setUserId(userId);
        loginLog.setLoginType(loginType);
        loginLog.setResult(result);
        loginLog.setMsg(msg);
        loginLog.setIp(RequestInfoUtil.getClientIp(request));
        loginLog.setUserAgent(userAgent);
        loginLog.setOs(RequestInfoUtil.parseOs(userAgent));
        loginLog.setBrowser(RequestInfoUtil.parseBrowser(userAgent));
        loginLog.setDeviceId(deviceId);
        loginLog.setCreateTime(LocalDateTime.now());

        // 兜底：确保当前周期分表存在（正常已在启动/定时任务中创建）
        partitionMaintainer.ensureCurrentPeriod(LogPartitionMaintainer.TABLE_LOGIN);
        try {
            this.save(loginLog);
        } catch (Exception e) {
            // 日志写入失败不影响主流程
            log.warn("登录日志写入失败：{}", e.getMessage());
        }
    }

    @Override
    public Page<UcLoginLog> pageQuery(LoginLogQueryDTO query) {
        LambdaQueryWrapper<UcLoginLog> wrapper = new LambdaQueryWrapper<UcLoginLog>()
                .like(hasText(query.getUsername()), UcLoginLog::getUsername, query.getUsername())
                .eq(hasText(query.getResult()), UcLoginLog::getResult, query.getResult())
                .ge(query.getBegin() != null, UcLoginLog::getCreateTime, query.getBegin())
                .le(query.getEnd() != null, UcLoginLog::getCreateTime, query.getEnd())
                .orderByDesc(UcLoginLog::getCreateTime);
        return this.page(new Page<>(query.currentOrDefault(), query.sizeOrDefault()), wrapper);
    }

    private static boolean hasText(String text) {
        return text != null && !text.isBlank();
    }

}
