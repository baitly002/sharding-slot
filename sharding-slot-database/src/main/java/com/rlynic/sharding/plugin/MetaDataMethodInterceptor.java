package com.rlynic.sharding.plugin;


import com.rlynic.sharding.plugin.metadata.ShardingSphereDatabaseMetaData;
import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.This;
import org.apache.shardingsphere.driver.jdbc.core.connection.ShardingSphereConnection;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;

@Slf4j
public class MetaDataMethodInterceptor {

    /**
     * 进行方法拦截, 注意这里可以对所有修饰符的修饰的方法（包含private的方法）进行拦截
     *
     * param callable 原方法执行
     * @return 执行结果
     */
    @RuntimeType
    public static DatabaseMetaData getMetaData(@This ShardingSphereConnection target) throws SQLException {
        return new ShardingSphereDatabaseMetaData(target);
    }
}
