package com.bdsoft.authcenter.service.log.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bdsoft.authcenter.dto.log.OperLogQueryDTO;
import com.bdsoft.authcenter.po.log.AcOperLog;
import com.bdsoft.authcenter.mapper.log.AcOperLogMapper;
import com.bdsoft.authcenter.service.log.AcOperLogService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 操作日志 Service 实现（追加写入复用 IService.save，此处仅分页查询）
 */
@Service
public class AcOperLogServiceImpl extends ServiceImpl<AcOperLogMapper, AcOperLog> implements AcOperLogService {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public Page<AcOperLog> pageQuery(OperLogQueryDTO query) {
        LocalDateTime begin = parseTime(query.getBeginTime());
        LocalDateTime end = parseTime(query.getEndTime());
        return this.page(new Page<>(query.currentOrDefault(), query.sizeOrDefault()),
                new LambdaQueryWrapper<AcOperLog>()
                        .like(hasText(query.getOperUserName()), AcOperLog::getOperUserName, query.getOperUserName())
                        .like(hasText(query.getModule()), AcOperLog::getModule, query.getModule())
                        .like(hasText(query.getAction()), AcOperLog::getAction, query.getAction())
                        .eq(query.getSuccess() != null, AcOperLog::getSuccess, query.getSuccess())
                        .ge(begin != null, AcOperLog::getCreateTime, begin)
                        .le(end != null, AcOperLog::getCreateTime, end)
                        .orderByDesc(AcOperLog::getCreateTime)
                        .orderByDesc(AcOperLog::getId));
    }

    private static LocalDateTime parseTime(String text) {
        if (!hasText(text)) {
            return null;
        }
        try {
            return LocalDateTime.parse(text.trim(), TIME_FORMATTER);
        } catch (Exception e) {
            return null;
        }
    }

    private static boolean hasText(String text) {
        return text != null && !text.isBlank();
    }
}
