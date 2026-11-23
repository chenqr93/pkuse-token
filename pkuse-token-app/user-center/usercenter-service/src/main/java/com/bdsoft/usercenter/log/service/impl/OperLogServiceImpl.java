package com.bdsoft.usercenter.log.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bdsoft.pkusetoken.stp.StpUtil;
import com.bdsoft.usercenter.log.support.LogPartitionMaintainer;
import com.bdsoft.usercenter.common.util.RequestInfoUtil;
import com.bdsoft.usercenter.log.dto.OperLogQueryDTO;
import com.bdsoft.usercenter.log.po.UcOperLog;
import com.bdsoft.usercenter.user.po.UcUser;
import com.bdsoft.usercenter.log.mapper.UcOperLogMapper;
import com.bdsoft.usercenter.user.mapper.UcUserMapper;
import com.bdsoft.usercenter.log.service.OperLogService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 操作日志 Service 实现
 *
 * <p> 参数摘要会先做敏感字段脱敏（password / secretKey / captchaTrack 等）， </p>
 * <p> 再截断到列宽以内；写入失败不影响主流程。 </p>
 */
@Service
public class OperLogServiceImpl extends ServiceImpl<UcOperLogMapper, UcOperLog> implements OperLogService {

    private static final Logger log = LoggerFactory.getLogger(OperLogServiceImpl.class);

    /** 参数摘要最大长度（与表字段长度一致） */
    private static final int PARAMS_MAX_LENGTH = 2000;

    /**
     * 敏感字段脱敏规则：键名包含下列关键字（忽略大小写）时，其字符串值替换为 ***
     *
     * <p> 不含 password/secret/token/captcha 的普通配置值（如布尔开关）保持原样，便于审计； </p>
     * <p> 例外：值为 true/false 的键不脱敏（配置项开关），密码等真实值仍会被替换。 </p>
     */
    private static final Pattern SENSITIVE_PATTERN = Pattern.compile(
            "(?i)(\"[A-Za-z0-9_.-]*(password|passwd|pwd|salt|secret|token|captcha)[A-Za-z0-9_.-]*\"\\s*:\\s*)\"(?!true\"|false\"|null\")[^\"]*\"");

    private final LogPartitionMaintainer partitionMaintainer;

    private final UcUserMapper userMapper;

    /** 与 MVC 共用的 Jackson（已注册 Java8 时间模块，可序列化 LocalDateTime 字段） */
    private final ObjectMapper objectMapper;

