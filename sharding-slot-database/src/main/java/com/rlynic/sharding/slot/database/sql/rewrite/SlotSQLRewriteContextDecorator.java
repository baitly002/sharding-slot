package com.rlynic.sharding.slot.database.sql.rewrite;

import com.rlynic.sharding.slot.database.configuration.SlotShardingProperties;
import com.rlynic.sharding.slot.database.sql.rewrite.parameter.ShardingSlotParameterRewriterBuilder;
import org.apache.shardingsphere.infra.binder.statement.dml.InsertStatementContext;
import org.apache.shardingsphere.infra.config.props.ConfigurationProperties;
import org.apache.shardingsphere.infra.rewrite.context.SQLRewriteContext;
import org.apache.shardingsphere.infra.rewrite.context.SQLRewriteContextDecorator;
import org.apache.shardingsphere.infra.rewrite.parameter.rewriter.ParameterRewriter;
import org.apache.shardingsphere.infra.route.context.RouteContext;
import org.apache.shardingsphere.sharding.rule.ShardingRule;

import java.util.Collection;

public class SlotSQLRewriteContextDecorator implements SQLRewriteContextDecorator<ShardingRule> {

    protected SlotShardingProperties slotShardingProperties;

    @Override
    public void decorate(ShardingRule shardingRule, ConfigurationProperties configurationProperties, SQLRewriteContext sqlRewriteContext, RouteContext routeContext) {

//        try {
//            if (sqlRewriteContext.getSqlStatementContext() instanceof InsertStatementContext) {
//
//                SQLParserEngine sqlParserEngine = new SQLParserEngine("MySQL", new CacheOption(10, 20));
//                sqlParserEngine.parse(sqlRewriteContext.getSql(), true);
//                sqlRewriteContext.generateSQLTokens();
//                InsertStatementContext insertStatementContext = (InsertStatementContext) sqlRewriteContext.getSqlStatementContext();
//                if (null == slotShardingProperties) {
//                    slotShardingProperties = ShardingAutoConfiguration.context.getBean(SlotShardingProperties.class);
//                }
//                String tableName = insertStatementContext.getSqlStatement().getTable().getTableName().getIdentifier().getValue();
//                List<String> insertColumns = insertStatementContext.getInsertColumnNames();
//                insertColumns.add("slot");
//                List<InsertValueContext> insertValues = insertStatementContext.getInsertValueContexts();
////            insertValues.add(new InsertValueContext());
//
//                List<Integer> slotsContext = SlotContextHolder.get();
//
//                if (CollectionUtils.isEmpty(slotsContext)) {
//                    return;
//                }
//                Iterator<Integer> slots = slotsContext.iterator();
//                int count = 0;
//
//                List<String> columnNames = insertStatementContext.getColumnNames();
//                int cIndex = columnNames.indexOf(slotShardingProperties.getColumn()) + 1;
//                for (List<Object> each : insertStatementContext.getGroupedParameters()) {
//                    if (cIndex <= 0) {
//                        cIndex = sqlRewriteContext.getParameterBuilder().getParameters().size();
//                    }
//
//                    Comparable<?> generatedValue = slots.next();
//                    if (!each.isEmpty()) {
//                        GroupedParameterBuilder builder = (GroupedParameterBuilder) sqlRewriteContext.getParameterBuilder();
//                        builder.getParameterBuilders().get(count).addAddedParameters(cIndex, Lists.newArrayList(generatedValue));
////                    sqlRewriteContext.getParameterBuilder().getParameters().add(cIndex, generatedValue);
//                    }
//                    count++;
//                }
//
//                count = 0;
//                for (InsertValueContext each : insertStatementContext.getInsertValueContexts()) {
////                InsertValue insertValueToken = result.get().getInsertValues().get(count);
//                    DerivedSimpleExpressionSegment expressionSegment = isToAddDerivedLiteralExpression(insertStatementContext, count)
//                            ? new DerivedLiteralExpressionSegment(slots.next()) : new DerivedParameterMarkerExpressionSegment(each.getParameterCount());
////                GroupedParameterBuilder builder = (GroupedParameterBuilder)sqlRewriteContext.getParameterBuilder();
////                insertStatementContext.getValueExpressions().get(count).add(expressionSegment);
//                    each.getValueExpressions().add(expressionSegment);
////                insertValueToken.getValues().add(expressionSegment);
////                System.out.println(each.getParameterIndex(0));
//                    count++;
//                }
////            System.out.println(insertStatementContext.getColumnNames());
//            }
//        }catch (Exception e){
//
//        }finally {
//            SlotContextHolder.clear();
//        }

        if (routeContext.isFederated()) {
            return;
        }
        if (!sqlRewriteContext.getParameters().isEmpty()) {
            Collection<ParameterRewriter> parameterRewriters = new ShardingSlotParameterRewriterBuilder(shardingRule,
                    routeContext, sqlRewriteContext.getSchemas(), sqlRewriteContext.getSqlStatementContext()).getParameterRewriters();
            rewriteParameters(sqlRewriteContext, parameterRewriters);
        }
        sqlRewriteContext.addSQLTokenGenerators(new SlotTokenGenerateBuilder(shardingRule, routeContext, sqlRewriteContext.getSqlStatementContext()).getSQLTokenGenerators());

        //最终生成的语句操作在 -> SQLRewriteEntry.rewrite()
//        ShardingSQLRouter
//        ShardingSQLRouter
//        ShardingResultMergerEngine
    }
    private boolean isToAddDerivedLiteralExpression(final InsertStatementContext insertStatementContext, final int insertValueCount) {
        return insertStatementContext.getGroupedParameters().get(insertValueCount).isEmpty();
    }

    private void rewriteParameters(final SQLRewriteContext sqlRewriteContext, final Collection<ParameterRewriter> parameterRewriters) {
        for (ParameterRewriter each : parameterRewriters) {
            each.rewrite(sqlRewriteContext.getParameterBuilder(), sqlRewriteContext.getSqlStatementContext(), sqlRewriteContext.getParameters());
        }
    }

    @Override
    public int getOrder() {
        return 0;
    }

    @Override
    public Class<ShardingRule> getTypeClass() {
        return ShardingRule.class;
    }

}
