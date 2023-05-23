package com.rlynic.sharding.slot.example.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.RequiredArgsConstructor;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.shardingsphere.driver.ShardingSphereDriver;
import org.apache.shardingsphere.driver.api.ShardingSphereDataSourceFactory;
import org.apache.shardingsphere.driver.jdbc.core.datasource.ShardingSphereDataSource;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import javax.sql.DataSource;

/**
 * 如果配置了本地数据源，那必须也同样配置shardingDataSource 是否插入不了数据 -> LocalMapperConfiguration
 * 如果只有shardingDataSource，则只需要yml配置就好，会自动注入
 * 多个数据源时，shardingDataSource需要定义
 */
@Configuration
//@EnableTransactionManagement
@MapperScan(basePackages = "com.rlynic.sharding.slot.example.repositories.sharding",
        sqlSessionTemplateRef = "shardingSqlSessionTemplate"/*, sqlSessionFactoryRef = "shardingSqlSessionFactory"*/)
//@AutoConfigureAfter({MyShardingSphereAutoConfiguration.class})
@RequiredArgsConstructor
public class ShardingMapperConfiguration {
//    @Resource
//    ShardingSphereDataSource shardingSphereDataSource;

    private String driverClassName = "org.apache.shardingsphere.driver.ShardingSphereDriver";
    private String jdbcUrl = "jdbc:shardingsphere:classpath:sharding.yaml";

    @Bean("shardingDatasource")
    public DataSource dataSource(){
        // 以 HikariCP 为例
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setDriverClassName(driverClassName);
        dataSource.setJdbcUrl(jdbcUrl);
        return dataSource;
    }


    @Bean(name = "shardingSqlSessionFactory")
    public SqlSessionFactory shardingSqlSessionFactory(@Qualifier("shardingDatasource") DataSource dataSource) throws Exception {
        SqlSessionFactoryBean bean = new SqlSessionFactoryBean();
        bean.setDataSource(dataSource);
        bean.setConfigLocation(new PathMatchingResourcePatternResolver().getResource("classpath:/META-INF/mybatis-config.xml"));
//        bean.setMapperLocations(new PathMatchingResourcePatternResolver().getResources("classpath*:/**/*Mapper.xml"));
//        bean.setTransactionFactory(new SpringManagedTransactionFactory());
        return bean.getObject();
    }

    @Bean(name = "shardingSqlSessionTemplate")
    public SqlSessionTemplate shardingSqlSessionTemplate(@Qualifier("shardingSqlSessionFactory") SqlSessionFactory sqlSessionFactory) throws Exception {
        return new SqlSessionTemplate(sqlSessionFactory);
    }

}