package com.rlynic.sharding.plugin;

import net.bytebuddy.asm.Advice;
import org.apache.shardingsphere.driver.jdbc.core.connection.ShardingSphereConnection;
import org.apache.shardingsphere.driver.jdbc.core.datasource.metadata.ShardingSphereDatabaseMetaData;
import org.apache.shardingsphere.infra.executor.sql.execute.engine.ConnectionMode;

import java.sql.Connection;
import java.sql.SQLException;

public class AdviceGetConnection {

    @Advice.OnMethodEnter
    public static Connection getConnection(@Advice.This ShardingSphereDatabaseMetaData target, @Advice.FieldValue("connection") ShardingSphereConnection connection,
                                           @Advice.FieldValue("currentPhysicalConnection") Connection currentPhysicalConnection, @Advice.FieldValue("currentPhysicalDataSourceName") String currentPhysicalDataSourceName) throws SQLException {
        if (null == currentPhysicalDataSourceName) {
            currentPhysicalDataSourceName = connection.getConnectionManager().getRandomPhysicalDataSourceName();
        }
        if (null == currentPhysicalConnection) {
            // currentPhysicalConnection = connection.getConnectionManager().getRandomConnection();
            // fix: 修复多个物理主机组成分库时，随机的库名与随机的连接不一致导致的查询异常
            currentPhysicalConnection = connection.getConnectionManager().getConnections(currentPhysicalDataSourceName, 1, ConnectionMode.MEMORY_STRICTLY).get(0);
        }
        return currentPhysicalConnection;
    }
}
