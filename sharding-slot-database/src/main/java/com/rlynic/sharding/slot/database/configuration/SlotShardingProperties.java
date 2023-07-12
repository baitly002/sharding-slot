/*
  This file created at 2020/6/24.

  Copyright (c) 2002-2020 crisis, Inc. All rights reserved.
 */
package com.rlynic.sharding.slot.database.configuration;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

import java.io.Serializable;
import java.util.Map;

/**
 * <code>{@link SlotShardingProperties}</code>
 *
 * @author crisis
 */
@Data
@NoArgsConstructor
@ConfigurationProperties(prefix="slot.sharding")
public class SlotShardingProperties implements Serializable {

    private String column="slot";
    private int number=16384;
    @NestedConfigurationProperty
    private Range range;
//    private List<String> datasourceNames;
//    private List<String> tableNames;
    //分库的数据库个数
    private Integer size;
    //强制按新分库大小（当分库大小新旧不一致时）
    private Boolean forceSharding;
    //分库的数据库名后缀从0开始还是从1开始
    private Integer dbStartIndex = 0;
    //分库的数据库逻辑名称前缀，用于slot-range及shardingsphere的配置,以下示例的[pan-1],[pan-$->{1..${slot.sharding.size}}.pan_file]
    //slot.sharding.range.datasource.pan-1={0, 4095}
    //spring.shardingsphere.rules.sharding.tables.pan_file.actual-data-nodes=pan-$->{1..${slot.sharding.size}}.pan_file
    //spring.shardingsphere.datasource.pan-1.driver-class-name=com.mysql.jdbc.Driver
    private String logicDatasourcePrefix = "pan";
    //表分库规则,多个用逗号隔开。格式：[表名称.分库字段:分库算法]冒号后可接分库算法，不指定则采用默认,  eg:pan_file.file_id 或 pan_file.file_id:hash-slot 多个：pan_file.file_id:hash-slot, pan_dir.dir_id:mod
    private String tableRules;
    //jdbc-url扩展参数
    private String urlArgs;
    //是否打印分库后的sql
    private String sqlShow;
    //sharding jdbc 的驱动
    private String driverClassName = "org.apache.shardingsphere.driver.ShardingSphereDriver";
    //sharding jdbc 的配置文件url
    private String jdbcUrl = "jdbc:shardingsphere:spring:auto";

    @Data
    @NoArgsConstructor
    public static class Range implements Serializable{
        private Map<String, String> datasource;
        private Map<String, String> table;
    }

}