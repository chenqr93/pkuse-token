package com.bdsoft.authcenter.support.log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

/**
 * 操作日志月分区维护器（ac_oper_log 按 create_time 按月分区）
 *
 * <p> 启动时与每日 00:05 自动创建「当前月 + 下一月」分区（区间连续不重叠，CREATE IF NOT EXISTS 幂等）； </p>
 * <p> 与用户中心的可配频率分区不同，授权中心日志量小，固定按月（yyyyMM）即可。 </p>
 */
@Component
public class AcLogPartitionMaintainer {

    private static final Logger log = LoggerFactory.getLogger(AcLogPartitionMaintainer.class);

    /** 父表（声明式分区：数据按 create_time 路由到月分区 ac_oper_log_yyyyMM） */
    private static final String PARENT_TABLE = "ac_oper_log";

    /** 分区区间边界格式 */
    private static final DateTimeFormatter BOUND_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final JdbcTemplate jdbcTemplate;

    public AcLogPartitionMaintainer(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    /** 启动时创建当前 + 下一月分区 */
    @jakarta.annotation.PostConstruct
    public void init() {
        try {
            ensurePartitions();
        } catch (Exception e) {
            log.error("操作日志分区启动维护失败（不影响启动）：{}", e.getMessage());
        }
    }

    /** 每日 00:05 维护（滚动创建下一月分区） */
    @Scheduled(cron = "0 5 0 * * ?")
    public void dailyMaintain() {
        try {
            ensurePartitions();
        } catch (Exception e) {
            log.warn("操作日志分区定时维护失败：{}", e.getMessage());
        }
    }

    /** 确保「当前月 + 下一月」分区存在 */
    public void ensurePartitions() {
        ensurePartition(YearMonth.now());
        ensurePartition(YearMonth.now().plusMonths(1));
    }

    /** 创建单个月分区（已存在则跳过） */
    private void ensurePartition(YearMonth month) {
        String partition = PARENT_TABLE + "_" + month.format(DateTimeFormatter.ofPattern("yyyyMM"));
        LocalDate rangeStart = month.atDay(1);
        LocalDate rangeEnd = month.plusMonths(1).atDay(1);
        Integer exists = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM pg_class WHERE relname = ?", Integer.class, partition);
        if (exists != null && exists > 0) {
            return;
        }
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS " + partition
                + " PARTITION OF " + PARENT_TABLE
                + " FOR VALUES FROM ('" + rangeStart.format(BOUND_FORMATTER) + "')"
                + " TO ('" + rangeEnd.format(BOUND_FORMATTER) + "')");
        // 分区内索引：操作人 + 模块 + 时间（各分区独立建立）
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_" + partition + "_user ON " + partition + " (oper_user_name)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_" + partition + "_module ON " + partition + " (module)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_" + partition + "_time ON " + partition + " (create_time)");
        log.info("操作日志分区已创建：{}（{} ~ {}）", partition, rangeStart, rangeEnd);
    }
}
