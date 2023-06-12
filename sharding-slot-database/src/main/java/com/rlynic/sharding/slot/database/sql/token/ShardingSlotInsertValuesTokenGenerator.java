package com.rlynic.sharding.slot.database.sql.token;

import com.google.common.base.Preconditions;
import lombok.Setter;
import org.apache.shardingsphere.infra.binder.segment.insert.values.InsertValueContext;
import org.apache.shardingsphere.infra.binder.segment.insert.values.expression.DerivedLiteralExpressionSegment;
import org.apache.shardingsphere.infra.binder.segment.insert.values.expression.DerivedParameterMarkerExpressionSegment;
import org.apache.shardingsphere.infra.binder.segment.insert.values.expression.DerivedSimpleExpressionSegment;
import org.apache.shardingsphere.infra.binder.statement.dml.InsertStatementContext;
import org.apache.shardingsphere.infra.rewrite.sql.token.generator.aware.PreviousSQLTokensAware;
import org.apache.shardingsphere.infra.rewrite.sql.token.pojo.SQLToken;
import org.apache.shardingsphere.infra.rewrite.sql.token.pojo.generic.InsertValue;
import org.apache.shardingsphere.infra.rewrite.sql.token.pojo.generic.InsertValuesToken;
import org.apache.shardingsphere.sharding.rule.ShardingRule;
import org.apache.shardingsphere.sql.parser.sql.common.segment.dml.column.InsertColumnsSegment;

import java.util.List;
import java.util.Optional;


@Setter
public final class ShardingSlotInsertValuesTokenGenerator extends AbstractBaseSlotTokenGenerator implements PreviousSQLTokensAware {

    private List<SQLToken> previousSQLTokens;
    private ShardingRule shardingRule;

    public ShardingSlotInsertValuesTokenGenerator(ShardingRule shardingRule) {
        this.shardingRule = shardingRule;
    }

    @Override
    protected boolean isGenerateSQLToken(final InsertStatementContext insertStatementContext) {
        Optional<InsertColumnsSegment> sqlSegment = insertStatementContext.getSqlStatement().getInsertColumns();
        return sqlSegment.isPresent() && !insertStatementContext.getSqlStatement().getValues().isEmpty()
                && !insertStatementContext.getColumnNames().contains(slotShardingProperties.getColumn())
                && shardingRule.getAllTables().contains(insertStatementContext.getSqlStatement().getTable().getTableName().getIdentifier().getValue())
                && "HASH_SLOT".equalsIgnoreCase(shardingRule.getShardingAlgorithms().get(shardingRule
                .getTableRule(insertStatementContext.getSqlStatement().getTable().getTableName().getIdentifier().getValue())
                .getDatabaseShardingStrategyConfig().getShardingAlgorithmName()).getType());
    }

    @Override
    public SQLToken generateSQLToken(final InsertStatementContext insertStatementContext) {
        Optional<InsertValuesToken> result = findPreviousSQLToken();
        Preconditions.checkState(result.isPresent());
        int count = 0;
        List<List<Object>> parameters = insertStatementContext.getGroupedParameters();
        for (InsertValueContext each : insertStatementContext.getInsertValueContexts()) {
            InsertValue insertValueToken = result.get().getInsertValues().get(count);
            //----start-------兼容GeneratedKey注入
            int parameterCount = each.getParameterCount();
            if(insertStatementContext.getGeneratedKeyContext().isPresent()){
                DerivedSimpleExpressionSegment expressionSegment = isToAddDerivedLiteralExpression(parameters, count)
//                        ? new DerivedLiteralExpressionSegment(each.getValue(count))
                        ? new DerivedLiteralExpressionSegment(each.getLiteralValue(count))
                        : new DerivedParameterMarkerExpressionSegment(parameterCount);
                insertValueToken.getValues().add(expressionSegment);
                parameterCount+=1;
            }
            //----end-------

            DerivedSimpleExpressionSegment expressionSegment = isToAddDerivedLiteralExpression(parameters, count)
//                    ? new DerivedLiteralExpressionSegment(each.getValue(count))
                    ? new DerivedLiteralExpressionSegment(each.getLiteralValue(count))
                    : new DerivedParameterMarkerExpressionSegment(parameterCount);
            insertValueToken.getValues().add(expressionSegment);
            count++;
        }
        return result.get();
    }

    private Optional<InsertValuesToken> findPreviousSQLToken() {
        for (SQLToken each : previousSQLTokens) {
            if (each instanceof SlotInsertValuesToken) {
                return Optional.of((InsertValuesToken) each);
            }
        }
        return Optional.empty();
    }

    private boolean isToAddDerivedLiteralExpression(final List<List<Object>> parameters, final int insertValueCount) {
        return parameters.get(insertValueCount).isEmpty();
    }
}
