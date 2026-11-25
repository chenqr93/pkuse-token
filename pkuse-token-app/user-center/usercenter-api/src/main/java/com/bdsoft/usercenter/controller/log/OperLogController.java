package com.bdsoft.usercenter.controller.log;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bdsoft.pkusetoken.util.PkuseResult;
import com.bdsoft.usercenter.dto.log.OperLogQueryDTO;
import com.bdsoft.usercenter.po.log.UcOperLog;
import com.bdsoft.usercenter.service.log.OperLogService;
import com.bdsoft.usercenter.vo.log.OperLogVO;
import com.bdsoft.usercenter.common.pojo.PageData;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 操作日志接口（仅系统管理员可访问）
 *
 * <p> 记录平台管理动作（新增用户、修改应用、授权、解锁、解绑设备等）；日志按月分表存储， </p>
 * <p> 此处分页查询会自动跨月分区。 </p>
 *
 * <p> 入参为 DTO、出参为 VO：实体不进入接口层。 </p>
 */
@RestController
@RequestMapping("/api/log")
public class OperLogController {

    @Resource
    private OperLogService operLogService;

    /** 分页查询操作日志（支持操作人、模块、动作、结果、时间范围筛选） */
    @GetMapping("/oper/page")
    public PkuseResult page(OperLogQueryDTO query) {
        Page<UcOperLog> page = operLogService.pageQuery(query);
        List<OperLogVO> records = page.getRecords().stream().map(OperLogVO::from).toList();
        return PkuseResult.ok().setData(PageData.of(records, page.getTotal(), page.getCurrent(), page.getSize()));
    }

}
