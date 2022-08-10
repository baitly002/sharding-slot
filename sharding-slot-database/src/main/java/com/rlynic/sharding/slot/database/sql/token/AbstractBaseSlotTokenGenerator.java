package com.rlynic.sharding.slot.database.sql.token;

import com.rlynic.sharding.slot.database.SlotContextHolder;
import com.rlynic.sharding.slot.database.configuration.ShardingAutoConfiguration;
import com.rlynic.sharding.slot.database.configuration.SlotShardingProperties;
import org.apache.shardingsphere.infra.binder.statement.SQLStatementContext;
import org.apache.shardingsphere.infra.binder.statement.dml.InsertStatementContext;
import org.apache.shardingsphere.infra.rewrite.sql.token.generator.OptionalSQLTokenGenerator;

public abstract class AbstractBaseSlotTokenGenerator implements OptionalSQLTokenGenerator<InsertStatementContext> {

    protected SlotShardingProperties slotShardingProperties;

    @Override
    public final boolean isGenerateSQLToken(final SQLStatementContext sqlStatementContext) {
        if(null == slotShardingProperties){
            slotShardingProperties = ShardingAutoConfiguration.context.getBean(SlotShardingProperties.class);
        }
        boolean isSlot = sqlStatementContext instanceof InsertStatementContext
                && isGenerateSQLToken((InsertStatementContext)sqlStatementContext);
        if(!isSlot){
            SlotContextHolder.clear();
        }
        return isSlot;
    }

    protected abstract boolean isGenerateSQLToken(InsertStatementContext insertStatementContext);
}
