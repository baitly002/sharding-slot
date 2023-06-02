package com.rlynic.sharding.slot.database.sql.token;

import com.rlynic.sharding.slot.database.CRC16;
import com.rlynic.sharding.slot.database.RemoveParameterMarkerHolder;
import com.rlynic.sharding.slot.database.configuration.ShardingAutoConfiguration;
import com.rlynic.sharding.slot.database.configuration.SlotShardingProperties;
import com.rlynic.sharding.slot.database.context.selectin.engine.SelectInContextEngine;
import com.rlynic.sharding.slot.database.strategy.HashSlotRouteException;
import com.rlynic.sharding.slot.database.strategy.SlotDatabaseMatcher;
import org.apache.commons.lang.StringUtils;
import org.apache.shardingsphere.infra.binder.statement.SQLStatementContext;
import org.apache.shardingsphere.infra.binder.statement.dml.DeleteStatementContext;
import org.apache.shardingsphere.infra.binder.statement.dml.SelectStatementContext;
import org.apache.shardingsphere.infra.binder.statement.dml.UpdateStatementContext;
import org.apache.shardingsphere.infra.rewrite.context.SQLRewriteContext;
import org.apache.shardingsphere.infra.rewrite.sql.token.generator.CollectionSQLTokenGenerator;
import org.apache.shardingsphere.infra.rewrite.sql.token.pojo.SQLToken;
import org.apache.shardingsphere.infra.route.context.RouteContext;
import org.apache.shardingsphere.sharding.api.config.ShardingRuleConfiguration;
import org.apache.shardingsphere.sharding.api.config.strategy.sharding.StandardShardingStrategyConfiguration;
import org.apache.shardingsphere.sharding.rewrite.token.generator.IgnoreForSingleRoute;
import org.apache.shardingsphere.sharding.rule.ShardingRule;
import org.apache.shardingsphere.sql.parser.sql.common.segment.dml.column.ColumnSegment;
import org.apache.shardingsphere.sql.parser.sql.common.segment.dml.expr.ExpressionSegment;
import org.apache.shardingsphere.sql.parser.sql.common.segment.dml.expr.InExpression;
import org.apache.shardingsphere.sql.parser.sql.common.segment.dml.expr.simple.LiteralExpressionSegment;
import org.apache.shardingsphere.sql.parser.sql.common.segment.dml.expr.simple.ParameterMarkerExpressionSegment;
import org.apache.shardingsphere.sql.parser.sql.common.segment.dml.predicate.WhereSegment;
import org.apache.shardingsphere.sql.parser.sql.common.segment.generic.OwnerSegment;
import org.apache.shardingsphere.sql.parser.sql.common.segment.generic.table.SimpleTableSegment;

import java.util.*;

/**
 * 移除in查询与分库不匹配的值
 */
public class ShardingRemoveInTokenGenerator implements CollectionSQLTokenGenerator<SQLStatementContext<?>>, IgnoreForSingleRoute {

    private final ShardingRule shardingRule;

    private final RouteContext routeContext;

    private final SQLRewriteContext sqlRewriteContext;

    private SlotShardingProperties slotShardingProperties;

    private SlotDatabaseMatcher slotDatabaseMatcher;

    private Collection<InExpression> inExpressions;


    public ShardingRemoveInTokenGenerator(ShardingRule shardingRule, RouteContext routeContext, SQLRewriteContext sqlRewriteContext){
        this.shardingRule = shardingRule;
        this.routeContext = routeContext;
        this.sqlRewriteContext = sqlRewriteContext;
        this.slotShardingProperties = ShardingAutoConfiguration.context.getBean(SlotShardingProperties.class);
        this.slotDatabaseMatcher = ShardingAutoConfiguration.context.getBean(SlotDatabaseMatcher.class);
    }

    @Override
    public boolean isGenerateSQLToken(final SQLStatementContext<?> sqlStatementContext) {
        return isContainsAggregationDistinctProjection(sqlStatementContext);
    }

    private boolean isContainsAggregationDistinctProjection(final SQLStatementContext<?> sqlStatementContext) {
        return (sqlStatementContext instanceof SelectStatementContext || sqlStatementContext instanceof UpdateStatementContext ||
                sqlStatementContext instanceof DeleteStatementContext) && !routeContext.isSingleRouting();
    }

