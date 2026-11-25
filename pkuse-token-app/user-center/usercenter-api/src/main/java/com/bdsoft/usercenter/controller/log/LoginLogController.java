package com.bdsoft.usercenter.controller.log;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bdsoft.pkusetoken.util.PkuseResult;
import com.bdsoft.usercenter.dto.log.LoginLogQueryDTO;
import com.bdsoft.usercenter.po.log.UcLoginLog;
import com.bdsoft.usercenter.service.log.LoginLogService;
import com.bdsoft.usercenter.vo.log.LoginLogVO;
import com.bdsoft.usercenter.common.pojo.PageData;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 登录日志接口（仅系统管理员可访问）
 *
 * <p> 日志按月份表存储（uc_login_log_yyyyMM），此处分页查询会自动跨月分区。 </p>
 *
 * <p> 入参为 DTO、出参为 VO：实体不进入接口层。 </p>
 */
@RestController
@RequestMapping("/api/log")
public class LoginLogController {

    @Resource
    private LoginLogService loginLogService;

    /** 分页查询登录日志（支持账号、结果、时间范围筛选） */
    @GetMapping("/login/page")
    public PkuseResult page(LoginLogQueryDTO query) {
        Page<UcLoginLog> page = loginLogService.pageQuery(query);
        List<LoginLogVO> records = page.getRecords().stream().map(LoginLogVO::from).toList();
        return PkuseResult.ok().setData(PageData.of(records, page.getTotal(), page.getCurrent(), page.getSize()));
    }

}
