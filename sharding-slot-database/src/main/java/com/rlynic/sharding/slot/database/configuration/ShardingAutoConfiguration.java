/*
  This file created at 2020/6/24.

  Copyright (c) 2002-2020 crisis, Inc. All rights reserved.
 */
package com.rlynic.sharding.slot.database.configuration;

import com.rlynic.sharding.plugin.ShardingJdbcCoreAgent;
import com.rlynic.sharding.slot.database.strategy.SlotDatabaseMatcher;
import com.rlynic.sharding.slot.database.util.SpringBeanUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Map;

/**
 * <code>{@link ShardingAutoConfiguration}</code>
 *
 * @author crisis
 */
@Configuration
@EnableConfigurationProperties({
    SlotShardingProperties.class
})
@Slf4j
public class ShardingAutoConfiguration{

    @Bean
    @ConditionalOnClass(RedisTemplate.class)
    @ConditionalOnBean(RedisTemplate.class)
    public SlotDatabaseMatcher slotDatabaseMatcherRedis(SlotShardingProperties slotShardingProperties, RedisTemplate redisTemplate){
        ShardingJdbcCoreAgent.getInstance().init();
        if(redisTemplate!=null){
            String keySlot = "pan-server:slot";
            if(redisTemplate.hasKey(keySlot)){
                //原已经有记录
                SlotShardingProperties redidSlotProp = (SlotShardingProperties)redisTemplate.opsForValue().get(keySlot);
                if(slotShardingProperties.getSize().intValue() == redidSlotProp.getSize().intValue()){
                    //分库大小一致
                    if(slotShardingProperties.getRange()!=null) {
                        Map<String, String> datasourceRange = slotShardingProperties.getRange().getDatasource();
                        Map<String, String> redisRange = redidSlotProp.getRange().getDatasource();
                        datasourceRange.forEach((k,v) ->{
                            String redisVal = trim(redisRange.get(k));
                            if(StringUtils.isBlank(redisVal)){
                                if(slotShardingProperties.getForceSharding()!=null && slotShardingProperties.getForceSharding()){
                                    log.error("从旧的分库规则找不到对应的[{}]数据，分库名称配置有变动!,但已经跳过检查", k);
                                }else {
                                    throw new RuntimeException(String.format("从旧的分库规则找不到对应的[%s]数据，请检查分库名称配置[slot.sharding.logic-datasource-prefix]是否有变动!", k));
                                }
                            }
                            if(!trim(v).equalsIgnoreCase(redisVal) ){
                                if(slotShardingProperties.getForceSharding()!=null && slotShardingProperties.getForceSharding()){
                                    log.error("分库规则前后有变动,原分库[{}]的取值范围是[{}],新分库[{}]的取值范围是[{}],但已经跳过检查", k, redisVal, k, v);
                                }else {
                                    throw new RuntimeException(String.format("分库规则前后有变动,原分库[%s]的取值范围是[%s],新分库[%s]的取值范围是[%s]", k, redisVal, k, v));
                                }
                            }
                        });
                        redisTemplate.opsForValue().set(keySlot, slotShardingProperties);//替换旧配置
                    }else{
                        throw new RuntimeException("分库规则不能为空，请检查[slot.sharding.range]是否已经正确配置");
                    }
                }else {
                    if(slotShardingProperties.getForceSharding()!=null && slotShardingProperties.getForceSharding()){
                        log.error("slot分库大小与之前的不一致，但已经强制刷新跳过处理！");
                        redisTemplate.opsForValue().set(keySlot, slotShardingProperties);//替换旧配置
                    }else{
                        throw new RuntimeException("slot分库大小与之前的不一致，请检查确认！若需强制启动可配置[slot.sharding.force-sharding=true]");
                    }
                }
            }else{
                redisTemplate.opsForValue().set(keySlot, slotShardingProperties);//保存分库配置
            }
        }
        return new SlotDatabaseMatcher(slotShardingProperties);
    }

    @Bean
    @ConditionalOnMissingBean(RedisTemplate.class)
    public SlotDatabaseMatcher slotDatabaseMatcher(SlotShardingProperties slotShardingProperties){
        ShardingJdbcCoreAgent.getInstance().init();
        return new SlotDatabaseMatcher(slotShardingProperties);
    }

    @Bean
    public SpringBeanUtil springBeanUtil(){
        return new SpringBeanUtil();
    }

    static String trim(String str){
        if(str != null) {
            return str.replaceAll(" ", "");
        }
        return "";
    }
}