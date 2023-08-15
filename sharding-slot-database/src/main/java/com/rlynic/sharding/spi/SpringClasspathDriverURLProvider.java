package com.rlynic.sharding.spi;

import com.google.common.base.Preconditions;
import com.rlynic.sharding.slot.database.ShardingEnvironmentPostProcessor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.shardingsphere.driver.jdbc.core.driver.ShardingSphereDriverURLProvider;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.StandardEnvironment;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

@Slf4j
public class SpringClasspathDriverURLProvider implements ShardingSphereDriverURLProvider {

    private static final String CLASSPATH_TYPE = "spring:";

    @Override
    public boolean accept(final String url) {
        return StringUtils.isNotBlank(url) && url.contains(CLASSPATH_TYPE);
    }

    @Override
    @SneakyThrows(IOException.class)
    public byte[] getContent(final String url) {
        String configuredFile = url.substring("jdbc:shardingsphere:".length(), url.contains("?") ? url.indexOf("?") : url.length());
        String file = configuredFile.substring(CLASSPATH_TYPE.length());
        Preconditions.checkArgument(!file.isEmpty(), "Configuration file is required in ShardingSphere driver URL.");
        if("auto".equalsIgnoreCase(file)){
            return autoConfig(ShardingEnvironmentPostProcessor.getEnvironment()).getBytes(StandardCharsets.UTF_8);
        }
        try (InputStream stream = getResourceAsStream(file)) {
            Objects.requireNonNull(stream, String.format("Can not find configuration file `%s`.", file));
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            String line;
            while (null != (line = reader.readLine())) {
                if (!line.startsWith("#")) {
                    builder.append(line).append('\n');
                }
            }
            String resolveContent = ShardingEnvironmentPostProcessor.resoveConfig(builder.toString());
            log.info("sharding.yml : {}", resolveContent);
            return resolveContent.getBytes(StandardCharsets.UTF_8);
        }
    }

    private InputStream getResourceAsStream(final String resource) {
        ClassLoader[] classLoaders = new ClassLoader[]{
                Thread.currentThread().getContextClassLoader(), getClass().getClassLoader(), ClassLoader.getSystemClassLoader(),
        };
        for (ClassLoader each : classLoaders) {
            if (null != each) {
                InputStream result = each.getResourceAsStream(resource);
                if (null == result) {
                    result = each.getResourceAsStream("/" + resource);
                }
                if (null != result) {
                    return result;
                }
            }
        }
        return null;
    }

