package com.rlynic.sharding.slot.database.strategy;

import com.rlynic.sharding.slot.database.configuration.ShardingAutoConfiguration;
import org.apache.shardingsphere.sharding.api.sharding.standard.PreciseShardingValue;
import org.apache.shardingsphere.sharding.api.sharding.standard.RangeShardingValue;
import org.apache.shardingsphere.sharding.api.sharding.standard.StandardShardingAlgorithm;

import java.util.Collection;
import java.util.Properties;

public class HashSlotShardingAlgorithm implements StandardShardingAlgorithm<String> {
    private SlotDatabaseMatcher matcher;
    public HashSlotShardingAlgorithm(){};

    @Override
    public String doSharding(Collection availableTargetNames, PreciseShardingValue shardingValue) {
        if(null == matcher){
            matcher = ShardingAutoConfiguration.context.getBean(SlotDatabaseMatcher.class);
        }

        return matcher.match(shardingValue.getValue());
    }

    @Override
    public Collection<String> doSharding(Collection availableTargetNames, RangeShardingValue shardingValue) {
        return null;
    }

    @Override
    public String getType() {
        return "HASH_SLOT";
    }

    @Override
    public Properties getProps() {
        return null;
    }

    @Override
    public void init(Properties properties) {

    }
}
