package com.bdsoft.usercenter.auth.service;

/**
 * 登录失败计数 Service
 *
 * <p> 连续失败次数存于 Redis（框架 Dao，带窗口过期时间），用于： </p>
 * <ul>
 *   <li>触发「多次密码错误锁定」（达到阈值写入账号锁定状态）；</li>
 *   <li>决定是否要求滑块验证码（失败次数达到策略阈值）。 </li>
 * </ul>
 */
public interface LoginAttemptService {

    /** 当前窗口内的连续失败次数 */
    long getFailCount(String username);

    /** 记录一次失败（返回本次失败后的累计次数） */
    long increaseFailCount(String username);

    /** 登录成功 / 管理员解锁后清零 */
    void clearFailCount(String username);

    /** 是否需要滑块验证码（策略：user-center.login.captcha-after-fail） */
    boolean needCaptcha(String username);

}
