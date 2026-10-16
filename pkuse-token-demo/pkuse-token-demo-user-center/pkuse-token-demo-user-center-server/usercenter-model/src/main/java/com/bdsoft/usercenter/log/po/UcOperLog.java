package com.bdsoft.usercenter.log.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户中心-操作日志表（uc_oper_log，按 create_time 按月分表）
 *
 * <p> 记录平台管理动作（新增用户、修改应用、授权、解锁、解绑设备等）： </p>
 * <p> 操作人、模块、动作、请求方法、参数摘要（脱敏）、结果、耗时、IP/UA 等。 </p>
 */
@TableName("uc_oper_log")
public class UcOperLog implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键（32 位 UUID 字符串） */
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    /** 操作人用户 ID */
    private String userId;

    /** 操作人账号 */
    private String username;

    /** 模块：用户管理 / 应用管理 / 授权 / 设备 / 登录安全 */
    private String module;

    /** 动作：新增用户 / 修改应用 / 批量授权 ... */
    private String action;

    /** 请求方法（HTTP 方法 + 路径） */
    private String method;

    /** 请求参数摘要（JSON，密码等敏感字段已脱敏） */
    private String params;

    /** 修改前数据快照（JSON，更新/删除时记录；敏感字段已脱敏） */
    private String beforeData;

    /** 修改后数据快照（JSON，新增/更新时记录；新增含生成的主键） */
    private String afterData;

    /** 结果：success / fail */
    private String result;

    /** 说明信息（失败时为异常信息） */
    private String msg;

    /** 耗时（毫秒） */
    private Long costTime;

    /** 客户端 IP */
    private String ip;

    /** User-Agent */
    private String userAgent;

    /** 操作系统 */
    private String os;

    /** 浏览器 */
    private String browser;

    /** 发生时间（分区键） */
    private LocalDateTime createTime;

    public static final String RESULT_SUCCESS = "success";
    public static final String RESULT_FAIL = "fail";

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getModule() {
        return module;
    }

    public void setModule(String module) {
        this.module = module;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getParams() {
        return params;
    }

    public void setParams(String params) {
        this.params = params;
    }

    public String getBeforeData() {
        return beforeData;
    }

    public void setBeforeData(String beforeData) {
        this.beforeData = beforeData;
    }

    public String getAfterData() {
        return afterData;
    }

    public void setAfterData(String afterData) {
        this.afterData = afterData;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public Long getCostTime() {
        return costTime;
    }

    public void setCostTime(Long costTime) {
        this.costTime = costTime;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getOs() {
        return os;
    }

    public void setOs(String os) {
        this.os = os;
    }

    public String getBrowser() {
        return browser;
    }

    public void setBrowser(String browser) {
        this.browser = browser;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    @Override
    public String toString() {
        return "UcOperLog{" +
                "id='" + id + '\'' +
                ", userId='" + userId + '\'' +
                ", username='" + username + '\'' +
                ", module='" + module + '\'' +
                ", action='" + action + '\'' +
                ", method='" + method + '\'' +
                ", params='" + params + '\'' +
                ", beforeData='" + beforeData + '\'' +
                ", afterData='" + afterData + '\'' +
                ", result='" + result + '\'' +
                ", msg='" + msg + '\'' +
                ", costTime=" + costTime +
                ", ip='" + ip + '\'' +
                ", userAgent='" + userAgent + '\'' +
                ", os='" + os + '\'' +
                ", browser='" + browser + '\'' +
                ", createTime=" + createTime +
                '}';
    }
}
