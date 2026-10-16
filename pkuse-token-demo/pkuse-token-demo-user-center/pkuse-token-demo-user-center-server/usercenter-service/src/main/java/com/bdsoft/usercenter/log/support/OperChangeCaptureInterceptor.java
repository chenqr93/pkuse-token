package com.bdsoft.usercenter.log.support;

import com.baomidou.mybatisplus.core.metadata.TableFieldInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.toolkit.PluginUtils;
import com.baomidou.mybatisplus.extension.parser.JsqlParserGlobal;
import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.JdbcParameter;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.select.AllColumns;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.SelectItem;
import net.sf.jsqlparser.statement.update.Update;
import net.sf.jsqlparser.statement.update.UpdateSet;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.apache.ibatis.scripting.defaults.DefaultParameterHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Clob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 操作日志 - 数据变更捕获拦截器（MyBatis-Plus InnerInterceptor）
 *
 * <p> 借鉴 MyBatis-Plus {@code DataChangeRecorderInnerInterceptor} 的实现思路，但输出结构化的 </p>
 * <p> 「修改前 / 修改后」行数据（而非旧值->新值 diff 字符串），并只在 {@code @OperLog} 方法执行期间 </p>
 * <p>（{@link OperRecordCollector#isActive()}）工作，登录等其余链路零开销： </p>
 * <ul>
 *   <li>UPDATE：把原语句的 WHERE 改写为 {@code SELECT * FROM 表 WHERE ...}，用 MyBatis 自己的
 *       {@link DefaultParameterHandler} 绑定 WHERE 部分参数（过滤掉 SET 部分）在执行前查出<b>修改前数据</b>，
 *       并把同一查询交给收集器在方法结束后重查，得到<b>修改后数据</b>；</li>
 *   <li>DELETE：同上查询<b>删除前数据</b>；</li>
 *   <li>INSERT：从实体参数（TableInfo 元数据）提取<b>写入数据</b>（含生成的主键）。</li>
 * </ul>
 * <p> 一个方法内更新多张表 / 多个实体会逐条登记，全部进入同一条操作日志。 </p>
 */
public class OperChangeCaptureInterceptor implements InnerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(OperChangeCaptureInterceptor.class);

    @Override
    public void beforePrepare(StatementHandler statementHandler, Connection connection, Integer transactionTimeout) {
        if (!OperRecordCollector.isActive()) {
            return;
        }
        PluginUtils.MPStatementHandler mpHandler = PluginUtils.mpStatementHandler(statementHandler);
        MappedStatement mappedStatement = mpHandler.mappedStatement();
        SqlCommandType commandType = mappedStatement.getSqlCommandType();
        if (commandType != SqlCommandType.INSERT && commandType != SqlCommandType.UPDATE
                && commandType != SqlCommandType.DELETE) {
            return;
        }
        BoundSql boundSql = mpHandler.boundSql();
        try {
            String sql = boundSql.getSql().trim();
            if (commandType == SqlCommandType.INSERT) {
                captureInsert(mappedStatement, boundSql);
            } else {
                net.sf.jsqlparser.statement.Statement statement = JsqlParserGlobal.parse(sql);
                if (commandType == SqlCommandType.UPDATE && statement instanceof Update update) {
                    captureUpdate(mappedStatement, boundSql, update, connection);
                } else if (commandType == SqlCommandType.DELETE && statement instanceof Delete delete) {
                    captureDelete(mappedStatement, boundSql, delete, connection);
                }
            }
        } catch (Exception e) {
            // 捕获失败只影响前后数据快照，不影响业务与日志主记录
            log.warn("操作日志数据变更捕获跳过：mappedStatement={}，{}", mappedStatement.getId(), e.getMessage());
        }
    }

    // ------------------------- UPDATE：执行前查修改前数据，方法结束后重查修改后数据

    private void captureUpdate(MappedStatement mappedStatement, BoundSql boundSql, Update updateStmt,
                               Connection connection) {
        Expression where = updateStmt.getWhere();
        if (where == null) {
            // 无 WHERE 的全表更新不放行记录（避免超大快照）
            return;
        }
        String tableName = updateStmt.getTable().getName();
        OperRecordCollector.Change change = new OperRecordCollector.Change(tableName, "update");

        // 修改前数据：WHERE 原样改写为 SELECT，仅保留 WHERE 部分的参数映射，立即执行
        PlainSelect select = new PlainSelect();
        select.setFromItem(new Table(tableName));
        select.setWhere(where);
        select.setSelectItems(List.of(new SelectItem<>(new AllColumns())));
        SelectSpec beforeSpec = buildSelectSpec(mappedStatement, boundSql, select, countSetJdbcParameters(updateStmt));
        change.beforeRows = runSelect(beforeSpec, connection);
        // 修改后数据：同一查询交给收集器，在方法结束后（更新已生效）重查
        change.afterSelect = beforeSpec;
        OperRecordCollector.collect(change);
    }

    // ------------------------- DELETE：查删除前数据

    private void captureDelete(MappedStatement mappedStatement, BoundSql boundSql, Delete deleteStmt,
                               Connection connection) {
        String tableName = deleteStmt.getTable().getName();
        OperRecordCollector.Change change = new OperRecordCollector.Change(tableName, "delete");
        PlainSelect select = new PlainSelect();
        select.setFromItem(new Table(tableName));
        select.setWhere(deleteStmt.getWhere());
        select.setSelectItems(List.of(new SelectItem<>(new AllColumns())));
        // DELETE 没有 SET 子句，全部参数映射都属于 WHERE
        SelectSpec beforeSpec = buildSelectSpec(mappedStatement, boundSql, select, 0);
        change.beforeRows = runSelect(beforeSpec, connection);
        OperRecordCollector.collect(change);
    }

    // ------------------------- INSERT：从实体参数提取写入数据（含生成的主键）

    private void captureInsert(MappedStatement mappedStatement, BoundSql boundSql) {
        Object entity = boundSql.getParameterObject();
        TableInfo tableInfo = entity == null ? null : TableInfoHelper.getTableInfo(entity.getClass());
        if (tableInfo == null) {
            return;
        }
        Map<String, Object> row = new LinkedHashMap<>();
        MetaObject metaObject = SystemMetaObject.forObject(entity);
        if (tableInfo.getKeyColumn() != null && metaObject.hasGetter(tableInfo.getKeyProperty())) {
            row.put(tableInfo.getKeyColumn(), metaObject.getValue(tableInfo.getKeyProperty()));
        }
        for (TableFieldInfo field : tableInfo.getFieldList()) {
            if (metaObject.hasGetter(field.getProperty())) {
                row.put(field.getColumn(), metaObject.getValue(field.getProperty()));
            }
        }
        OperRecordCollector.Change change = new OperRecordCollector.Change(tableInfo.getTableName(), "insert");
        change.afterRows = List.of(row);
        OperRecordCollector.collect(change);
    }

    // ------------------------- SELECT 构造与执行（借鉴 MyBatis-Plus 官方插件的做法）

    /**
     * 构造「查询受影响行」的 SELECT
     *
     * <p> 关键点：新 BoundSql 复用原语句的 parameterObject，但 parameterMappings 只保留 WHERE 部分
     * （UPDATE 需剔除 SET 子句的参数个数），再交给 MyBatis 的 DefaultParameterHandler 完成参数绑定，
     * 从而无需手工格式化各种类型的参数值。 </p>
     */
    private SelectSpec buildSelectSpec(MappedStatement mappedStatement, BoundSql boundSql,
                                       PlainSelect select, int setParamCount) {
        List<ParameterMapping> parameterMappings = boundSql.getParameterMappings();
        List<ParameterMapping> whereMappings = parameterMappings.isEmpty()
                ? parameterMappings
                : parameterMappings.subList(setParamCount, parameterMappings.size());
        BoundSql selectBoundSql = new BoundSql(mappedStatement.getConfiguration(), select.toString(),
                whereMappings, boundSql.getParameterObject());
        Map<String, Object> additionalParameters = PluginUtils.mpBoundSql(boundSql).additionalParameters();
        if (additionalParameters != null && !additionalParameters.isEmpty()) {
            additionalParameters.forEach(selectBoundSql::setAdditionalParameter);
        }
        return new SelectSpec(select.toString(), mappedStatement, selectBoundSql);
    }

    /**
     * 执行受影响行查询
     *
     * @param connection 为 null 时仅登记、由收集器稍后（拿到连接后）执行
     */
    static List<Map<String, Object>> runSelect(SelectSpec spec, Connection connection) {
        if (connection == null) {
            return null;
        }
        try (PreparedStatement statement = connection.prepareStatement(spec.sql())) {
            DefaultParameterHandler parameterHandler = new DefaultParameterHandler(
                    spec.mappedStatement(), spec.boundSql().getParameterObject(), spec.boundSql());
            parameterHandler.setParameters(statement);
            try (ResultSet resultSet = statement.executeQuery()) {
                ResultSetMetaData metaData = resultSet.getMetaData();
                int columnCount = metaData.getColumnCount();
                List<Map<String, Object>> rows = new ArrayList<>();
                while (resultSet.next() && rows.size() < OperRecordCollector.MAX_ROWS_PER_STATEMENT) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int i = 1; i <= columnCount; i++) {
                        Object value = resultSet.getObject(i);
                        if (value instanceof Clob clob) {
                            value = clob.getSubString(1, (int) clob.length());
                        }
                        row.put(metaData.getColumnLabel(i), value);
                    }
                    rows.add(row);
                }
                return rows;
            }
        } catch (Exception e) {
            log.warn("操作日志变更数据查询失败：{}，{}", spec.sql(), e.getMessage());
            return List.of();
        }
    }

    /** UPDATE 语句 SET 子句中的 JDBC 参数个数（这些参数不属于 WHERE） */
    private static int countSetJdbcParameters(Update updateStmt) {
        int count = 0;
        for (UpdateSet updateSet : updateStmt.getUpdateSets()) {
            for (Expression expression : updateSet.getValues()) {
                if (expression instanceof JdbcParameter) {
                    count++;
                }
            }
        }
        return count;
    }

    /** 受影响行查询（SQL + 参数绑定所需信息） */
    record SelectSpec(String sql, MappedStatement mappedStatement, BoundSql boundSql) {
    }

}
