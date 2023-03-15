package com.rlynic.sharding.slot.database.sql.rewrite.parameter;

import com.rlynic.sharding.slot.database.RemoveParameterMarkerHolder;
import com.rlynic.sharding.slot.database.configuration.ShardingAutoConfiguration;
import com.rlynic.sharding.slot.database.configuration.SlotShardingProperties;
import com.rlynic.sharding.slot.database.context.selectin.engine.SelectInContextEngine;
import com.rlynic.sharding.slot.database.util.SqlExpressionExtractUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.shardingsphere.infra.binder.statement.SQLStatementContext;
import org.apache.shardingsphere.infra.binder.statement.dml.SelectStatementContext;
import org.apache.shardingsphere.infra.rewrite.parameter.builder.ParameterBuilder;
import org.apache.shardingsphere.infra.rewrite.parameter.rewriter.ParameterRewriter;
import org.apache.shardingsphere.sql.parser.sql.common.segment.dml.expr.ExpressionSegment;
import org.apache.shardingsphere.sql.parser.sql.common.segment.dml.expr.InExpression;
import org.apache.shardingsphere.sql.parser.sql.common.segment.dml.expr.ListExpression;
import org.apache.shardingsphere.sql.parser.sql.common.segment.dml.expr.simple.ParameterMarkerExpressionSegment;
import org.apache.shardingsphere.sql.parser.sql.common.segment.dml.predicate.WhereSegment;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * <code>{@link ShardingInValueParameterRewriter}</code>
 * 注入slot字段值
 * @author crisis
 */
@Slf4j
public class ShardingInValueParameterRewriter implements ParameterRewriter<SelectStatementContext> {
    private SlotShardingProperties slotShardingProperties;
    private Collection<InExpression> inExpressions;

    @Override
    public boolean isNeedRewrite(final SQLStatementContext sqlStatementContext) {
        if(null == slotShardingProperties){
            slotShardingProperties = ShardingAutoConfiguration.context.getBean(SlotShardingProperties.class);
        }
        if(sqlStatementContext instanceof SelectStatementContext){
//            sqlStatementContext.getTablesContext().getTableNames().contains()
            Collection<WhereSegment> whereSegments = ((SelectStatementContext) sqlStatementContext).getWhereSegments();
            inExpressions = new SelectInContextEngine().getInExpressions(whereSegments);
        }
        return sqlStatementContext instanceof SelectStatementContext;
//                && !((InsertStatementContext)sqlStatementContext).getColumnNames().contains(slotShardingProperties.getColumn())
//                && slotShardingProperties.getTableNames().contains(((SelectStatementContext) ((SelectStatementContext) sqlStatementContext).isSameGroupByAndOrderByItems().getSqlStatement()).getTable().getTableName().getIdentifier().getValue());
    }


    @Override
    public void rewrite(ParameterBuilder parameterBuilder, SelectStatementContext selectStatementContext, List<Object> parameters) {
        try {
            Map<String, List<ParameterMarkerExpressionSegment>> pms = RemoveParameterMarkerHolder.get();
            for(InExpression inExpression : inExpressions){
//                ExpressionSegment right = inExpression.getRight();
//                if (right instanceof ListExpression) {
//                    List<ExpressionSegment> items = ((ListExpression) right).getItems();
//                    items.remove(0);
//                }
            }
        }catch (Exception e){
            log.error("处理in语句时发生异常，请检查！", e);
        }
    }
}
