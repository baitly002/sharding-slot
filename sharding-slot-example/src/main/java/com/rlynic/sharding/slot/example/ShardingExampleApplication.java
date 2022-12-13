/*
  This file created at 2020/6/21.

  Copyright (c) 2002-2020 crisis, Inc. All rights reserved.
 */
package com.rlynic.sharding.slot.example;

import io.seata.spring.annotation.datasource.EnableAutoDataSourceProxy;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.transaction.jta.JtaAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * <code>{@link ShardingExampleApplication}</code>
 *
 * @author crisis
 */
//@SpringBootApplication(
//        scanBasePackages = "com.rlynic.sharding.slot.example",
//        exclude = JtaAutoConfiguration.class
//)
//@SpringBootApplication(exclude = {JtaAutoConfiguration.class, ShardingSphereAutoConfiguration.class})
//@EnableAutoDataSourceProxy(excludes = "com.rlynic.sharding.slot.example.config.sharding.MyShardingSphereDataSource")

@SpringBootApplication(exclude = {JtaAutoConfiguration.class/*, ShardingSphereAutoConfiguration.class*/})
@EnableAutoDataSourceProxy(useJdkProxy = true)
public class ShardingExampleApplication {


    public static void main(final String[] args) throws Exception {
        try (ConfigurableApplicationContext applicationContext = SpringApplication.run(ShardingExampleApplication.class, args)) {
            ExampleExecuteTemplate.run(applicationContext.getBean(ExampleService.class));
        }
//        GeneratedKeysResultSet
//        ShardingSphereDatabaseMetaData
//        SeataAutoDataSourceProxyCreator
    }

//    @Configuration
//    @MapperScan(basePackages = "com.rlynic.sharding.slot.example.repositories", sqlSessionFactoryRef = "shardingSqlSessionFactory")
//    public static class MybatisConfiguration{
//        private final MybatisProperties properties;
//
//        private final ResourceLoader resourceLoader;
//
//        private final DatabaseIdProvider databaseIdProvider;
//
//        private final List<ConfigurationCustomizer> configurationCustomizers;
//
//        public MybatisConfiguration(
//                MybatisProperties properties,
//                ResourceLoader resourceLoader,
//                ObjectProvider<DatabaseIdProvider> databaseIdProvider,
//                ObjectProvider<List<ConfigurationCustomizer>> configurationCustomizersProvider) {
//            this.properties = properties;
//            this.resourceLoader = resourceLoader;
//            this.databaseIdProvider = databaseIdProvider.getIfAvailable();
//            this.configurationCustomizers = configurationCustomizersProvider
//                    .getIfAvailable();
//        }
//
//        @Bean(name = "shardingSqlSessionFactory")
//        public SqlSessionFactory basicSqlSessionFactory(@Qualifier("shardingSphereDataSource") DataSource dataSource) throws Exception {
//            SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
//            factoryBean.setDataSource(dataSource);
//
//            return generateSessionFactory(factoryBean);
//        }
//
//        private SqlSessionFactory generateSessionFactory(SqlSessionFactoryBean factory) throws Exception {
//            factory.setVfs(SpringBootVFS.class);
//            if (StringUtils.hasText(properties.getConfigLocation())) {
//                factory.setConfigLocation(resourceLoader
//                        .getResource(properties.getConfigLocation()));
//            }
//            if (properties.getConfigurationProperties() != null) {
//                factory.setConfigurationProperties(properties
//                        .getConfigurationProperties());
//            }
//            if (databaseIdProvider != null) {
//                factory.setDatabaseIdProvider(databaseIdProvider);
//            }
//            if (StringUtils.hasLength(properties.getTypeAliasesPackage())) {
//                factory.setTypeAliasesPackage(this.properties
//                        .getTypeAliasesPackage());
//            }
//            if (StringUtils.hasLength(properties.getTypeHandlersPackage())) {
//                factory.setTypeHandlersPackage(properties
//                        .getTypeHandlersPackage());
//            }
//            if (!ObjectUtils.isEmpty(properties.resolveMapperLocations())) {
//                factory.setMapperLocations(properties.resolveMapperLocations());
//            }
//
//            factory.getObject().getConfiguration().setMapUnderscoreToCamelCase(true);
//
//            return factory.getObject();
//        }
//    }
//
//
//    @Configuration
//    @ConditionalOnProperty(prefix = "spring.datasource", name = "url")
//    class DataSourceConfiguration{
//
//        @Bean
//        @Primary
//        @ConfigurationProperties("spring.datasource.hikari")
//        public DataSource defaultDataSource(@Qualifier("dataSourceProperties") DataSourceProperties dataSourceProperties) {
//            return dataSourceProperties.initializeDataSourceBuilder().type(HikariDataSource.class).build();
//        }
//
//        @Bean
//        @Primary
//        @ConfigurationProperties("spring.datasource")
//        public DataSourceProperties dataSourceProperties(){
//            return new DataSourceProperties();
//        }
//
//    }

}