package com.rlynic.sharding.slot.database.sql.token;

import com.rlynic.sharding.slot.database.configuration.ShardingAutoConfiguration;
import com.rlynic.sharding.slot.database.configuration.SlotShardingProperties;
import lombok.Setter;
import org.apache.shardingsphere.infra.binder.segment.insert.values.InsertValueContext;
import org.apache.shardingsphere.infra.binder.statement.SQLStatementContext;
import org.apache.shardingsphere.infra.binder.statement.dml.InsertStatementContext;
import org.apache.shardingsphere.infra.datanode.DataNode;
import org.apache.shardingsphere.infra.rewrite.sql.token.generator.OptionalSQLTokenGenerator;
import org.apache.shardingsphere.infra.rewrite.sql.token.generator.aware.RouteContextAware;
import org.apache.shardingsphere.infra.rewrite.sql.token.pojo.generic.InsertValuesToken;
import org.apache.shardingsphere.infra.route.context.RouteContext;
import org.apache.shardingsphere.sharding.rewrite.token.pojo.ShardingInsertValue;
import org.apache.shardingsphere.sql.parser.sql.common.segment.dml.assignment.InsertValuesSegment;
import org.apache.shardingsphere.sql.parser.sql.common.segment.dml.expr.ExpressionSegment;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

@Setter
public final class TransformSlotInsertValuesTokenGenerator implements OptionalSQLTokenGenerator<InsertStatementContext>, RouteContextAware {

    private RouteContext routeContext;
    private SlotShardingProperties slotShardingProperties;

    @Override
    public boolean isGenerateSQLToken(final SQLStatementContext<?> sqlStatementContext) {
        if(null == slotShardingProperties){
            slotShardingProperties = ShardingAutoConfiguration.context.getBean(SlotShardingProperties.class);
        }
        if(sqlStatementContext instanceof InsertStatementContext){
            InsertStatementContext insertStatementContext = (InsertStatementContext) sqlStatementContext;
            return !insertStatementContext.getSqlStatement().getValues().isEmpty()
                    && slotShardingProperties != null
                    && !insertStatementContext.getColumnNames().contains(slotShardingProperties.getColumn())
                    && slotShardingProperties.getTableNames().contains(insertStatementContext.getSqlStatement().getTable().getTableName().getIdentifier().getValue());
        }else{
            return false;
        }
    }

    @Override
    public InsertValuesToken generateSQLToken(final InsertStatementContext insertStatementContext) {
        Collection<InsertValuesSegment> insertValuesSegments = (insertStatementContext.getSqlStatement()).getValues();
        InsertValuesToken result = new SlotInsertValuesToken(getStartIndex(insertValuesSegments), getStopIndex(insertValuesSegments));
        Iterator<Collection<DataNode>> originalDataNodesIterator = null == routeContext || routeContext.getOriginalDataNodes().isEmpty()
                ? null
                : routeContext.getOriginalDataNodes().iterator();
        for (InsertValueContext each : insertStatementContext.getInsertValueContexts()) {
            List<ExpressionSegment> expressionSegments = each.getValueExpressions();
            Collection<DataNode> dataNodes = null == originalDataNodesIterator ? Collections.emptyList() : originalDataNodesIterator.next();
            result.getInsertValues().add(new SlotInsertValue(expressionSegments, dataNodes));
        }
        return result;
    }

    private int getStartIndex(final Collection<InsertValuesSegment> segments) {
        int result = segments.iterator().next().getStartIndex();
        for (InsertValuesSegment each : segments) {
            result = Math.min(result, each.getStartIndex());
        }
        return result;
    }

    private int getStopIndex(final Collection<InsertValuesSegment> segments) {
        int result = segments.iterator().next().getStopIndex();
        for (InsertValuesSegment each : segments) {
            result = Math.max(result, each.getStopIndex());
        }
        return result;
    }
}
