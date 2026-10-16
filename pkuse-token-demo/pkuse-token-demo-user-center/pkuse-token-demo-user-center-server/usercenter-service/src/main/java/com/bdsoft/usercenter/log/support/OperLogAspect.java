package com.bdsoft.usercenter.log.support;

import com.bdsoft.pkusetoken.spring.SpringMVCUtil;
import com.bdsoft.usercenter.common.annotation.OperLog;
import com.bdsoft.usercenter.log.po.UcOperLog;
import com.bdsoft.usercenter.log.service.OperLogService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

/**
 * 操作日志切面
 *
 * <p> 拦截标注了 {@link OperLog} 的接口方法，自动记录：操作人、模块、动作、请求参数摘要、结果、耗时、IP/UA； </p>
 * <p> <b>变更前后数据</b>由 MyBatis-Plus 拦截器体系自动捕获：方法执行期间， </p>
 * <p> {@link OperChangeCaptureInterceptor} 把所有落库的 INSERT/UPDATE/DELETE（不限定实体类型， </p>
 * <p> 一个方法更新多张表 / 多个实体同样覆盖）登记到 {@link OperRecordCollector}， </p>
 * <p> 方法结束时统一整理为 before_data（修改前）/ after_data（修改后），随本条日志写入。 </p>
 * <p> 操作失败时同样记录（保留修改前数据与失败原因），且日志记录异常不影响业务主流程。 </p>
 */
@Aspect
@Component
public class OperLogAspect {

    private static final Logger log = LoggerFactory.getLogger(OperLogAspect.class);

    private final OperLogService operLogService;

    private final DataSource dataSource;

    public OperLogAspect(OperLogService operLogService, DataSource dataSource) {
        this.operLogService = operLogService;
        this.dataSource = dataSource;
    }

    @Around("@annotation(operLog)")
    public Object around(ProceedingJoinPoint joinPoint, OperLog operLog) throws Throwable {
        long start = System.currentTimeMillis();
        OperRecordCollector.begin();
        Throwable error = null;
        try {
            return joinPoint.proceed();
        } catch (Throwable e) {
            error = e;
            throw e;
        } finally {
            // 先结束收集（补查修改后数据、清空 ThreadLocal），再写日志；无论成功失败都记录
            OperRecordCollector.Result changes = OperRecordCollector.finish(dataSource);
            writeLog(operLog, error, joinPoint.getArgs(), changes, start);
        }
    }

    /** 写日志（任何异常都不能影响业务） */
    private void writeLog(OperLog operLog, Throwable error, Object[] args,
                          OperRecordCollector.Result changes, long start) {
        try {
            operLogService.record(operLog.module(), operLog.action(),
                    error == null ? UcOperLog.RESULT_SUCCESS : UcOperLog.RESULT_FAIL,
                    error == null ? null : error.getMessage(),
                    args, changes.before(), changes.after(),
                    SpringMVCUtil.isWeb() ? SpringMVCUtil.getRequest() : null,
                    System.currentTimeMillis() - start);
        } catch (Exception e) {
            // 日志记录失败不影响接口返回，但需留下痕迹便于排查
            log.warn("操作日志记录失败：module={}，action={}：{}", operLog.module(), operLog.action(), e.getMessage());
        }
    }

}
