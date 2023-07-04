package com.rlynic.sharding.slot.example.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import javax.sql.DataSource;

/**
 * 增加本地数据源时，会影响sharding的数据源，需要针对sharding进行修改 -> ShardingMapperConfiguration
 */
@Configuration
@MapperScan(basePackages = "com.rlynic.sharding.slot.example.repositories.master",
        sqlSessionTemplateRef = "localSqlSessionTemplate"/*, sqlSessionFactoryRef = "localSqlSessionFactory"*/)
//@MyBatisResourcesScan(typeAliasesPackages = "com.rlynic.sharding.slot.example.entities", mapperLocationPatterns = "META-INF/**/*Mapper.xml")
public class MasterMapperConfiguration {

    @Bean
//    @ConfigurationProperties(prefix = "spring.datasource.master")
    public HikariConfig hikariConfig() {
        HikariConfig hikariConfig = new HikariConfig();
        //driver-class-name: com.mysql.cj.jdbc.Driver
        //      type: com.zaxxer.hikari.HikariDataSource
        //      #      jdbc-url: jdbc:mysql://10.201.62.180:3306/demo_ds_master?characterEncoding=utf-8&useSSL=false&autoReconnect=true&serverTimezone=Asia/Shanghai&nullCatalogMeansCurrent=true&allowPublicKeyRetrieval=true&useInformationSchema=false
        //      jdbc-url: jdbc:mysql://10.200.20.94:30093/demo_ds_master?characterEncoding=utf8&zeroDateTimeBehavior=convertToNull&useSSL=false&allowMultiQueries=true&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=Asia/Shanghai&nullCatalogMeansCurrent=true&allowPublicKeyRetrieval=true
        //      username: root
        //      password: Link$2013
        hikariConfig.setPoolName("localDatasourcePool");
        hikariConfig.setUsername("root");
        hikariConfig.setPassword("Link$2013");
        hikariConfig.setDriverClassName("com.mysql.cj.jdbc.Driver");
        hikariConfig.setJdbcUrl("jdbc:mysql://10.200.20.94:30093/demo_ds_master?characterEncoding=utf8&zeroDateTimeBehavior=convertToNull&useSSL=false&allowMultiQueries=true&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=Asia/Shanghai&nullCatalogMeansCurrent=true&allowPublicKeyRetrieval=true");
        return hikariConfig;
    }

    @Bean(name="localDataSource")
    @Primary
    public DataSource localDataSource(@Qualifier("hikariConfig") HikariConfig configuration){
        DataSource dataSource = new HikariDataSource(configuration);
//        SeataDataSourceProxy proxy = buildProxy(dataSource, "AT");
//        DataSourceProxyHolder.put(dataSource, proxy);
        return dataSource;
    }

//    SeataDataSourceProxy buildProxy(DataSource origin, String proxyMode) {
//        if (BranchType.AT.name().equalsIgnoreCase(proxyMode)) {
//            return new DataSourceProxy(origin);
//        }
//        if (BranchType.XA.name().equalsIgnoreCase(proxyMode)) {
//            return new DataSourceProxyXA(origin);
//        }
//        throw new IllegalArgumentException("Unknown dataSourceProxyMode: " + proxyMode);
//    }

    @Bean(name = "localSqlSessionFactory")
    public SqlSessionFactory localSqlSessionFactory(@Qualifier("localDataSource") DataSource dataSource) throws Exception {
        SqlSessionFactoryBean bean = new SqlSessionFactoryBean();
        bean.setDataSource(dataSource);
//        bean.setDataSource(new HikariDataSource(hikariConfig));
//        bean.setConfigLocation(new PathMatchingResourcePatternResolver().getResource("classpath:/META-INF/mybatis-master-config.xml"));
        bean.setMapperLocations(new PathMatchingResourcePatternResolver().getResources("classpath*:/**/Master*Mapper.xml"));
        return bean.getObject();
    }

    @Bean(name = "localSqlSessionTemplate")
    public SqlSessionTemplate localSqlSessionTemplate(@Qualifier("localSqlSessionFactory") SqlSessionFactory sqlSessionFactory) throws Exception {
        return new SqlSessionTemplate(sqlSessionFactory);
    }

}