package com.bdsoft.apiplatform.handler;

import com.bdsoft.pkusetoken.exception.NotLoginException;
import com.bdsoft.pkusetoken.exception.NotPermissionException;
import com.bdsoft.pkusetoken.exception.NotRoleException;
import com.bdsoft.pkusetoken.exception.PkuseTokenException;
import com.bdsoft.pkusetoken.util.PkuseResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理：统一转换为 PkuseResult 信封返回，前端按 code 判断
 *
 * <pre>
 *   code = 200  成功
 *   code = 401  未登录（前端跳转统一登录）
 *   code = 403  无权限（开放接口未授权权限码时）
 *   code = 500  系统异常
 * </pre>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** 未登录异常：code=401 */
    @ExceptionHandler(NotLoginException.class)
    public PkuseResult handleNotLogin(NotLoginException e) {
        return PkuseResult.get(PkuseResult.CODE_NOT_LOGIN, "未登录或会话已失效，请先完成统一登录或获取开放接口令牌", null);
    }

    /** 无权限异常（角色/权限不足）：code=403 */
    @ExceptionHandler({NotRoleException.class, NotPermissionException.class})
    public PkuseResult handleNotPermission(Exception e) {
        return PkuseResult.get(PkuseResult.CODE_NOT_PERMISSION, "无权限访问该接口（需要权限码：" + e.getMessage() + "）", null);
    }

    /** Pkuse-Token 框架异常（如 SSO 协议参数不合法）：直接返回框架提示 */
    @ExceptionHandler(PkuseTokenException.class)
    public PkuseResult handlePkuseToken(PkuseTokenException e) {
        return PkuseResult.error(e.getMessage());
    }

    /** 兜底异常 */
    @ExceptionHandler(Exception.class)
    public PkuseResult handleException(Exception e) {
        log.error("请求处理异常", e);
        return PkuseResult.error("系统异常：" + e.getMessage());
    }
}
