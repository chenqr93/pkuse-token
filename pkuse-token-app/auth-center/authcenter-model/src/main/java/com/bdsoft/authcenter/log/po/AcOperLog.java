package com.bdsoft.authcenter.log.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 授权中心-操作日志表（ac_oper_log）
 *
 * <p> 管理动作审计（V1 参数摘要版，普通表不分区）；由 @OperLog + OperLogAspect 自动写入。 </p>
 * <p> 追加型日志表：仅保留创建时间，不继承审计父类。 </p>
 */
@TableName("ac_oper_log")
public class AcOperLog implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键（32 位 UUID 字符串） */
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    /** 操作人 ID */
    private String operUserId;

    /** 操作人账号（冗余存储，用户中心删除用户后日志仍可读） */
    private String operUserName;

    /** 模块（如：授权中心-菜单管理） */
    private String module;

    /** 动作（如：新增菜单） */
    private String action;

    /** 请求方法与路径（如：POST /api/authz/menu） */
    private String method;

    /** 请求参数摘要（敏感字段脱敏，超长截断） */
    private String params;

    /** 修改前数据快照（JSON：[{table,operation,rows:[...]}]，由变更捕获拦截器自动登记） */
    private String beforeData;

    /** 修改后数据快照（JSON：新增=写入数据（含主键）、更新=修改后、删除为空） */
    private String afterData;

    /** 结果：1=成功 0=失败 */
    private Integer success;

    /** 结果说明 / 失败原因 */
    private String message;

    /** 耗时（毫秒） */
    private Long costMs;

    /** 来源 IP */
    private String ip;

    /** 浏览器 User-Agent */
    private String ua;

    /** 创建时间 */
    private LocalDateTime createTime;

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
