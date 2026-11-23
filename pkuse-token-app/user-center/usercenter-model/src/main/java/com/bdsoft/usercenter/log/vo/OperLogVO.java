package com.bdsoft.usercenter.log.vo;

import com.bdsoft.usercenter.log.po.UcOperLog;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 操作日志视图对象（Controller 返回给前端的响应对象）
 */
public class OperLogVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键（序列化为字符串，避免前端 JS 精度丢失） */
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

    /** 修改前数据快照（JSON，敏感字段已脱敏） */
    private String beforeData;

    /** 修改后数据快照（JSON，敏感字段已脱敏） */
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

    /** 发生时间 */
    private LocalDateTime createTime;

    /** 由实体转换为视图对象 */
    public static OperLogVO from(UcOperLog log) {
        if (log == null) {
            return null;
        }
        OperLogVO vo = new OperLogVO();
        vo.setId(log.getId());
        vo.setUserId(log.getUserId());
        vo.setUsername(log.getUsername());
        vo.setModule(log.getModule());
        vo.setAction(log.getAction());
        vo.setMethod(log.getMethod());
        vo.setParams(log.getParams());
        vo.setBeforeData(log.getBeforeData());
        vo.setAfterData(log.getAfterData());
        vo.setResult(log.getResult());
        vo.setMsg(log.getMsg());
        vo.setCostTime(log.getCostTime());
        vo.setIp(log.getIp());
        vo.setUserAgent(log.getUserAgent());
        vo.setOs(log.getOs());
        vo.setBrowser(log.getBrowser());
        vo.setCreateTime(log.getCreateTime());
        return vo;
    }

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
        return "OperLogVO{" +
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
