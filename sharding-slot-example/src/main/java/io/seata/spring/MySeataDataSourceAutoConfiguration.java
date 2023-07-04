//package io.seata.spring;
//
//import io.seata.spring.annotation.datasource.MySeataAutoDataSourceProxyCreator;
//import io.seata.spring.boot.autoconfigure.SeataCoreAutoConfiguration;
//import io.seata.spring.boot.autoconfigure.properties.SeataProperties;
//import org.springframework.boot.autoconfigure.AutoConfigureAfter;
//import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
//import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
//import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
//import org.springframework.context.annotation.Bean;
//
//import javax.sql.DataSource;
//
//@ConditionalOnBean({DataSource.class})
//@ConditionalOnExpression("${seata.enabled:true} && ${seata.enableAutoDataSourceProxy:true} && ${seata.enable-auto-data-source-proxy:true}")
//@AutoConfigureAfter({SeataCoreAutoConfiguration.class})
//public class MySeataDataSourceAutoConfiguration {
//    public MySeataDataSourceAutoConfiguration() {
//    }
//
//    @Bean({"seataAutoDataSourceProxyCreator"})
//    @ConditionalOnMissingBean({MySeataAutoDataSourceProxyCreator.class})
//    public MySeataAutoDataSourceProxyCreator seataAutoDataSourceProxyCreator(SeataProperties seataProperties) {
//        return new MySeataAutoDataSourceProxyCreator(seataProperties.isUseJdkProxy(), seataProperties.getExcludesForAutoProxying(), seataProperties.getDataSourceProxyMode());
//    }
//
//}