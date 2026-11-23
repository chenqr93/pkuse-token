package com.bdsoft.usercenter.log.support;

import org.springframework.jdbc.datasource.DataSourceUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 操作日志 - 数据变更收集器（请求级）
 *
 * <p> {@link OperLogAspect} 在标注 {@code @OperLog} 的方法执行前开启收集，方法结束后结束收集； </p>
 * <p> 执行期间，{@link OperChangeCaptureInterceptor}（MyBatis-Plus 内置拦截器体系）会把该方法内 </p>
 * <p> <b>所有</b> 落库的 INSERT/UPDATE/DELETE（无论涉及多少张表、多少个实体）自动登记到这里： </p>
 * <ul>
 *   <li>UPDATE / DELETE：登记「修改前数据」（SQL 执行前按同一 WHERE 查询）；</li>
 *   <li>UPDATE：另登记一条「修改后数据」延迟查询（方法结束后按同一 WHERE 重查）；</li>
 *   <li>INSERT：从实体参数提取「修改后数据」（含生成的主键）。</li>
 * </ul>
 * <p> 未开启收集时（如登录、SSO 流程）拦截器直接跳过，零开销。 </p>
 */
public final class OperRecordCollector {

    /** 单条语句最多记录的行数（防止全表变更撑爆日志） */
    static final int MAX_ROWS_PER_STATEMENT = 50;

    /** 单次操作最多记录的语句数 */
    private static final int MAX_CHANGES_PER_OPERATION = 20;

    private static final ThreadLocal<Ctx> CTX = new ThreadLocal<>();

    private OperRecordCollector() {
    }

    /** 一条 SQL 语句造成的数据变更 */
    public static class Change {
        final String table;
        final String operation;
        List<Map<String, Object>> beforeRows;
        List<Map<String, Object>> afterRows;

        /** UPDATE 的「修改后数据」延迟查询（方法结束后按同一 WHERE 重查） */
        OperChangeCaptureInterceptor.SelectSpec afterSelect;

        Change(String table, String operation) {
            this.table = table;
            this.operation = operation;
        }
    }

    /** 一次操作收集到的全部变更（before/after 已按表分组） */
    public record Result(Object before, Object after) {
        static final Result EMPTY = new Result(null, null);
    }

    private static final class Ctx {
        final List<Change> changes = new ArrayList<>();
    }

    /** 开启收集（由切面在方法执行前调用） */
    public static void begin() {
        CTX.set(new Ctx());
    }

    /** 是否正在收集（拦截器据此判断是否需要登记变更） */
    public static boolean isActive() {
        return CTX.get() != null;
    }

    /** 登记一条变更（由拦截器调用） */
    static void collect(Change change) {
        Ctx ctx = CTX.get();
        if (ctx == null || ctx.changes.size() >= MAX_CHANGES_PER_OPERATION) {
            return;
        }
        ctx.changes.add(change);
    }

    /**
     * 结束收集：对 UPDATE 补查「修改后数据」（同事务连接，能看到本操作写入的新值），并清空 ThreadLocal
     *
     * @return before/after 分组结果（无变更时两个方向均为 null）
     */
    public static Result finish(DataSource dataSource) {
        Ctx ctx = CTX.get();
        CTX.remove();
        if (ctx == null || ctx.changes.isEmpty()) {
            return Result.EMPTY;
        }
        List<Change> changes = ctx.changes;
        boolean needAfter = changes.stream().anyMatch(c -> c.afterSelect != null);
        if (needAfter && dataSource != null) {
            Connection connection = DataSourceUtils.getConnection(dataSource);
            try {
                for (Change change : changes) {
                    if (change.afterSelect != null) {
                        change.afterRows = OperChangeCaptureInterceptor.runSelect(change.afterSelect, connection);
                    }
                }
            } finally {
                DataSourceUtils.releaseConnection(connection, dataSource);
            }
        }
        return new Result(group(changes, false), group(changes, true));
    }

    /**
     * 按「表 + 操作」分组序列化
     *
     * @param after true=取修改后数据，false=取修改前数据
     * @return 形如 {@code [{"table":"uc_user","operation":"update","rows":[{...}]}]}
     */
    private static List<Map<String, Object>> group(List<Change> changes, boolean after) {
        List<Map<String, Object>> blocks = new ArrayList<>();
        for (Change change : changes) {
            List<Map<String, Object>> rows = after ? change.afterRows : change.beforeRows;
            if (rows == null || rows.isEmpty()) {
                continue;
            }
            Map<String, Object> block = new LinkedHashMap<>();
            block.put("table", change.table);
            block.put("operation", change.operation);
            block.put("rows", rows);
            blocks.add(block);
        }
        return blocks;
    }

}
