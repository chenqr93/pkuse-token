package com.bdsoft.client.config;

import com.bdsoft.pkusetoken.exception.NotLoginException;
import com.bdsoft.pkusetoken.exception.NotPermissionException;
import com.bdsoft.pkusetoken.util.PkuseResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理：统一转换为 PkuseResult（前端按 code 判断）
 *
 * <p> 401=未登录（跳统一登录）/ 403=无权限（按钮级鉴权演示的核心返回）/ 500=系统异常。 </p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** 未登录：code=401，前端跳统一登录 */
    @ExceptionHandler(NotLoginException.class)
    public PkuseResult handleNotLogin(NotLoginException e) {
        return PkuseResult.get(PkuseResult.CODE_NOT_LOGIN, "未登录或登录已过期，请重新登录", null);
    }

    /** 无权限：code=403（如 zhangsan 无 sys:user:del 权限码时调用受保护接口） */
    @ExceptionHandler({NotPermissionException.class,
            com.bdsoft.pkusetoken.exception.NotRoleException.class})
    public PkuseResult handleNotPermission(Exception e) {
        return PkuseResult.get(PkuseResult.CODE_NOT_PERMISSION, "无权限访问：" + e.getMessage(), null);
    }

    /** 兜底异常 */
    @ExceptionHandler(Exception.class)
    public PkuseResult handleException(Exception e) {
        log.error("请求处理异常", e);
        return PkuseResult.error("系统异常：" + e.getMessage());
    }
}
