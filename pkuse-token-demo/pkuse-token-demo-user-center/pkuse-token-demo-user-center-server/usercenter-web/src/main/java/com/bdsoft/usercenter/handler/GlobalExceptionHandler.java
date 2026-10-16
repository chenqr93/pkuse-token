package com.bdsoft.usercenter.handler;

import com.bdsoft.pkusetoken.exception.NotLoginException;
import com.bdsoft.pkusetoken.exception.NotPermissionException;
import com.bdsoft.pkusetoken.exception.NotRoleException;
import com.bdsoft.pkusetoken.exception.PkuseTokenException;
import com.bdsoft.pkusetoken.util.PkuseResult;
import com.bdsoft.usercenter.common.exception.BizException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理：统一转换为 PkuseResult 返回，前端按 code 判断
 *
 * <pre>
 *   code = 200  成功
 *   code = 401  未登录（前端跳转统一登录页）
 *   code = 500  业务失败 / 系统异常
 * </pre>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** 未登录异常：code=401，前端据此跳转登录页 */
    @ExceptionHandler(NotLoginException.class)
    public PkuseResult handleNotLogin(NotLoginException e) {
        return PkuseResult.get(PkuseResult.CODE_NOT_LOGIN, "未登录或登录已过期，请重新登录", null);
    }

    /** 无权限异常（角色/权限不足）：code=403，管理接口仅系统管理员可用 */
    @ExceptionHandler({NotRoleException.class, NotPermissionException.class})
    public PkuseResult handleNotPermission(Exception e) {
        return PkuseResult.get(PkuseResult.CODE_NOT_PERMISSION, "无权限访问：该功能仅系统管理员可用", null);
    }

    /** 业务异常：使用异常携带的业务状态码与附加数据（如 421=新设备验证 / 423=账号锁定 / 428=需要验证码） */
    @ExceptionHandler(BizException.class)
    public PkuseResult handleBiz(BizException e) {
        return PkuseResult.get(e.getCode(), e.getMessage(), e.getData());
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
