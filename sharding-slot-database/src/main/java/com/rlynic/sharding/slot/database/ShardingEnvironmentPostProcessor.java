/*
  This file created at 2020/6/21.

  Copyright (c) 2002-2020 crisis, Inc. All rights reserved.
 */
package com.rlynic.sharding.slot.database;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.boot.env.PropertiesPropertySourceLoader;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * <code>{@link ShardingEnvironmentPostProcessor}</code>
 *
 * @author crisis
 */
public class ShardingEnvironmentPostProcessor implements EnvironmentPostProcessor {
    private final static Logger log = LoggerFactory.getLogger(ShardingEnvironmentPostProcessor.class);

    private ResourceLoader resourceLoader = new DefaultResourceLoader();
    private PropertiesPropertySourceLoader propertySourcesLoader = new PropertiesPropertySourceLoader();
    private static ConfigurableEnvironment environment = null;

    private String[] propertiesLocations = {
            "classpath:/META-INF/sharding-constants.properties"
    };

    public static String resoveConfig(String text){
        if(environment != null){
            return environment.resolvePlaceholders(text);
        }else{
            return text;
        }
    }

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        try{
            for(String l : propertiesLocations){
                Resource r = resourceLoader.getResource(l);
                if(null != r && r.exists()){
                    List<PropertySource<?>> propertySource = propertySourcesLoader.load("sharding config: [profile=default]", r);
                    propertySource.forEach(p -> environment.getPropertySources().addLast(p));
                }
            }
            ShardingEnvironmentPostProcessor.environment = environment;
            Properties properties = creatShardingConfig(environment);
            PropertiesPropertySource pps = new PropertiesPropertySource("sharding-jdbc.properties", properties);
            environment.getPropertySources().addLast(pps);
        }catch(IOException e){
            log.error("the sharding config failed to load", e);
        }
    }

    public Properties creatShardingConfig(ConfigurableEnvironment environment){
        Properties properties = new Properties();
        Integer size = environment.getProperty("slot.sharding.size", Integer.class);
        if(size == null){
            log.error("The system must be configured [slot.sharding.size] value");
        }
        if(!is2pow(size)){
            log.warn("配置 [slot.sharding.size] 的值最好是2的n次方！");
        }
        String logicDatasourcePrefix = environment.getProperty("slot.sharding.logic-datasource-prefix", "pan");
        Integer dbStartIndex = environment.getProperty("slot.sharding.db-start-index", Integer.class, 0);
        Integer maxSlot = environment.getProperty("slot.sharding.number", Integer.class, 16384);
        if(!environment.containsProperty("slot.sharding.range.datasource."+logicDatasourcePrefix+"-"+dbStartIndex)){
            //自动计算每个库的slot范围
            calSlotRange(maxSlot, size, logicDatasourcePrefix, dbStartIndex, properties, environment);
        }
        properties.setProperty("spring.shardingsphere.props.sql-show", "true");
        //table rules
        if(!environment.containsProperty("slot.sharding.table-rules")){
            log.error("找不到分库规则，请检查[slot.sharding.table-rules]配置是否正确！");
        }
        String ruleStr = environment.getProperty("slot.sharding.table-rules");
        List<String> tableNames = new ArrayList<>();
        for(String rule : ruleStr.split(",")){
            String[] trs = rule.split(":");
            String[] t = trs[0].trim().split("\\.");
            if(t.length<=1){
                log.error("配置[slot.sharding.table-rules]在内容"+trs[0]+"附近有格式错误，请检查是否符合格式[表名称.分库字段:分库算法]或[表名称.分库字段]");
            }
            String tableName = t[0];
            String columnName = t[1];
            String shardingAlgorithmName = environment.getProperty("slot.sharding.default-sharding-algorithm-name", "hash-slot");
            if(trs.length>1){
                shardingAlgorithmName = trs[1].trim();
            }
            properties.setProperty("spring.shardingsphere.rules.sharding.tables."+tableName+".actual-data-nodes", logicDatasourcePrefix+"-$->{"+dbStartIndex+".."+size+"}."+tableName);
            properties.setProperty("spring.shardingsphere.rules.sharding.tables."+tableName+".database-strategy.standard.sharding-column", columnName);
            properties.setProperty("spring.shardingsphere.rules.sharding.tables."+tableName+".database-strategy.standard.sharding-algorithm-name", shardingAlgorithmName);
            tableNames.add(tableName);
        }
        properties.setProperty("spring.shardingsphere.rules.sharding.binding-tables", String.join(",", tableNames));
        //db config
        String upUrl = null;
        String upUsername = null;
        String upPasswd = null;
        List<String> dbNames = new ArrayList<>();
        for(int i=0; i<size; i++){
            int index = i + dbStartIndex;
            properties.setProperty("spring.shardingsphere.datasource."+logicDatasourcePrefix+"-"+index+".driver-class-name", "com.mysql.cj.jdbc.Driver");
            properties.setProperty("spring.shardingsphere.datasource."+logicDatasourcePrefix+"-"+index+".type", "com.zaxxer.hikari.HikariDataSource");
            properties.setProperty("spring.shardingsphere.datasource."+logicDatasourcePrefix+"-"+index+".data-source-class-name", "com.zaxxer.hikari.HikariDataSource");

            String url = environment.getProperty("sharding.datasource.url."+logicDatasourcePrefix+"-"+index, upUrl==null?environment.getProperty("sharding.datasource.url"):upUrl);
            if(!StringUtils.hasLength(url)){
                log.error("分库后缀[index={}]的url没有配置，请检查！", index);
            }
            upUrl = url;
            String schemaPrefix = environment.getProperty("sharding.datasource.schemaPrefix."+logicDatasourcePrefix+"-"+index, environment.getProperty("sharding.datasource.schemaPrefix"));
            String args = environment.getProperty("slot.sharding.url-args", "allowPublicKeyRetrieval=true&serverTimezone=UTC&useSSL=false&useUnicode=true&characterEncoding=UTF-8&allowMultiQueries=true");
            String jdbcUrl = url+"/"+schemaPrefix+"_"+index+"?"+args;
            properties.setProperty("spring.shardingsphere.datasource."+logicDatasourcePrefix+"-"+index+".jdbc-url", jdbcUrl);
            String username = environment.getProperty("sharding.datasource.username."+logicDatasourcePrefix+"-"+index, upUsername==null?environment.getProperty("sharding.datasource.username"):upUsername);
            if(!StringUtils.hasLength(username)){
                log.error("分库后缀[index={}]的username没有配置，请检查！", index);
            }
            upUsername = username;
            properties.setProperty("spring.shardingsphere.datasource."+logicDatasourcePrefix+"-"+index+".username", username);
            String password = environment.getProperty("sharding.datasource.password."+logicDatasourcePrefix+"-"+index, upPasswd==null?environment.getProperty("sharding.datasource.password"):upPasswd);
            if(!StringUtils.hasLength(password)){
                log.error("分库后缀[index={}]的password没有配置，请检查！", index);
            }
            upPasswd = password;
            properties.setProperty("spring.shardingsphere.datasource."+logicDatasourcePrefix+"-"+index+".password", password);
            properties.setProperty("spring.shardingsphere.datasource."+logicDatasourcePrefix+"-"+index+".maximum-pool-size", environment.resolvePlaceholders("${sharding.datasource.maximum-pool-size:100}"));
            properties.setProperty("spring.shardingsphere.datasource."+logicDatasourcePrefix+"-"+index+".minimum-idle", environment.resolvePlaceholders("${sharding.datasource.minimum-idle:50}"));
            dbNames.add(logicDatasourcePrefix+"-"+index);
        }
        properties.setProperty("spring.shardingsphere.datasource.names", String.join(",", dbNames));

        return properties;
    }

    //判断是否属于2的n次方
    public static boolean is2pow(Integer n){
        return n!=null && n > 0 && (n & (n-1)) == 0;
    }

    //计算每个库的slot范围
    public static void calSlotRange(int maxSlot, int shardingSize, String logicDatasourcePrefix, Integer dbStartIndex,
                                    Properties properties, ConfigurableEnvironment environment){
        int val = maxSlot / shardingSize;
        int rangIndex = 0;
        for(int i=0; i<shardingSize; i++){
            int from = rangIndex;
            int to = 0;
            if(i == (shardingSize-1)){
                to = maxSlot-1;
            }else{
                to = rangIndex+val-1;
            }
            rangIndex += val;
            System.out.println(i + "   " + from + "->" + to);
            properties.setProperty("slot.sharding.range.datasource."+logicDatasourcePrefix+"-", "{"+from+", "+to+"}");
        }
    }

    public static void main(String[] args) {

    }
}
