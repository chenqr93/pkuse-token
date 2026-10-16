package com.bdsoft.usercenter.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.bdsoft.pkusetoken.stp.StpUtil;
import com.bdsoft.usercenter.log.support.OperChangeCaptureInterceptor;
import org.apache.ibatis.mapping.DatabaseIdProvider;
import org.apache.ibatis.mapping.VendorDatabaseIdProvider;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;
import java.util.Properties;

/**
 * MyBatis-Plus 配置：注册 KingbaseES 分页插件 + 操作日志数据变更捕获插件 + 多数据库方言路由
 *
 * <p> 自定义 SQL 的 XML 与 Mapper 接口同模块存放（usercenter-service/src/main/resources/mapper/）， </p>
 * <p> 多数据库（KingbaseES / 达梦 DM / MySQL）兼容采用 MyBatis 官方 databaseId 机制： </p>
 * <ul>
 *   <li>XML 按方言分目录（mapper/kingbase/、mapper/dm/、mapper/mysql/），或在同一文件内用
 *       {@code databaseId} 属性区分；不带 databaseId 的语句作为默认实现；</li>
 *   <li>{@link VendorDatabaseIdProvider} 按 JDBC 产品名自动解析当前库的方言标识
 *       （KingbaseES→kingbase、DM→dm、MySQL→mysql），运行期无需任何手工配置；</li>
 *   <li>XML 扫描路径见 application.yml 的 mybatis-plus.mapper-locations（classpath*: 通配 mapper 下任意层级）。 </li>
 * </ul>
 */
@Configuration
public class MybatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 数据变更捕获：@OperLog 方法执行期间自动记录 INSERT/UPDATE/DELETE 的修改前/修改后数据
        interceptor.addInnerInterceptor(new OperChangeCaptureInterceptor());
        // 分页插件：人大金仓 KingbaseES 方言（跨库部署时按目标库调整，如 DbType.MYSQL / DbType.ORACLE）
        PaginationInnerInterceptor pagination = new PaginationInnerInterceptor(DbType.KINGBASE_ES);
        // 单页最大条数限制
        pagination.setMaxLimit(1000L);
        interceptor.addInnerInterceptor(pagination);
        return interceptor;
    }

    /**
     * 审计字段自动填充（AbstractAuditPO）：
     * INSERT 填 createBy / createTime，UPDATE 填 updateBy / updateTime；
     * 操作人取当前登录用户 ID（String 主键；启动初始化等未登录场景为 null）。
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
                    // 无会话上下文的场景（如启动时规则配置初始化写库）：操作人留空
                    return null;
                }
            }
        };
    }

    /**
     * 数据库厂商标识：为 XML 语句的 databaseId 属性提供当前库的方言名
     *
     * <p> key 为 DatabaseMetaData.getDatabaseProductName()（不区分大小写、支持部分匹配），value 为方言标识。 </p>
     */
    @Bean
    public DatabaseIdProvider databaseIdProvider() {
        VendorDatabaseIdProvider provider = new VendorDatabaseIdProvider();
        Properties properties = new Properties();
        // 人大金仓 KingbaseES（部分版本驱动报告为 PostgreSQL 兼容模式）
        properties.setProperty("KingbaseES", "kingbase");
        properties.setProperty("PostgreSQL", "kingbase");
        // 达梦
        properties.setProperty("DM DBMS", "dm");
        // MySQL
        properties.setProperty("MySQL", "mysql");
        provider.setProperties(properties);
        return provider;
    }

}
