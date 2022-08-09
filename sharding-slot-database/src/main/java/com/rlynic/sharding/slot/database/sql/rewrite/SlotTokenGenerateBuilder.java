package com.rlynic.sharding.slot.database.sql.rewrite;

import com.rlynic.sharding.slot.database.sql.token.*;
import lombok.RequiredArgsConstructor;
import org.apache.shardingsphere.infra.binder.statement.SQLStatementContext;
import org.apache.shardingsphere.infra.rewrite.sql.token.generator.SQLTokenGenerator;
import org.apache.shardingsphere.infra.rewrite.sql.token.generator.aware.RouteContextAware;
import org.apache.shardingsphere.infra.rewrite.sql.token.generator.builder.SQLTokenGeneratorBuilder;
import org.apache.shardingsphere.infra.route.context.RouteContext;
import org.apache.shardingsphere.sharding.rewrite.token.generator.IgnoreForSingleRoute;
import org.apache.shardingsphere.sharding.rewrite.token.generator.impl.*;
import org.apache.shardingsphere.sharding.rewrite.token.generator.impl.keygen.GeneratedKeyAssignmentTokenGenerator;
import org.apache.shardingsphere.sharding.rewrite.token.generator.impl.keygen.GeneratedKeyForUseDefaultInsertColumnsTokenGenerator;
import org.apache.shardingsphere.sharding.rewrite.token.generator.impl.keygen.GeneratedKeyInsertColumnTokenGenerator;
import org.apache.shardingsphere.sharding.rewrite.token.generator.impl.keygen.GeneratedKeyInsertValuesTokenGenerator;
import org.apache.shardingsphere.sharding.rule.ShardingRule;
import org.apache.shardingsphere.sharding.rule.aware.ShardingRuleAware;

import java.util.Collection;
import java.util.LinkedList;

@RequiredArgsConstructor
public final class SlotTokenGenerateBuilder implements SQLTokenGeneratorBuilder {

    private final ShardingRule shardingRule;

    private final RouteContext routeContext;

    private final SQLStatementContext<?> sqlStatementContext;

    @Override
    public Collection<SQLTokenGenerator> getSQLTokenGenerators() {
        Collection<SQLTokenGenerator> result = new LinkedList<>();
        addSQLTokenGenerator(result, new TableTokenGenerator());
        addSQLTokenGenerator(result, new DistinctProjectionPrefixTokenGenerator());
        addSQLTokenGenerator(result, new ProjectionsTokenGenerator());
        addSQLTokenGenerator(result, new OrderByTokenGenerator());
        addSQLTokenGenerator(result, new AggregationDistinctTokenGenerator());
        addSQLTokenGenerator(result, new IndexTokenGenerator());
        addSQLTokenGenerator(result, new ConstraintTokenGenerator());
        addSQLTokenGenerator(result, new OffsetTokenGenerator());
        addSQLTokenGenerator(result, new RowCountTokenGenerator());
//        addSQLTokenGenerator(result, new SlotInsertColumnTokenGenerator());
        addSQLTokenGenerator(result, new GeneratedKeyInsertColumnTokenGenerator());
        addSQLTokenGenerator(result, new GeneratedKeyForUseDefaultInsertColumnsTokenGenerator());
        addSQLTokenGenerator(result, new GeneratedKeyAssignmentTokenGenerator());
//        addSQLTokenGenerator(result, new SlotInsertValuesTokenGenerator());
        addSQLTokenGenerator(result, new GeneratedKeyInsertValuesTokenGenerator());
        addSQLTokenGenerator(result, new ShardingRemoveTokenGenerator());
        addSQLTokenGenerator(result, new CursorTokenGenerator());
//        result.add(new SlotInsertColumnTokenGenerator());
//        result.add(new SlotInsertValuesTokenGenerator());
        result.add(new ShardingSlotInsertColumnTokenGenerator());
        result.add(new TransformSlotInsertValuesTokenGenerator());
        result.add(new ShardingSlotInsertValuesTokenGenerator());
        return result;
    }

    private void addSQLTokenGenerator(final Collection<SQLTokenGenerator> sqlTokenGenerators, final SQLTokenGenerator toBeAddedSQLTokenGenerator) {
        if (toBeAddedSQLTokenGenerator instanceof IgnoreForSingleRoute && routeContext.isSingleRouting()) {
            return;
        }
        if (toBeAddedSQLTokenGenerator instanceof ShardingRuleAware) {
            ((ShardingRuleAware) toBeAddedSQLTokenGenerator).setShardingRule(shardingRule);
        }
        if (toBeAddedSQLTokenGenerator instanceof RouteContextAware) {
            ((RouteContextAware) toBeAddedSQLTokenGenerator).setRouteContext(routeContext);
        }
        if (toBeAddedSQLTokenGenerator.isGenerateSQLToken(sqlStatementContext)) {
            sqlTokenGenerators.add(toBeAddedSQLTokenGenerator);
        }
    }
}