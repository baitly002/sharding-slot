package com.rlynic.sharding.slot.database.sql.rewrite.parameter;

import com.rlynic.sharding.slot.database.SlotContextHolder;
import com.rlynic.sharding.slot.database.configuration.ShardingAutoConfiguration;
import com.rlynic.sharding.slot.database.configuration.SlotShardingProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.shardingsphere.infra.binder.statement.SQLStatementContext;
import org.apache.shardingsphere.infra.binder.statement.dml.InsertStatementContext;
import org.apache.shardingsphere.infra.rewrite.parameter.builder.ParameterBuilder;
import org.apache.shardingsphere.infra.rewrite.parameter.builder.impl.GroupedParameterBuilder;
import org.apache.shardingsphere.infra.rewrite.parameter.rewriter.ParameterRewriter;
import org.apache.shardingsphere.sql.parser.sql.common.statement.dml.InsertStatement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * <code>{@link ShardingSlotInsertValueParameterRewriter}</code>
 * 注入slot字段值
 * @author crisis
 */
@Slf4j
public class ShardingSlotInsertValueParameterRewriter implements ParameterRewriter<InsertStatementContext> {
    private SlotShardingProperties slotShardingProperties;

    @Override
    public boolean isNeedRewrite(final SQLStatementContext sqlStatementContext) {
        if(null == slotShardingProperties){
            slotShardingProperties = ShardingAutoConfiguration.context.getBean(SlotShardingProperties.class);
        }
        return sqlStatementContext instanceof InsertStatementContext
                && !((InsertStatementContext)sqlStatementContext).getColumnNames().contains(slotShardingProperties.getColumn())
                && slotShardingProperties.getTableNames().contains(((InsertStatement) sqlStatementContext.getSqlStatement()).getTable().getTableName().getIdentifier().getValue());
    }

    @Override
    public void rewrite(ParameterBuilder parameterBuilder, InsertStatementContext insertStatementContext, List<Object> parameters) {
        try {
            List<Integer> slotsContext = SlotContextHolder.get();
            if (CollectionUtils.isEmpty(slotsContext)) {
                return;
            }
            Iterator<Integer> slots = slotsContext.iterator();
            int count = 0;
            int parameterCount = 0;
            for (List<Object> each : insertStatementContext.getGroupedParameters()) {
                parameterCount += insertStatementContext.getInsertValueContexts().get(count).getParameterCount();
                Comparable<?> generatedValue = slots.next();
                if (!each.isEmpty()) {
                    ((GroupedParameterBuilder) parameterBuilder).getParameterBuilders().get(count).addAddedParameters(parameterCount, new ArrayList<>(Collections.singleton(generatedValue)));
                }
                count++;
            }
        }catch (Exception e){
            log.error("注入slot时发生异常，请检查！", e);
        }finally {
            SlotContextHolder.clear();
        }
    }
}
