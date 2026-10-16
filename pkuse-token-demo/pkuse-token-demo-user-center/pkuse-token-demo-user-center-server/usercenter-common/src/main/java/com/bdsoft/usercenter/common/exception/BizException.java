package com.bdsoft.usercenter.common.exception;

/**
 * 业务异常：由 Service 层抛出，全局异常处理器统一转换为接口返回值
 *
 * <p> 可携带业务状态码（如 421=新设备需验证、423=账号锁定、428=需要验证码）与 </p>
 * <p> 附加数据（如新设备验证所需的 verifyToken、脱敏手机号），前端据此展示对应交互。 </p>
 */
public class BizException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** 业务状态码（默认 500） */
    private final int code;

    /** 附加数据（返回给前端，可为空） */
    private final transient Object data;

    public BizException(String message) {
        this(500, message, null);
    }

    public BizException(String message, Throwable cause) {
        this(500, message, cause);
    }

    public BizException(int code, String message) {
        this(code, message, (Object) null);
    }

    public BizException(int code, String message, Object data) {
        super(message);
        this.code = code;
        this.data = data;
    }

    public BizException(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.data = null;
    }

    public int getCode() {
        return code;
    }

    public Object getData() {
        return data;
    }

}
