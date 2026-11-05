package com.bdsoft.authcenter.log.support;

import com.bdsoft.authcenter.common.annotation.OperLog;
import com.bdsoft.authcenter.log.po.AcOperLog;
import com.bdsoft.authcenter.log.service.AcOperLogService;
import com.bdsoft.authcenter.usercenter.bo.UserInfo;
import com.bdsoft.authcenter.usercenter.support.UserInfoCache;
import com.bdsoft.pkusetoken.stp.StpUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 操作日志切面：@OperLog 方法自动记录管理动作审计（写本地 ac_oper_log，按月分区）
 *
 * <p> 记录内容：操作人（会话 + 缓存用户名）、模块、动作、请求方法与路径、请求参数摘要（敏感字段脱敏）、 </p>
 * <p> <b>修改前/后数据快照</b>、结果与说明、耗时、IP/UA。写日志失败不影响业务主流程。 </p>
 *
 * <p> <b>变更前后数据</b>由 MyBatis-Plus 拦截器体系自动捕获：方法执行期间， </p>
 * <p> {@link OperChangeCaptureInterceptor} 把所有落库的 INSERT/UPDATE/DELETE（不限定实体类型， </p>
 * <p> 一个方法更新多张表 / 多个实体同样覆盖）登记到 {@link OperRecordCollector}， </p>
 * <p> 方法结束时统一整理为 before_data（修改前）/ after_data（修改后，新增=写入数据含主键），随日志写入。 </p>
 */
@Aspect
@Component
public class OperLogAspect {

    private static final Logger log = LoggerFactory.getLogger(OperLogAspect.class);

    /** 参数/快照最大长度（超长截断） */
    private static final int MAX_TEXT_LENGTH = 2000;

    /** 敏感字段关键字（键名包含即脱敏为 ***） */
    private static final String[] SENSITIVE_KEYS = {"password", "salt", "secret", "token", "captcha"};

    private final AcOperLogService operLogService;
    private final UserInfoCache userInfoCache;
    private final DataSource dataSource;

    /** 快照/参数序列化（注册 classpath 上的 Jackson 模块以支持 LocalDateTime 等 Java8 时间类型） */
    private final ObjectMapper objectMapper = new ObjectMapper()
            .findAndRegisterModules()
            .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    public OperLogAspect(AcOperLogService operLogService,
                         UserInfoCache userInfoCache,
                         DataSource dataSource) {
        this.operLogService = operLogService;
        this.userInfoCache = userInfoCache;
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
            saveLog(joinPoint, operLog, error, changes, System.currentTimeMillis() - start);
        }
    }

    /** 写操作日志（任何异常仅告警，不影响业务） */
    private void saveLog(ProceedingJoinPoint joinPoint, OperLog operLog, Throwable error,
                         OperRecordCollector.Result changes, long costMs) {
        try {
            AcOperLog record = new AcOperLog();
            record.setModule(operLog.module());
            record.setAction(operLog.action());
            record.setSuccess(error == null ? 1 : 0);
            record.setMessage(truncate(error == null ? "success" : error.getMessage(), 500));
            record.setCostMs(costMs);
            record.setCreateTime(LocalDateTime.now());
            record.setMethod(requestMethod());

            Object loginId = StpUtil.getLoginIdDefaultNull();
            if (loginId != null) {
                record.setOperUserId(loginId.toString());
                UserInfo info = userInfoCache.get(loginId.toString());
                record.setOperUserName(info == null ? null : info.getUsername());
            }

            HttpServletRequest request = currentRequest();
            if (request != null) {
                record.setIp(clientIp(request));
                record.setUa(truncate(request.getHeader("User-Agent"), 500));
            }

            record.setParams(toJson(maskSensitive(joinPoint.getArgs(), "")));
            record.setBeforeData(toJson(maskSensitive(changes.before(), "")));
            record.setAfterData(toJson(maskSensitive(changes.after(), "")));
            operLogService.save(record);
        } catch (Exception e) {
            log.warn("操作日志写入失败（不影响业务）：{}", e.getMessage());
        }
    }

    /** 请求方法与路径（如：POST /api/authz/menu） */
    private String requestMethod() {
        HttpServletRequest request = currentRequest();
        if (request == null) {
            return null;
        }
        return request.getMethod() + " " + request.getRequestURI();
    }

    /** 方法入参序列化摘要（排除 Servlet 对象；敏感字段脱敏；超长截断） */
    private Object maskSensitive(Object value, String keyHint) {
        for (String sensitive : SENSITIVE_KEYS) {
            if (keyHint.toLowerCase().contains(sensitive)) {
                return "***";
            }
        }
        if (value instanceof Map) {
            Map<String, Object> result = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
                result.put(String.valueOf(entry.getKey()), maskSensitive(entry.getValue(), String.valueOf(entry.getKey())));
            }
            return result;
        }
        if (value instanceof List) {
            List<Object> result = new ArrayList<>();
            for (Object item : (List<?>) value) {
                result.add(maskSensitive(item, ""));
            }
            return result;
        }
        if (value != null && value.getClass().getName().startsWith("com.bdsoft.")) {
            try {
                Map<?, ?> map = objectMapper.convertValue(value, Map.class);
                return maskSensitive(map, "");
            } catch (Exception e) {
                return String.valueOf(value);
            }
        }
        if (value instanceof Object[] args) {
            List<Object> result = new ArrayList<>();
            for (Object arg : args) {
                if (arg instanceof HttpServletRequest || arg instanceof jakarta.servlet.http.HttpServletResponse) {
                    continue;
                }
                result.add(maskSensitive(arg, ""));
            }
            return result;
        }
        return value;
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return truncate(objectMapper.writeValueAsString(value), MAX_TEXT_LENGTH);
        } catch (Exception e) {
            return truncate("序列化失败：" + e.getMessage(), MAX_TEXT_LENGTH);
        }
    }

    /** 客户端 IP（代理场景优先 X-Forwarded-For 首个地址） */
    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private HttpServletRequest currentRequest() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes == null ? null : attributes.getRequest();
    }

    private static String truncate(String text, int maxLength) {
        if (text == null) {
            return null;
        }
        return text.length() <= maxLength ? text : text.substring(0, maxLength) + "...(截断)";
    }
}
