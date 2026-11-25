package com.bdsoft.authcenter.vo.log;

import com.bdsoft.authcenter.po.log.AcOperLog;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 操作日志视图（VO）
 */
public class OperLogVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;

    private String operUserId;

    private String operUserName;

    private String module;

    private String action;

    private String method;

    private String params;

    /** 修改前数据快照（JSON） */
    private String beforeData;

    /** 修改后数据快照（JSON） */
    private String afterData;

    private Integer success;

    private String message;

    private Long costMs;

    private String ip;

    private String ua;

    private LocalDateTime createTime;

    public static OperLogVO from(AcOperLog log) {
        if (log == null) {
            return null;
        }
        OperLogVO vo = new OperLogVO();
        vo.id = log.getId();
        vo.operUserId = log.getOperUserId();
        vo.operUserName = log.getOperUserName();
        vo.module = log.getModule();
        vo.action = log.getAction();
        vo.method = log.getMethod();
        vo.params = log.getParams();
        vo.beforeData = log.getBeforeData();
        vo.afterData = log.getAfterData();
        vo.success = log.getSuccess();
        vo.message = log.getMessage();
        vo.costMs = log.getCostMs();
        vo.ip = log.getIp();
        vo.ua = log.getUa();
        vo.createTime = log.getCreateTime();
        return vo;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getOperUserId() {
        return operUserId;
    }

    public void setOperUserId(String operUserId) {
        this.operUserId = operUserId;
    }

    public String getOperUserName() {
        return operUserName;
    }

    public void setOperUserName(String operUserName) {
        this.operUserName = operUserName;
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

    public Integer getSuccess() {
        return success;
    }

    public void setSuccess(Integer success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Long getCostMs() {
        return costMs;
    }

    public void setCostMs(Long costMs) {
        this.costMs = costMs;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getUa() {
        return ua;
    }

    public void setUa(String ua) {
        this.ua = ua;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}
