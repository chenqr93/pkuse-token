package com.bdsoft.authcenter.controller.log;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bdsoft.authcenter.dto.log.OperLogQueryDTO;
import com.bdsoft.authcenter.po.log.AcOperLog;
import com.bdsoft.authcenter.service.log.AcOperLogService;
import com.bdsoft.authcenter.vo.log.OperLogVO;
import com.bdsoft.authcenter.common.pojo.PageData;
import com.bdsoft.pkusetoken.util.PkuseResult;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.stream.Collectors;

/**
 * 操作日志 Controller（本地 ac_oper_log 只读查询）
 */
@RestController
@RequestMapping("/api/authz/log/oper")
public class OperLogController {

    @Resource
    private AcOperLogService operLogService;


    /** 操作日志分页（操作人/模块/动作/结果/时间范围筛选） */
    @GetMapping("/page")
    public PkuseResult page(OperLogQueryDTO query) {
        Page<AcOperLog> page = operLogService.pageQuery(query);
        return PkuseResult.ok().setData(PageData.of(
                page.getRecords().stream().map(OperLogVO::from).collect(Collectors.toList()),
                page.getTotal(), page.getCurrent(), page.getSize()));
    }
}
