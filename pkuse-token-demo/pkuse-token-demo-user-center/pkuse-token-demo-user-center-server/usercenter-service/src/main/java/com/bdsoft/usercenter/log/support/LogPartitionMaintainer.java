package com.bdsoft.usercenter.log.support;

import com.bdsoft.usercenter.common.config.SystemConfigChangedEvent;
import com.bdsoft.usercenter.config.service.UcConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 日志分区分表维护（KingbaseES 声明式分区）
 *
 * <p> 登录日志（uc_login_log）与操作日志（uc_oper_log）均为按 create_time 的范围分区父表， </p>
 * <p> 数据自动路由到分区表 <code>{父表}_{周期后缀}</code>；分区频率可在「规则配置」中按表设置 </p>
 * <p>（按月 / 按周 / 按天，见 {@link PartitionFrequency}）。 </p>
 *
 * <p> 维护策略： </p>
 * <ol>
 *   <li>启动时、每日 00:05、以及规则配置变更后：确保「当前周期 + 下一周期」分区存在；</li>
 *   <li>写入日志前：内存兜底检查（命中时无数据库开销）；</li>
 *   <li>频率变更：从数据库现有分区区间的「最大终点」续建，保证区间连续（既不重叠也不留空档）；</li>
 *   <li>历史周期不补建（日志只写当前时间，不会回写历史）。 </li>
 * </ol>
 */
@Component
public class LogPartitionMaintainer {

    private static final Logger log = LoggerFactory.getLogger(LogPartitionMaintainer.class);

    /** 登录日志父表 */
    public static final String TABLE_LOGIN = "uc_login_log";

    /** 操作日志父表 */
    public static final String TABLE_OPER = "uc_oper_log";

    /** 单次维护最多创建的分区数（避免异常情况下创建过多分区） */
    private static final int MAX_PARTITIONS_PER_RUN = 64;

    /** 分区边界（系统目录 pg_get_expr(relpartbound)）解析：FOR VALUES FROM ('..') TO ('..') */
    private static final Pattern BOUND_PATTERN =
            Pattern.compile("FROM\\s*\\(\\s*'([^']+)'\\s*\\)\\s*TO\\s*\\(\\s*'([^']+)'\\s*\\)");

    private static final DateTimeFormatter BOUND_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 分区覆盖终点（内存缓存：父表 → 覆盖情况） */
    private record Coverage(LocalDateTime end, PartitionFrequency frequency) {
    }

    private final Map<String, Coverage> coverageCache = new ConcurrentHashMap<>();

    private final JdbcTemplate jdbcTemplate;

    private final UcConfigService configService;

    public LogPartitionMaintainer(JdbcTemplate jdbcTemplate, UcConfigService configService) {
        this.jdbcTemplate = jdbcTemplate;
        this.configService = configService;
    }

    /** 启动时：确保当前周期与下一周期分区存在（在规则配置加载之后执行） */
    @Order(10)
    @EventListener(ApplicationReadyEvent.class)
    public void initOnStartup() {
        ensureCoverageForAll();
    }

    /** 每日 00:05 检查分区（跨周期自动创建新分区） */
    @Scheduled(cron = "0 5 0 * * ?")
    public void scheduledEnsurePartition() {
        ensureCoverageForAll();
    }

    /** 规则配置变更（如分区频率调整）：立即按新频率续建分区 */
    @EventListener(SystemConfigChangedEvent.class)
    public void onConfigChanged() {
        ensureCoverageForAll();
    }

    /** 按当前配置维护两张日志表的分区 */
    public void ensureCoverageForAll() {
        ensureCoverage(TABLE_LOGIN, frequencyOf(TABLE_LOGIN));
        ensureCoverage(TABLE_OPER, frequencyOf(TABLE_OPER));
    }

    /** 写入日志前兜底：保证「当前周期」已被某个分区覆盖（内存命中时无数据库开销） */
    public void ensureCurrentPeriod(String parentTable) {
        PartitionFrequency frequency = frequencyOf(parentTable);
        LocalDateTime now = LocalDateTime.now();
        Coverage cached = coverageCache.get(parentTable);
        if (cached != null && cached.frequency() == frequency && cached.end() != null
                && cached.end().isAfter(now)) {
            return;
        }
        ensureCoverage(parentTable, frequency);
    }

    /**
     * 确保「当前周期 + 下一周期」分区存在
     *
     * @param parentTable 分区父表名（如 uc_login_log）
     * @param frequency   分区频率
     */
    public synchronized void ensureCoverage(String parentTable, PartitionFrequency frequency) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime periodStart = frequency.periodStart(now);
        // 目标：当前周期 + 下一周期
        LocalDateTime horizon = frequency.periodEnd(frequency.periodEnd(periodStart));
        // 上一个周期的起点：仅在库中尚无可用分区（全新表 / 覆盖已过期）时作为兜底起点
        LocalDateTime fallbackStart = frequency.periodStart(periodStart.minusDays(1));

