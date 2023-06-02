package com.rlynic.sharding.slot.database.sql.rewrite.parameter;

import lombok.RequiredArgsConstructor;
import org.apache.shardingsphere.infra.binder.statement.SQLStatementContext;
import org.apache.shardingsphere.infra.metadata.database.schema.model.ShardingSphereSchema;
import org.apache.shardingsphere.infra.rewrite.parameter.rewriter.ParameterRewriter;
import org.apache.shardingsphere.infra.rewrite.parameter.rewriter.ParameterRewriterBuilder;
import org.apache.shardingsphere.infra.rewrite.sql.token.generator.aware.RouteContextAware;
import org.apache.shardingsphere.infra.rewrite.sql.token.generator.aware.SchemaMetaDataAware;
import org.apache.shardingsphere.infra.route.context.RouteContext;
import org.apache.shardingsphere.sharding.rewrite.parameter.impl.ShardingGeneratedKeyInsertValueParameterRewriter;
import org.apache.shardingsphere.sharding.rewrite.parameter.impl.ShardingPaginationParameterRewriter;
import org.apache.shardingsphere.sharding.rule.ShardingRule;
import org.apache.shardingsphere.sharding.rule.aware.ShardingRuleAware;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;

@RequiredArgsConstructor
public final class GeneratedKeyParameterRewriterBuilder implements ParameterRewriterBuilder {

    private final ShardingRule shardingRule;

    private final RouteContext routeContext;

    private final Map<String, ShardingSphereSchema> schemas;

    private final SQLStatementContext<?> sqlStatementContext;

    @SuppressWarnings("rawtypes")
    @Override
    public Collection<ParameterRewriter> getParameterRewriters() {
        Collection<ParameterRewriter> result = new LinkedList<>();
        addParameterRewriter(result, new ShardingGeneratedKeyInsertValueParameterRewriter());
        addParameterRewriter(result, new ShardingPaginationParameterRewriter());
        return result;
    }

    @SuppressWarnings("rawtypes")
    private void addParameterRewriter(final Collection<ParameterRewriter> paramRewriters, final ParameterRewriter toBeAddedParamRewriter) {
        if (toBeAddedParamRewriter instanceof SchemaMetaDataAware) {
            ((SchemaMetaDataAware) toBeAddedParamRewriter).setSchemas(schemas);
        }
        if (toBeAddedParamRewriter instanceof ShardingRuleAware) {
            ((ShardingRuleAware) toBeAddedParamRewriter).setShardingRule(shardingRule);
        }
        if (toBeAddedParamRewriter instanceof RouteContextAware) {
            ((RouteContextAware) toBeAddedParamRewriter).setRouteContext(routeContext);
        }
        if (toBeAddedParamRewriter.isNeedRewrite(sqlStatementContext)) {
            paramRewriters.add(toBeAddedParamRewriter);
        }
    }
}
