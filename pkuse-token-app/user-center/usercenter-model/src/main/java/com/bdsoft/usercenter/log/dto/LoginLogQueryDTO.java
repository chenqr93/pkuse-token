package com.bdsoft.usercenter.log.dto;

import com.bdsoft.usercenter.common.pojo.PageQueryDTO;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/**
 * 登录日志分页查询参数（数据传输对象）
 */
public class LoginLogQueryDTO extends PageQueryDTO {

    /** 登录账号（模糊匹配，可空） */
    private String username;

    /** 结果：success / fail / locked / captcha_fail / device_lock / disabled / no_user（可空） */
    private String result;

    /** 开始时间（可空） */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime begin;

    /** 结束时间（可空） */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime end;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public LocalDateTime getBegin() {
        return begin;
    }

    public void setBegin(LocalDateTime begin) {
        this.begin = begin;
    }

    public LocalDateTime getEnd() {
        return end;
    }

    public void setEnd(LocalDateTime end) {
        this.end = end;
    }

    @Override
    public String toString() {
        return "LoginLogQueryDTO{" + super.toString() +
                ", username='" + username + '\'' +
                ", result='" + result + '\'' +
                ", begin=" + begin +
                ", end=" + end +
                '}';
    }
}
