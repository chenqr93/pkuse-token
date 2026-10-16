package com.bdsoft.usercenter.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 操作日志注解
 *
 * <p> 标注在管理接口方法上，由 {@code com.bdsoft.usercenter.log.support.OperLogAspect} 自动记录操作日志 </p>
 * <p>（操作人、模块、动作、请求参数摘要、结果、耗时、IP/UA），无需在业务代码中手工埋点。 </p>
 *
 * <p> <b>变更前后数据</b>无需任何声明：方法执行期间所有落库的 INSERT/UPDATE/DELETE 由 MyBatis-Plus </p>
 * <p> 拦截器 {@code com.bdsoft.usercenter.log.support.OperChangeCaptureInterceptor} 自动捕获（不限定实体类型， </p>
 * <p> 一个方法更新多张表 / 多个实体同样覆盖），敏感字段（密码等）脱敏后随日志存储。 </p>
 *
 * <pre>
 *   &#64;OperLog(module = "用户管理", action = "修改用户")
 *   public PkuseResult update(@RequestBody UserSaveDTO param) { ... }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OperLog {

    /** 模块名（如：用户管理 / 应用管理 / 授权管理 / 设备管理 / 登录安全 / 规则配置 / 账号安全） */
    String module();

    /** 动作名（如：新增用户 / 修改应用 / 批量授权） */
    String action();

}