    public OperLogServiceImpl(LogPartitionMaintainer partitionMaintainer, UcUserMapper userMapper,
                              ObjectMapper objectMapper) {
        this.partitionMaintainer = partitionMaintainer;
        this.userMapper = userMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public void record(String module, String action, String result, String msg,
                       Object[] args, Object beforeData, Object afterData,
                       HttpServletRequest request, long costTime) {
        String userAgent = RequestInfoUtil.getUserAgent(request);
        UcOperLog operLog = new UcOperLog();
        // 操作人：未登录（理论上管理接口都要求登录）时留空
        try {
            if (StpUtil.isLogin()) {
                operLog.setUserId(String.valueOf(StpUtil.getLoginId()));
            }
        } catch (Exception ignored) {
            // 忽略：仅影响操作人记录
        }
        operLog.setUsername(currentUsername(operLog.getUserId()));
        operLog.setModule(module);
        operLog.setAction(action);
        operLog.setResult(result);
        operLog.setMsg(truncate(msg, 500));
        operLog.setCostTime(costTime);
        operLog.setMethod(request == null ? null
                : truncate(request.getMethod() + " " + request.getRequestURI(), 255));
        operLog.setParams(buildParams(args));
        operLog.setBeforeData(toJsonMasked(beforeData));
        operLog.setAfterData(toJsonMasked(afterData));
        operLog.setIp(RequestInfoUtil.getClientIp(request));
        operLog.setUserAgent(userAgent);
        operLog.setOs(RequestInfoUtil.parseOs(userAgent));
        operLog.setBrowser(RequestInfoUtil.parseBrowser(userAgent));
        operLog.setCreateTime(LocalDateTime.now());

        // 兜底：确保当前周期分表存在（正常已在启动/定时任务中创建）
        partitionMaintainer.ensureCurrentPeriod(LogPartitionMaintainer.TABLE_OPER);
        try {
            this.save(operLog);
        } catch (Exception e) {
            // 日志写入失败不影响主流程
            log.warn("操作日志写入失败：{}", e.getMessage());
        }
    }

    @Override
    public Page<UcOperLog> pageQuery(OperLogQueryDTO query) {
        LambdaQueryWrapper<UcOperLog> wrapper = new LambdaQueryWrapper<UcOperLog>()
                .like(hasText(query.getUsername()), UcOperLog::getUsername, query.getUsername())
                .eq(hasText(query.getModule()), UcOperLog::getModule, query.getModule())
                .like(hasText(query.getAction()), UcOperLog::getAction, query.getAction())
                .eq(hasText(query.getResult()), UcOperLog::getResult, query.getResult())
                .ge(query.getBegin() != null, UcOperLog::getCreateTime, query.getBegin())
                .le(query.getEnd() != null, UcOperLog::getCreateTime, query.getEnd())
                .orderByDesc(UcOperLog::getCreateTime);
        return this.page(new Page<>(query.currentOrDefault(), query.sizeOrDefault()), wrapper);
    }

    // ------------------------- 私有方法

    /** 操作人账号（按登录会话中的用户 ID 查库，便于日志直接展示） */
    private String currentUsername(String userId) {
        if (userId == null) {
            return null;
        }
        try {
            UcUser user = userMapper.selectById(userId);
            return user == null ? null : user.getUsername();
        } catch (Exception e) {
            return null;
        }
    }

    /** 请求参数摘要：过滤 Servlet 对象与非序列化对象，敏感字段脱敏，超长截断 */
    private String buildParams(Object[] args) {
        if (args == null || args.length == 0) {
            return null;
        }
        List<Object> printable = new ArrayList<>();
        for (Object arg : args) {
            if (arg == null) {
                continue;
            }
            // 跳过请求/响应等容器对象
            if (arg instanceof jakarta.servlet.ServletRequest || arg instanceof jakarta.servlet.ServletResponse) {
                continue;
            }
            printable.add(arg);
        }
        if (printable.isEmpty()) {
            return null;
        }
        try {
            String json = objectMapper.writeValueAsString(printable.size() == 1 ? printable.get(0) : printable);
            return truncate(maskSensitive(json), PARAMS_MAX_LENGTH);
        } catch (Exception e) {
            log.warn("操作日志请求参数序列化失败：{}", e.getMessage());
            return null;
        }
    }

    /** 变更前后数据快照：JSON 序列化 + 敏感字段脱敏 + 超长截断（空对象不记录） */
    private String toJsonMasked(Object data) {
        if (data == null) {
            return null;
        }
        try {
            String json = objectMapper.writeValueAsString(data);
            // 空对象 / 空集合不记录
            if ("{}".equals(json) || "[]".equals(json) || "null".equals(json)) {
                return null;
            }
            return truncate(maskSensitive(json), PARAMS_MAX_LENGTH);
        } catch (Exception e) {
            log.warn("操作日志变更数据序列化失败：{}", e.getMessage());
            return null;
        }
    }

    /** 简单脱敏：命中敏感字段名的值替换为 ***（按 JSON 键值对处理，够用且不引入额外依赖） */
    private String maskSensitive(String json) {
        return SENSITIVE_PATTERN.matcher(json).replaceAll("$1\"***\"");
    }

    private static String truncate(String text, int maxLength) {
        if (text == null) {
            return null;
        }
        return text.length() > maxLength ? text.substring(0, maxLength) : text;
    }

    private static boolean hasText(String text) {
        return text != null && !text.isBlank();
    }

}
