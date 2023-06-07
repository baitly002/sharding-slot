package com.rlynic.sharding.plugin;


import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.implementation.bind.annotation.FieldValue;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.This;
import org.apache.shardingsphere.driver.jdbc.core.connection.ShardingSphereConnection;
import org.apache.shardingsphere.driver.jdbc.core.datasource.metadata.ShardingSphereDatabaseMetaData;
import org.apache.shardingsphere.infra.executor.sql.execute.engine.ConnectionMode;

import java.sql.Connection;
import java.sql.SQLException;

@Slf4j
public class ConnectionMethodInterceptor {

    /**
     * 进行方法拦截, 注意这里可以对所有修饰符的修饰的方法（包含private的方法）进行拦截
     *
     * param callable 原方法执行
     * @return 执行结果
     */

    @RuntimeType
    public static Connection getConnection(@This ShardingSphereDatabaseMetaData target, @FieldValue("connection") ShardingSphereConnection connection,
                                           @FieldValue("currentPhysicalConnection") Connection currentPhysicalConnection, @FieldValue("currentPhysicalDataSourceName") String currentPhysicalDataSourceName) throws SQLException {
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
