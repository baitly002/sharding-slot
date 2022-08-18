package com.rlynic.sharding.slot.example.config;

import lombok.RequiredArgsConstructor;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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

//    @Bean
//    @Primary
//    public DataSourceProxy dataSourceProxy(DataSource dataSource){
//        return new DataSourceProxy(dataSource);
//    }


    @Bean(name = "shardingSqlSessionFactory")
    public SqlSessionFactory shardingSqlSessionFactory(@Qualifier("shardingSphereDataSource") DataSource dataSource) throws Exception {
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