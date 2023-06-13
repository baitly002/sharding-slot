/*
  This file created at 2020/6/24.

  Copyright (c) 2002-2020 crisis, Inc. All rights reserved.
 */
package com.rlynic.sharding.slot.database.configuration;

import com.rlynic.sharding.plugin.ShardingJdbcCoreAgent;
import com.rlynic.sharding.slot.database.strategy.SlotDatabaseMatcher;
import com.rlynic.sharding.slot.database.util.SpringBeanUtil;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * <code>{@link ShardingAutoConfiguration}</code>
 *
 * @author crisis
 */
@Configuration
@EnableConfigurationProperties({
    SlotShardingProperties.class
})
public class ShardingAutoConfiguration{

//    public static ApplicationContext context;

    @Bean
    public SlotDatabaseMatcher slotDatabaseMatcher(SlotShardingProperties slotShardingProperties){
        ShardingJdbcCoreAgent.getInstance().init();
        return new SlotDatabaseMatcher(slotShardingProperties);
    }

    @Bean
    public SpringBeanUtil springBeanUtil(){
        return new SpringBeanUtil();
    }
//    @Override
//    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
//        this.context = applicationContext;
//    }
}