    @Override
    public Collection<? extends SQLToken> generateSQLTokens(final SQLStatementContext<?> sqlStatementContext) {
        Collection<SQLToken> result = new LinkedList<>();
        if (isContainsAggregationDistinctProjection(sqlStatementContext)) {
            Collection<WhereSegment> whereSegments = null;
            if(sqlStatementContext instanceof UpdateStatementContext){
                whereSegments = ((UpdateStatementContext) sqlStatementContext).getWhereSegments();
            }
            if(sqlStatementContext instanceof DeleteStatementContext){
                whereSegments = ((DeleteStatementContext) sqlStatementContext).getWhereSegments();
            }
            if(sqlStatementContext instanceof SelectStatementContext){
                whereSegments = ((SelectStatementContext) sqlStatementContext).getWhereSegments();
            }
            if(whereSegments != null && whereSegments.size() > 0){
                Collection<InExpression> inExpressions = new SelectInContextEngine().getInExpressions(whereSegments);
                for(InExpression inExpression : inExpressions){
//                    Collection<ExpressionSegment> segments = inExpression.getExpressionList();
//                    for(ExpressionSegment segment : segments){
//                        if(segment instanceof LiteralExpressionSegment){
//                            //TODO 常量
//                        }
//                    }
                    boolean matchShardingStrategy = matchShardingStrategy((SelectStatementContext) sqlStatementContext, inExpression);
                    boolean matchSingleSelectIn = matchSingleSelectIn(inExpression);
                    if(matchShardingStrategy && matchSingleSelectIn) {
                        result.addAll(shardingIn(inExpression, sqlRewriteContext.getParameters()));
                    }
                }
            }
        }
        return result;
    }

    public Collection<RemoveInToken> shardingIn(InExpression inExpression, List<Object> parameters){
        //removeInTokens 无需有值，因为这是给外层统一rewrite的，而非针对sharding后具体库的sql语句rewrite
        Collection<RemoveInToken> removeInTokens = new ArrayList<>();
        inExpression.getExpressionList().forEach(item -> {
            String routedb = "";
            if(item instanceof ParameterMarkerExpressionSegment){
                int mi = ((ParameterMarkerExpressionSegment) item).getParameterMarkerIndex();
                routedb = match(parameters.get(mi));
            }
            if(item instanceof LiteralExpressionSegment){
                routedb = match(((LiteralExpressionSegment) item).getLiterals());
            }
            final String routedbName = routedb;
            if(StringUtils.isNotBlank(routedbName)){
                routeContext.getRouteUnits().forEach(route -> {
                    String dbName = route.getDataSourceMapper().getLogicName();
                    if(!routedbName.equalsIgnoreCase(dbName)){
                        List<ExpressionSegment> p = new ArrayList<>();
                        p.add(item);
                        RemoveParameterMarkerHolder.add(dbName, p);
                    }
                });
            }
        });
        return removeInTokens;
    }

    public boolean matchSingleSelectIn(InExpression inExpression){
        Collection<ExpressionSegment> items = inExpression.getExpressionList();
        for(ExpressionSegment item : items){
            return item instanceof ParameterMarkerExpressionSegment || item instanceof LiteralExpressionSegment;
        }
        return false;
    }

    public boolean matchShardingStrategy(SelectStatementContext selectStatementContext, InExpression inExpression){
        //TODO IN查询字段匹配规则后续再优化
        if(inExpression.isNot()){
            //not in 查询不进行重写
            return false;
        }
        Map<String, String> tableMapping = new HashMap<>();
        Collection<SimpleTableSegment> tableSegments =  selectStatementContext.getAllTables();
        tableSegments.forEach(table -> {
            table.getAlias().ifPresent(alias -> {
                tableMapping.put(alias, table.getTableName().getIdentifier().getValue());
            });
        });
        ShardingRuleConfiguration ruleConfiguration = (ShardingRuleConfiguration) shardingRule.getConfiguration();
        ruleConfiguration.getTables().forEach(tb -> {
            StandardShardingStrategyConfiguration shardingStrategyConfiguration = (StandardShardingStrategyConfiguration)tb.getDatabaseShardingStrategy();
            tableMapping.put(tb.getLogicTable() +"."+ shardingStrategyConfiguration.getShardingColumn(), "true");
        });
        ExpressionSegment left = inExpression.getLeft();
        if(left instanceof ColumnSegment){
            OwnerSegment ownerSegment = ((ColumnSegment) left).getOwner().orElse(null);
            if(ownerSegment != null){
                //有别名
                return matchSharding(tableSegments, tableMapping, tableMapping.get(ownerSegment.getIdentifier().getValue()), left);
            }else{
                return matchSharding(tableSegments, tableMapping, null, left);
            }
        }
        return false;
    }

    public boolean matchSharding(Collection<SimpleTableSegment> tableSegments, Map<String, String> tableMapping, String keyPrefix, ExpressionSegment left){
        for(SimpleTableSegment table : tableSegments){
            String match = "";
            if(keyPrefix==null){
                match = tableMapping.get(table.getTableName().getIdentifier().getValue()+"."+ ((ColumnSegment) left).getQualifiedName());
            }else{
                match = tableMapping.get(keyPrefix+"."+ ((ColumnSegment) left).getIdentifier().getValue());
            }
            if("true".equalsIgnoreCase(match)) return true;
        }
        return false;
    }

    public String match(Object key){
        try{
            String s = String.valueOf(key);
            int slot = CRC16.CRC16_CCITT(s.getBytes()) & slotShardingProperties.getNumber() - 1;
            return slotDatabaseMatcher.match(slot);
        }catch (Throwable t){
            throw new HashSlotRouteException(String.format("failed to calculate slot, value:%s", key), t);
        }
    }
}
