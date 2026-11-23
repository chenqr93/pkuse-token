package com.bdsoft.authcenter.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.bdsoft.authcenter.log.support.OperChangeCaptureInterceptor;
import com.bdsoft.pkusetoken.stp.StpUtil;
import org.apache.ibatis.mapping.DatabaseIdProvider;
import org.apache.ibatis.mapping.VendorDatabaseIdProvider;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;
import java.util.Properties;
import java.util.UUID;

/**
 * MyBatis-Plus 配置：注册 KingbaseES 分页插件 + 操作日志数据变更捕获插件 + 审计字段自动填充 + 多数据库方言路由
 */
@Configuration
public class MybatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 数据变更捕获：@OperLog 方法执行期间自动记录 INSERT/UPDATE/DELETE 的修改前/修改后数据
        interceptor.addInnerInterceptor(new OperChangeCaptureInterceptor());
        // 分页插件：人大金仓 KingbaseES 方言（跨库部署时按目标库调整）
        PaginationInnerInterceptor pagination = new PaginationInnerInterceptor(DbType.KINGBASE_ES);
        pagination.setMaxLimit(1000L);
        interceptor.addInnerInterceptor(pagination);
        return interceptor;
    }

    /**
     * 主键生成器：ASSIGN_UUID 统一生成 36 位标准 UUID（含连接号，如 550e8400-e29b-41d4-a716-446655440000），
     * 对应 db 脚本主键列 VARCHAR(36) 规范。
     *
     * <p> MP 默认 nextUUID 返回 32 位（无连接号），此处覆盖为标准 36 位格式；
     * nextId 保留雪花数值兜底（当前规范全部实体使用 String 主键，正常不会走到）。 </p>
     */
    @Bean
    public IdentifierGenerator identifierGenerator() {
        return new IdentifierGenerator() {

            @Override
            public Number nextId(Object entity) {
                return IdWorker.getId();
            }

            @Override
            public String nextUUID(Object entity) {
                return UUID.randomUUID().toString();
            }
        };
    }

    /**
     * 审计字段自动填充（AbstractAuditPO）：
     * INSERT 填 createBy / createTime，UPDATE 填 updateBy / updateTime；
     * 操作人取当前登录用户 ID（未登录场景为 null）。
     */
    @Bean
    public MetaObjectHandler metaObjectHandler() {
        return new MetaObjectHandler() {

            @Override
            public void insertFill(MetaObject metaObject) {
                strictInsertFill(metaObject, "createBy", String.class, currentUserId());
                strictInsertFill(metaObject, "createTime", LocalDateTime.class, LocalDateTime.now());
            }

            @Override
            public void updateFill(MetaObject metaObject) {
                strictUpdateFill(metaObject, "updateBy", String.class, currentUserId());
                strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
            }

            private String currentUserId() {
                try {
                    Object loginId = StpUtil.getLoginIdDefaultNull();
                    return loginId == null ? null : loginId.toString();
                } catch (Exception e) {
                    return null;
                }
            }
        };
    }

    /**
     * 数据库厂商标识：为 XML 语句的 databaseId 属性提供当前库的方言名
     */
    @Bean
    public DatabaseIdProvider databaseIdProvider() {
        VendorDatabaseIdProvider provider = new VendorDatabaseIdProvider();
        Properties properties = new Properties();
        properties.setProperty("KingbaseES", "kingbase");
        properties.setProperty("PostgreSQL", "kingbase");
        properties.setProperty("DM DBMS", "dm");
        properties.setProperty("MySQL", "mysql");
        provider.setProperties(properties);
        return provider;
    }
}
