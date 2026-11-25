package com.bdsoft.authcenter.dto.log;

import com.bdsoft.authcenter.common.pojo.PageQueryDTO;

/**
 * 操作日志分页查询参数（DTO）
 */
public class OperLogQueryDTO extends PageQueryDTO {

    /** 操作人账号（模糊匹配，可空） */
    private String operUserName;

    /** 模块（模糊匹配，可空） */
    private String module;

    /** 动作（模糊匹配，可空） */
    private String action;

    /** 结果：1=成功 0=失败（可空表示全部） */
    private Integer success;

    /** 开始时间（可空） */
    private String beginTime;

    /** 结束时间（可空） */
    private String endTime;

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

    public Integer getSuccess() {
        return success;
    }

    public void setSuccess(Integer success) {
        this.success = success;
    }

    public String getBeginTime() {
        return beginTime;
    }

    public void setBeginTime(String beginTime) {
        this.beginTime = beginTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }
}