    private static String autoConfig(ConfigurableEnvironment environment){
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
//        Integer maxSlot = environment.getProperty("slot.sharding.number", Integer.class, 16384);
//        if(!environment.containsProperty("slot.sharding.range.datasource."+logicDatasourcePrefix+"-"+dbStartIndex)){
//            //自动计算每个库的slot范围
//            calSlotRange(maxSlot, size, logicDatasourcePrefix, dbStartIndex, properties, environment);
//        }
//        PropertiesPropertySource pps = new PropertiesPropertySource("slot-auto.properties", properties);
//        environment.getPropertySources().addLast(pps);

        StringBuilder builder = new StringBuilder();

        builder.append("""
                mode:
                  type: Standalone
                  repository:
                    type: JDBC
                props:
                  sql-show: ${slot.sharding.sqlShow:true}
                dataSources:
                """)
                .append(dbConfig(logicDatasourcePrefix, dbStartIndex, size, environment))
                .append(tableRules(logicDatasourcePrefix, dbStartIndex, size, environment));
        String result = environment.resolvePlaceholders(builder.toString());
        log.info("sharding.yml : {}", result);
        return result;
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
            String k = "slot.sharding.range.datasource."+logicDatasourcePrefix+"-"+(i+dbStartIndex);
            if(!environment.containsProperty(k)) {
                String v = "{" + from + ", " + to + "}";
                properties.setProperty(k, v);
                log.info("slot config: {}={}", k, v);
            }
        }
    }

    //处理table rules
    public static String tableRules(String logicDatasourcePrefix, int dbStartIndex, int size, ConfigurableEnvironment environment){
        //table rules
        if(!environment.containsProperty("slot.sharding.table-rules")){
//            log.error("找不到分库规则，请检查[slot.sharding.table-rules]配置是否正确！");
            throw new NullPointerException("找不到分库规则，请检查[slot.sharding.table-rules]配置是否正确！");
        }
        String ruleStr = environment.getProperty("slot.sharding.table-rules");
        List<String> tableNames = new ArrayList<>();
        StringBuilder rules = new StringBuilder();
        rules.append("""
                rules:
                  - !SHARDING
                    shardingAlgorithms:
                      hash-slot:
                        type: HASH_SLOT
                      hash-mod:
                        type: HASH_MOD
                        props:
                          sharding-count: ${slot.sharding.size}
                    tables:
                """);
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
            tableNames.add(tableName);
            rules.append("      ").append(tableName).append(": ").append(newLine());
            rules.append("        ").append("actualDataNodes: ").append(logicDatasourcePrefix).append("-$->{").append(dbStartIndex)
                    .append("..").append(size+(dbStartIndex-1)).append("}.").append(tableName).append(newLine());
            rules.append("        ").append("databaseStrategy:").append(newLine());
            rules.append("          ").append("standard:").append(newLine());
            rules.append("            ").append("shardingAlgorithmName: ").append(shardingAlgorithmName).append(newLine());
            rules.append("            ").append("shardingColumn: ").append(columnName).append(newLine());
        }
        return rules.toString();
    }

    public static String dbConfig(String logicDatasourcePrefix, int dbStartIndex, int size, ConfigurableEnvironment environment){
        StringBuilder dbConfig = new StringBuilder();
        String upUrl = null;
        String upschemaPrefix = null;
        String upUsername = null;
        String upPasswd = null;
        List<String> dbNames = new ArrayList<>();
        for(int i=0; i<size; i++){
            int index = i + dbStartIndex;
            dbConfig.append("  ").append(logicDatasourcePrefix).append("-").append(index).append(": ").append(newLine());
            dbConfig.append("    ").append("dataSourceClassName: com.zaxxer.hikari.HikariDataSource").append(newLine());
            dbConfig.append("    ").append("type: com.zaxxer.hikari.HikariDataSource").append(newLine());
            dbConfig.append("    ").append("driverClassName: com.mysql.cj.jdbc.Driver").append(newLine());
//            dbConfig.append("    ").append("jdbcUrl: ${sharding.datasource.url}/${sharding.datasource.schemaPrefix}_0?characterEncoding=utf8&zeroDateTimeBehavior=convertToNull&useSSL=false&allowMultiQueries=true&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=Asia/Shanghai&nullCatalogMeansCurrent=true&allowPublicKeyRetrieval=true").append(newLine());


            String url = environment.getProperty("sharding.datasource.url"+index, upUrl==null?environment.getProperty("sharding.datasource.url"):upUrl);
            if(!org.springframework.util.StringUtils.hasLength(url)){
                log.error("分库后缀[index={}]的url没有配置，请检查！", index);
            }
            upUrl = url;
            String schemaPrefix = environment.getProperty("sharding.datasource.schemaPrefix"+index, upschemaPrefix==null?environment.getProperty("sharding.datasource.schemaPrefix"):upschemaPrefix);
            upschemaPrefix = schemaPrefix;
            String args = environment.getProperty("slot.sharding.url-args", "allowPublicKeyRetrieval=true&serverTimezone=UTC&useSSL=false&useUnicode=true&characterEncoding=UTF-8&allowMultiQueries=true");
            String jdbcUrl = url+"/"+schemaPrefix+"_"+index+"?"+args;
            dbConfig.append("    ").append("jdbcUrl: ").append(jdbcUrl).append(newLine());
            String username = environment.getProperty("sharding.datasource.username"+index, upUsername==null?environment.getProperty("sharding.datasource.username"):upUsername);
            if(!org.springframework.util.StringUtils.hasLength(username)){
                log.error("分库后缀[index={}]的username没有配置，请检查！", index);
            }
            upUsername = username;
            dbConfig.append("    ").append("username: ").append(username).append(newLine());
            String password = environment.getProperty("sharding.datasource.password"+index, upPasswd==null?environment.getProperty("sharding.datasource.password"):upPasswd);
            if(!org.springframework.util.StringUtils.hasLength(password)){
                log.error("分库后缀[index={}]的password没有配置，请检查！", index);
            }
            upPasswd = password;
            dbConfig.append("    ").append("password: ").append(password).append(newLine());
            dbConfig.append("    ").append("maximum-pool-size: ").append(environment.resolvePlaceholders("${sharding.datasource.maximum-pool-size:100}")).append(newLine());
            dbConfig.append("    ").append("minimum-idle: ").append(environment.resolvePlaceholders("${sharding.datasource.minimum-idle:50}")).append(newLine());
            dbConfig.append("    ").append("maxPoolSize: ").append(environment.resolvePlaceholders("${sharding.datasource.maximum-pool-size:100}")).append(newLine());
            dbConfig.append("    ").append("minPoolSize: ").append(environment.resolvePlaceholders("${sharding.datasource.minimum-idle:50}")).append(newLine());
            dbNames.add(logicDatasourcePrefix+"-"+index);
        }
        return dbConfig.toString();
    }
    //判断是否属于2的n次方
    public static boolean is2pow(Integer n){
        return n!=null && n > 0 && (n & (n-1)) == 0;
    }

    static String newLine(){
        return "\n";
    }

    public static void main(String[] args) {
        //注释的是可有可无的，没注释的是必须的
        ConfigurableEnvironment environment = new StandardEnvironment();
        Properties p = new Properties();
        p.setProperty("slot.sharding.size", "8");
//        p.setProperty("slot.sharding.db-start-index", "3");
        p.setProperty("sharding.datasource.url", "jdbc:mysql:10.200.20.100:3306");
//        p.setProperty("sharding.datasource.url3", "jdbc:mysql:10.200.20.200:3306");
//        p.setProperty("sharding.datasource.url6", "jdbc:mysql:10.200.20.300:3306");
        p.setProperty("sharding.datasource.schemaPrefix", "pan_t");
//        p.setProperty("sharding.datasource.schemaPrefix3", "db_t");
//        p.setProperty("sharding.datasource.schemaPrefix6", "sharding_t");
        p.setProperty("sharding.datasource.username", "root");
//        p.setProperty("sharding.datasource.username4", "app");
        p.setProperty("sharding.datasource.password", "Bingo@123");
//        p.setProperty("sharding.datasource.password4", "admin@123");
        p.setProperty("slot.sharding.table-rules", "pan_dir.dir_id,pan_file.file_id,undo_log.branch_id:hash-mod");
        PropertiesPropertySource propertySource = new PropertiesPropertySource("init", p);
        environment.getPropertySources().addLast(propertySource);
        autoConfig(environment);
    }
}
