package com.rlynic.sharding.slot.database.sql.token;

import com.rlynic.sharding.slot.database.CRC16;
import com.rlynic.sharding.slot.database.configuration.SlotShardingProperties;
import com.rlynic.sharding.slot.database.context.selectin.engine.SelectInContextEngine;
import com.rlynic.sharding.slot.database.segment.InColumnSegment;
import com.rlynic.sharding.slot.database.segment.SegmentUtil;
import com.rlynic.sharding.slot.database.segment.TableMapping;
import com.rlynic.sharding.slot.database.strategy.HashSlotRouteException;
import com.rlynic.sharding.slot.database.strategy.SlotDatabaseMatcher;
import com.rlynic.sharding.slot.database.util.SpringBeanUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.shardingsphere.infra.binder.statement.SQLStatementContext;
import org.apache.shardingsphere.infra.binder.statement.dml.DeleteStatementContext;
import org.apache.shardingsphere.infra.binder.statement.dml.SelectStatementContext;
import org.apache.shardingsphere.infra.binder.statement.dml.UpdateStatementContext;
import org.apache.shardingsphere.infra.rewrite.context.SQLRewriteContext;
import org.apache.shardingsphere.infra.rewrite.engine.RemoveParameterMarkerHolder;
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
import org.apache.shardingsphere.sql.parser.sql.common.segment.dml.expr.complex.CommonExpressionSegment;
import org.apache.shardingsphere.sql.parser.sql.common.segment.dml.expr.simple.LiteralExpressionSegment;
import org.apache.shardingsphere.sql.parser.sql.common.segment.dml.expr.simple.ParameterMarkerExpressionSegment;
import org.apache.shardingsphere.sql.parser.sql.common.segment.dml.predicate.WhereSegment;
import org.apache.shardingsphere.sql.parser.sql.common.segment.generic.ParameterMarkerSegment;
import org.apache.shardingsphere.sql.parser.sql.common.segment.generic.table.SimpleTableSegment;
import org.apache.shardingsphere.sql.parser.sql.common.statement.AbstractSQLStatement;
import org.apache.shardingsphere.sql.parser.sql.common.statement.SQLStatement;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
        if(SpringBeanUtil.getApplicationContext()!=null) {
            this.slotShardingProperties = SpringBeanUtil.getBean(SlotShardingProperties.class);
            this.slotDatabaseMatcher = SpringBeanUtil.getBean(SlotDatabaseMatcher.class);
        }
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
                    TableMapping tableMapping = createTableMapping(sqlStatementContext);
                    List<InColumnSegment> inColumnSegments = SegmentUtil.parserInColumn(inExpression);
                    //是否分库分表
                    boolean matchShardingStrategy = matchShardingStrategy(sqlStatementContext, inExpression, tableMapping, inColumnSegments);
                    //是否in操作
                    boolean matchSingleSelectIn = matchSingleSelectIn(inExpression);
                    if(matchShardingStrategy && matchSingleSelectIn) {
                        result.addAll(shardingIn(inExpression, sqlRewriteContext.getParameters(), inColumnSegments));
                    }
                }
            }
        }
        return result;
    }

    public Collection<ParameterMarkerSegment> getParameterMarkerSegments(){
        Collection<ParameterMarkerSegment> parameterMarkerSegments = null;
        SQLStatement sqlStatement = sqlRewriteContext.getSqlStatementContext().getSqlStatement();
        if(sqlStatement instanceof AbstractSQLStatement){
            parameterMarkerSegments = ((AbstractSQLStatement) sqlStatement).getParameterMarkerSegments();
        }
        return parameterMarkerSegments;
    }

    public Collection<RemoveInToken> shardingIn(InExpression inExpression, List<Object> parameters, List<InColumnSegment> inColumnSegments){
        //removeInTokens 无需有值，因为这是给外层统一rewrite的，而非针对sharding后具体库的sql语句rewrite
        Collection<RemoveInToken> removeInTokens = new ArrayList<>();
        if(parameters.isEmpty()){
            return removeInTokens;
        }
        Collection<ParameterMarkerSegment> parameterMarkerSegments = getParameterMarkerSegments();
        inExpression.getExpressionList().forEach(item -> {
            String routedb = "";
            if(item instanceof ParameterMarkerExpressionSegment){
                int mi = ((ParameterMarkerExpressionSegment) item).getParameterMarkerIndex();
                routedb = match(parameters.get(mi));
            }
            if(item instanceof LiteralExpressionSegment){
                routedb = match(((LiteralExpressionSegment) item).getLiterals());
            }
            if(item instanceof CommonExpressionSegment && parameterMarkerSegments != null){
                //((?,?,?),(?,?,?),(?,?,?)) 方式
                List<ParameterMarkerSegment> itemParameterMarkers = parameterMarkerSegments.stream().filter(p -> p.getStartIndex()>item.getStartIndex())
                        .filter(p -> p.getStopIndex()< item.getStopIndex()).collect(Collectors.toList());
                for(InColumnSegment inColumnSegment : inColumnSegments){
                    if(inColumnSegment.getShardingKey()){
                        ParameterMarkerSegment parameterMarkerSegment = itemParameterMarkers.get(inColumnSegment.getIndex());
                        if(parameterMarkerSegment instanceof ParameterMarkerExpressionSegment){
                            int mi = ((ParameterMarkerExpressionSegment) parameterMarkerSegment).getParameterMarkerIndex();
                            routedb = match(parameters.get(mi));
                        }
                    }
                }
            }
            if(StringUtils.isNotBlank(routedb)){
                RemoveParameterMarkerHolder.addKeepdb(routedb);
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
            return item instanceof ParameterMarkerExpressionSegment || item instanceof LiteralExpressionSegment || item instanceof CommonExpressionSegment;
        }
        return false;
    }

    public TableMapping createTableMapping(final SQLStatementContext<?> sqlStatementContext){
        TableMapping tableMapping = new TableMapping();
        Map<String, String> mapping = new HashMap<>();
        Collection<SimpleTableSegment> tableSegments = null;
        if(sqlStatementContext instanceof UpdateStatementContext){
            tableSegments = ((UpdateStatementContext) sqlStatementContext).getAllTables();
        }
        if(sqlStatementContext instanceof DeleteStatementContext){
            tableSegments = ((DeleteStatementContext) sqlStatementContext).getAllTables();
        }
        if(sqlStatementContext instanceof SelectStatementContext){
            tableSegments = ((SelectStatementContext) sqlStatementContext).getAllTables();
        }

        tableSegments.forEach(table -> {
            table.getAlias().ifPresent(alias -> {
                mapping.put(alias, table.getTableName().getIdentifier().getValue());
            });
        });
        ShardingRuleConfiguration ruleConfiguration = (ShardingRuleConfiguration) shardingRule.getConfiguration();
        ruleConfiguration.getTables().forEach(tb -> {
            StandardShardingStrategyConfiguration shardingStrategyConfiguration = (StandardShardingStrategyConfiguration)tb.getDatabaseShardingStrategy();
            mapping.put(tb.getLogicTable() +"."+ shardingStrategyConfiguration.getShardingColumn(), "true");
        });
        tableMapping.setMapping(mapping);
        tableMapping.setTableSegments(tableSegments);
        return tableMapping;
    }

    public boolean matchShardingStrategy(final SQLStatementContext<?> sqlStatementContext, InExpression inExpression, TableMapping tableMapping, List<InColumnSegment> inColumnSegments){
        //TODO IN查询字段匹配规则后续再优化
        if(inExpression.isNot()){
            //not in 查询不进行重写
            return false;
        }
        return matchSharding(tableMapping, inColumnSegments);
    }
    public boolean matchSharding(TableMapping tableMapping, List<InColumnSegment> inColumnSegments){
        boolean flag = false;
        for(SimpleTableSegment table : tableMapping.getTableSegments()){
            for(InColumnSegment inColumnSegment : inColumnSegments){
                String match = "";
                if(StringUtils.isBlank(inColumnSegment.getAliasName())){
                    match = tableMapping.getMapping().get(table.getTableName().getIdentifier().getValue() + "." + inColumnSegment.getColumnName());
                }else{
                    match = tableMapping.getMapping().get(tableMapping.getMapping().get(inColumnSegment.getAliasName()) + "." + inColumnSegment.getColumnName());
                }
                if("true".equalsIgnoreCase(match)) {
                    inColumnSegment.setShardingKey(true);
                    flag = true;
                }
            }
        }
        return flag;
    }

    public boolean matchSharding(Collection<SimpleTableSegment> tableSegments, Map<String, String> tableMapping, String keyPrefix, ExpressionSegment left){
        for(SimpleTableSegment table : tableSegments){
            String match = "";
            if(left instanceof ColumnSegment) {
                //单字段的in操作
                if (keyPrefix == null) {
                    match = tableMapping.get(table.getTableName().getIdentifier().getValue() + "." + ((ColumnSegment) left).getQualifiedName());
                } else {
                    match = tableMapping.get(keyPrefix + "." + ((ColumnSegment) left).getIdentifier().getValue());
                }
            }
            if(left instanceof CommonExpressionSegment){
                //多字段的in操作
                String leftText = ((CommonExpressionSegment) left).getText();
                String leftColumns = leftText.substring(1,leftText.length()-1);
                for(String column : leftColumns.split(",")) {
                    column = column.trim();
                    if(column.indexOf(".")>0){
                        //有别名
                        String[] aliasColumn = column.split("\\.");
                        if(aliasColumn.length>1) {
                            keyPrefix = tableMapping.get(aliasColumn[0]);
                            column = aliasColumn[1];
                        }
                    }
                    if (keyPrefix == null) {
                        match = tableMapping.get(table.getTableName().getIdentifier().getValue() + "." + column);
                    } else {
                        match = tableMapping.get(keyPrefix + "." + column);
                    }
                    if("true".equalsIgnoreCase(match)) return true;
                }
            }
            if("true".equalsIgnoreCase(match)) return true;
        }
        return false;
    }

    public String match(Object key){
        try{
            //TODO 目前仅支持hash-slot算法的in语句重写，其他算法暂不支持
            String s = String.valueOf(key);
            int slot = CRC16.CRC16_CCITT(s.getBytes()) & slotShardingProperties.getNumber() - 1;
            return slotDatabaseMatcher.match(slot);
        }catch (Throwable t){
            throw new HashSlotRouteException(String.format("failed to calculate slot, value:%s", key), t);
        }
    }
}
