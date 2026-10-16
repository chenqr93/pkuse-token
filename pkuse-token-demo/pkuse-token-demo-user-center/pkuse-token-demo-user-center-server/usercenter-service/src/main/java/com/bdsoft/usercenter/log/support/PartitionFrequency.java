package com.bdsoft.usercenter.log.support;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.WeekFields;

/**
 * 日志分区分区频率（可在「规则配置」中按日志表分别设置）
 *
 * <p> 分区名后缀以「区间起点」标识：按月 <code>yyyyMM</code>、按周 <code>yyyyww</code>（ISO 周）、 </p>
 * <p> 按天 <code>yyyyMMdd</code>；频率变更时不会与其他频率的分区重名。 </p>
 */
public enum PartitionFrequency {

    /** 按月分区：如 uc_login_log_202610 */
    MONTHLY("monthly", "按月"),

    /** 按周分区（ISO 周，周一为起点）：如 uc_login_log_202641 */
    WEEKLY("weekly", "按周"),

    /** 按天分区：如 uc_login_log_20261010 */
    DAILY("daily", "按天");

    private static final DateTimeFormatter SUFFIX_MONTH = DateTimeFormatter.ofPattern("yyyyMM");
    private static final DateTimeFormatter SUFFIX_DAY = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final String code;
    private final String label;

    PartitionFrequency(String code, String label) {
        this.code = code;
        this.label = label;
    }

    /** 由配置值解析（未知值回落到按月） */
    public static PartitionFrequency of(String code) {
        if (code != null) {
            for (PartitionFrequency frequency : values()) {
                if (frequency.code.equalsIgnoreCase(code.trim())) {
                    return frequency;
                }
            }
        }
        return MONTHLY;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    /** 所在周期的起点（按周为周一 00:00） */
    public LocalDateTime periodStart(LocalDateTime time) {
        LocalDate date = time.toLocalDate();
        return switch (this) {
            case MONTHLY -> date.withDayOfMonth(1).atStartOfDay();
            case WEEKLY -> date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).atStartOfDay();
            case DAILY -> date.atStartOfDay();
        };
    }

    /** 所在周期的终点（不含） */
    public LocalDateTime periodEnd(LocalDateTime time) {
        LocalDateTime start = periodStart(time);
        return switch (this) {
            case MONTHLY -> start.plusMonths(1);
            case WEEKLY -> start.plusWeeks(1);
            case DAILY -> start.plusDays(1);
        };
    }

    /** 分区名后缀（以区间起点标识） */
    public String suffix(LocalDateTime rangeStart) {
        LocalDate date = rangeStart.toLocalDate();
        return switch (this) {
            case MONTHLY -> date.format(SUFFIX_MONTH);
            case WEEKLY -> String.format("%d%02d",
                    date.get(WeekFields.ISO.weekBasedYear()), date.get(WeekFields.ISO.weekOfWeekBasedYear()));
            case DAILY -> date.format(SUFFIX_DAY);
        };
    }

}
