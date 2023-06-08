package com.rlynic.sharding.plugin;


import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.implementation.bind.annotation.Argument;
import net.bytebuddy.implementation.bind.annotation.FieldValue;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.This;
import org.apache.shardingsphere.driver.jdbc.core.connection.ShardingSphereConnection;
import org.apache.shardingsphere.driver.jdbc.core.datasource.metadata.ShardingSphereDatabaseMetaData;
import org.apache.shardingsphere.driver.jdbc.core.resultset.DatabaseMetaDataResultSet;
import org.apache.shardingsphere.infra.database.DefaultDatabase;
import org.apache.shardingsphere.infra.database.metadata.DataSourceMetaData;
import org.apache.shardingsphere.infra.executor.sql.execute.engine.ConnectionMode;
import org.apache.shardingsphere.infra.rule.ShardingSphereRule;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;

@Slf4j
public class ColumnsMethodInterceptor {

    /**
     * 进行方法拦截, 注意这里可以对所有修饰符的修饰的方法（包含private的方法）进行拦截
     *
     * param callable 原方法执行
     * @return 执行结果
     */
    @RuntimeType
    public static ResultSet getColumns(@This ShardingSphereDatabaseMetaData target, @FieldValue("connection") ShardingSphereConnection connection,
                                        @FieldValue("currentPhysicalConnection") Connection currentPhysicalConnection,
                                        @FieldValue("rules") Collection<ShardingSphereRule> rules,
                                        @FieldValue("currentPhysicalDataSourceName") String currentPhysicalDataSourceName,
                                        @FieldValue("currentDatabaseMetaData") DatabaseMetaData currentDatabaseMetaData,
                                        @Argument(0) final String catalog, @Argument(1) final String schemaPattern, @Argument(2) final String tableNamePattern,
                                        @Argument(3) final String columnNamePattern) throws SQLException {
        if (null == currentPhysicalDataSourceName) {
            currentPhysicalDataSourceName = connection.getConnectionManager().getRandomPhysicalDataSourceName();
        }
        if (null == currentPhysicalConnection) {
            // currentPhysicalConnection = connection.getConnectionManager().getRandomConnection();
            // fix: 修复多个物理主机组成分库时，随机的库名与随机的连接不一致导致的查询异常
            currentPhysicalConnection = connection.getConnectionManager().getConnections(currentPhysicalDataSourceName, 1, ConnectionMode.MEMORY_STRICTLY).get(0);
        }
        if (null == currentDatabaseMetaData) {
            currentDatabaseMetaData = currentPhysicalConnection.getMetaData();
        }
        // fix: 去除模糊查询表元数据，比如获取t_order,却连t_order_item都获取到了
        return createDatabaseMetaDataResultSet(
                currentDatabaseMetaData.getColumns(getActualCatalog(catalog, connection, currentPhysicalDataSourceName), getActualSchema(schemaPattern, connection, currentPhysicalDataSourceName), tableNamePattern, columnNamePattern), rules);
    }

    public static ResultSet createDatabaseMetaDataResultSet(final ResultSet resultSet, final Collection<ShardingSphereRule> rules) throws SQLException {
        return new DatabaseMetaDataResultSet(resultSet, rules);
    }

    public static String getActualCatalog(final String catalog, final ShardingSphereConnection connection, String currentPhysicalDataSourceName) {
        DataSourceMetaData metaData = connection.getContextManager()
                .getMetaDataContexts().getMetaData().getDatabase(connection.getDatabaseName()).getResourceMetaData().getDataSourceMetaData(currentPhysicalDataSourceName);
        return null != catalog && catalog.contains(DefaultDatabase.LOGIC_NAME) ? metaData.getCatalog() : catalog;
    }

    public static String getActualSchema(final String schema, final ShardingSphereConnection connection, String currentPhysicalDataSourceName) {
        DataSourceMetaData metaData = connection.getContextManager()
                .getMetaDataContexts().getMetaData().getDatabase(connection.getDatabaseName()).getResourceMetaData().getDataSourceMetaData(currentPhysicalDataSourceName);
        return null != schema && schema.contains(DefaultDatabase.LOGIC_NAME) ? metaData.getSchema() : schema;
    }
}