        LocalDateTime cursor = coverageEnd(parentTable, frequency);
        if (cursor == null || cursor.isBefore(periodStart)) {
            cursor = fallbackStart;
        }

        int created = 0;
        while (cursor.isBefore(horizon) && created < MAX_PARTITIONS_PER_RUN) {
            LocalDateTime rangeEnd = frequency.periodEnd(cursor);
            if (!rangeEnd.isAfter(cursor)) {
                // 保险：极端情况下周期计算异常时至少推进一天，避免死循环
                rangeEnd = cursor.plusDays(1);
            }
            createPartition(parentTable, frequency, cursor, rangeEnd);
            cursor = rangeEnd;
            created++;
        }
        coverageCache.put(parentTable, new Coverage(cursor, frequency));
        if (created > 0) {
            log.info("日志分区已就绪：{}（{}，新建 {} 个，覆盖至 {}）",
                    parentTable, frequency.getLabel(), created, cursor);
        }
    }

    // ------------------------- 私有方法

    /** 父表 → 分区频率（来自规则配置） */
    private PartitionFrequency frequencyOf(String parentTable) {
        return TABLE_LOGIN.equals(parentTable)
                ? PartitionFrequency.of(configService.get().getLoginLogPartition())
                : PartitionFrequency.of(configService.get().getOperLogPartition());
    }

    /** 数据库现有分区的覆盖终点（无分区或读取失败时返回 null） */
    private LocalDateTime coverageEnd(String parentTable, PartitionFrequency frequency) {
        Coverage cached = coverageCache.get(parentTable);
        if (cached != null && cached.frequency() == frequency && cached.end() != null) {
            return cached.end();
        }
        try {
            List<String> bounds = jdbcTemplate.queryForList(
                    "SELECT pg_get_expr(c.relpartbound, c.oid) FROM pg_class c"
                            + " JOIN pg_inherits i ON i.inhrelid = c.oid"
                            + " JOIN pg_class p ON p.oid = i.inhparent"
                            + " WHERE p.relname = ?", String.class, parentTable);
            LocalDateTime max = null;
            for (String bound : bounds) {
                Matcher matcher = BOUND_PATTERN.matcher(bound == null ? "" : bound);
                if (!matcher.find()) {
                    continue;
                }
                LocalDateTime end = parseTimestamp(matcher.group(2));
                if (end != null && (max == null || end.isAfter(max))) {
                    max = end;
                }
            }
            return max;
        } catch (Exception e) {
            log.warn("读取日志表 {} 现有分区区间失败（将按当前周期续建）：{}", parentTable, e.getMessage());
            return null;
        }
    }

    /** 创建分区（含分区内索引；已存在则跳过） */
    private void createPartition(String parentTable, PartitionFrequency frequency,
                                 LocalDateTime rangeStart, LocalDateTime rangeEnd) {
        String partition = parentTable + "_" + frequency.suffix(rangeStart);
        try {
            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS " + partition
                    + " PARTITION OF " + parentTable
                    + " FOR VALUES FROM ('" + rangeStart.format(BOUND_FORMATTER) + "')"
                    + " TO ('" + rangeEnd.format(BOUND_FORMATTER) + "')");
            // 分区内索引：操作人 + 时间（各分区独立建立，避免依赖父表索引的传播能力）
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_" + partition + "_username ON "
                    + partition + " (username)");
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_" + partition + "_time ON "
                    + partition + " (create_time)");
            log.info("日志分区已创建：{}（{} ~ {}）", partition, rangeStart, rangeEnd);
        } catch (Exception e) {
            // 例如区间与已有分区重叠：跳过并告警，不影响日志写入
            log.warn("日志分区创建跳过：{}（{} ~ {}）：{}", partition, rangeStart, rangeEnd, e.getMessage());
        }
    }

    /** 解析系统目录返回的时间戳（兼容不带/带毫秒、带时区等格式） */
    private static LocalDateTime parseTimestamp(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String value = text.trim();
        try {
            return LocalDateTime.parse(value, BOUND_FORMATTER);
        } catch (DateTimeParseException ignored) {
            // 继续尝试其他格式
        }
        try {
            return LocalDateTime.parse(value.replace(' ', 'T'));
        } catch (DateTimeParseException ignored) {
            // 继续尝试其他格式
        }
        try {
            return OffsetDateTime.parse(value).toLocalDateTime();
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

}
