package com.rlynic.sharding.slot.example.config.sharding;

import com.google.common.base.Strings;
import io.seata.rm.datasource.DataSourceProxy;
import io.seata.spring.MySeataDataSourceAutoConfiguration;
import io.seata.spring.annotation.datasource.DataSourceProxyHolder;
import io.seata.spring.annotation.datasource.SeataAutoDataSourceProxyCreator;
import lombok.RequiredArgsConstructor;
import org.apache.shardingsphere.infra.config.RuleConfiguration;
import org.apache.shardingsphere.infra.config.mode.ModeConfiguration;
import org.apache.shardingsphere.infra.database.DefaultDatabase;
import org.apache.shardingsphere.infra.yaml.config.swapper.mode.ModeConfigurationYamlSwapper;
import org.apache.shardingsphere.spring.boot.datasource.DataSourceMapSetter;
import org.apache.shardingsphere.spring.boot.prop.SpringBootPropertiesConfiguration;
import org.apache.shardingsphere.spring.boot.rule.LocalRulesCondition;
import org.apache.shardingsphere.spring.boot.schema.DatabaseNameSetter;
import org.apache.shardingsphere.spring.transaction.TransactionTypeScanner;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.*;

/**
 * Spring boot starter configuration.
 */
@Configuration
@ComponentScan("org.apache.shardingsphere.spring.boot.converter")
@EnableConfigurationProperties(SpringBootPropertiesConfiguration.class)
@ConditionalOnProperty(prefix = "spring.shardingsphere", name = "enabled", havingValue = "true", matchIfMissing = true)
@AutoConfigureBefore({DataSourceAutoConfiguration.class, MySeataDataSourceAutoConfiguration.class})
@RequiredArgsConstructor
public class MyShardingSphereAutoConfiguration implements EnvironmentAware {

    private String databaseName;

    private final SpringBootPropertiesConfiguration props;

    private final Map<String, DataSource> dataSourceMap = new LinkedHashMap<>();

    private final SeataAutoDataSourceProxyCreator seataAutoDataSourceProxyCreator;

    /**
     * Get mode configuration.
     *
     * @return mode configuration
     */
    @Bean
    public ModeConfiguration modeConfiguration() {
        return null == props.getMode() ? null : new ModeConfigurationYamlSwapper().swapToObject(props.getMode());
    }

    @Bean
    @Conditional(LocalRulesCondition.class)
    @Autowired(required = false)
    public DataSource shardingSphereDataSource(final ObjectProvider<List<RuleConfiguration>> rules, final ObjectProvider<ModeConfiguration> modeConfig) throws SQLException {
        Collection<RuleConfiguration> ruleConfigs = Optional.ofNullable(rules.getIfAvailable()).orElseGet(Collections::emptyList);
        MyShardingSphereDataSource shardingSphereDataSource = new MyShardingSphereDataSource(Strings.isNullOrEmpty(databaseName) ? DefaultDatabase.LOGIC_NAME : databaseName, modeConfig.getIfAvailable(), dataSourceMap, ruleConfigs, props.getProps());
        seataDataSourceProxy();
        return shardingSphereDataSource;
        //        return ShardingSphereDataSourceFactory.createDataSource(databaseName, modeConfig.getIfAvailable(), dataSourceMap, ruleConfigs, props.getProps());
    }

    public void seataDataSourceProxy(){
        DataSourceProxy dataSourceProxy = null;
        for(Map.Entry<String, DataSource> entry : dataSourceMap.entrySet()){
            dataSourceProxy = new DataSourceProxy(entry.getValue());
            DataSourceProxyHolder.put(entry.getValue(), dataSourceProxy);
        }
    }

    /**
     * Get data source bean from registry center.
     *
     * @param modeConfig mode configuration
     * @return data source bean
     * @throws SQLException SQL exception
     */
    @Bean("shardingSphereDataSource")
    @ConditionalOnMissingBean(DataSource.class)
    public DataSourceProxy dataSource(final ModeConfiguration modeConfig) throws SQLException {
        DataSource shardingDataSource = !dataSourceMap.isEmpty() ?
                new MyShardingSphereDataSource(Strings.isNullOrEmpty(databaseName) ? DefaultDatabase.LOGIC_NAME : databaseName, modeConfig, dataSourceMap, Collections.emptyList(), props.getProps())
//                ShardingSphereDataSourceFactory.createDataSource(databaseName, modeConfig, dataSourceMap, Collections.emptyList(), props.getProps())
                : new MyShardingSphereDataSource(Strings.isNullOrEmpty(databaseName) ? DefaultDatabase.LOGIC_NAME : databaseName, modeConfig);
//        ShardingSphereDataSourceFactory.createDataSource(databaseName, modeConfig);
        return new DataSourceProxy(shardingDataSource);
    }

    /**
     * Create transaction type scanner.
     *
     * @return transaction type scanner
     */
    @Bean
    public TransactionTypeScanner transactionTypeScanner() {
        return new TransactionTypeScanner();
    }

    @Override
    public final void setEnvironment(final Environment environment) {
        dataSourceMap.putAll(DataSourceMapSetter.getDataSourceMap(environment));
        databaseName = DatabaseNameSetter.getDatabaseName(environment);
    }
}

