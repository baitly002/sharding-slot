package com.rlynic.sharding.slot.database.configuration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertiesPropertySource;

import java.util.Properties;

@Configuration
@Order(0)
@Slf4j
public class SlotShardingBeanPostProcessor implements BeanPostProcessor, ApplicationContextAware {

    private ConfigurableEnvironment environment;
    private ApplicationContext applicationContext;

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
        if(applicationContext.getEnvironment() instanceof ConfigurableEnvironment) {
            this.environment = (ConfigurableEnvironment)applicationContext.getEnvironment();
            slotAuto(this.environment);
        }
    }

    public void slotAuto(ConfigurableEnvironment environment){
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
        PropertiesPropertySource pps = new PropertiesPropertySource("slot-auto.properties", properties);
        environment.getPropertySources().addFirst(pps);
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

    //判断是否属于2的n次方
    public static boolean is2pow(Integer n){
        return n!=null && n > 0 && (n & (n-1)) == 0;
    }

}
