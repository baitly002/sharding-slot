package com.rlynic.sharding.slot.database.sql.rewrite.parameter;

import org.apache.shardingsphere.infra.binder.statement.SQLStatementContext;
import org.apache.shardingsphere.infra.metadata.database.schema.decorator.model.ShardingSphereSchema;
import org.apache.shardingsphere.infra.rewrite.parameter.rewriter.ParameterRewriter;
import org.apache.shardingsphere.infra.rewrite.parameter.rewriter.ParameterRewriterBuilder;
import org.apache.shardingsphere.infra.rewrite.sql.token.generator.aware.RouteContextAware;
import org.apache.shardingsphere.infra.rewrite.sql.token.generator.aware.SchemaMetaDataAware;
import org.apache.shardingsphere.infra.route.context.RouteContext;
import org.apache.shardingsphere.sharding.rule.ShardingRule;
import org.apache.shardingsphere.sharding.rule.aware.ShardingRuleAware;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;

/**
 * 注入字段值入口
 */
public final class ShardingInParameterRewriterBuilder implements ParameterRewriterBuilder {

    private final ShardingRule shardingRule;

    private final RouteContext routeContext;

    private final Map<String, ShardingSphereSchema> schemas;

    private final SQLStatementContext<?> sqlStatementContext;

    public ShardingInParameterRewriterBuilder(ShardingRule shardingRule, RouteContext routeContext, Map<String, ShardingSphereSchema> schemas, SQLStatementContext<?> sqlStatementContext) {
        this.shardingRule = shardingRule;
        this.routeContext = routeContext;
        this.schemas = schemas;
        this.sqlStatementContext = sqlStatementContext;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public Collection<ParameterRewriter> getParameterRewriters() {
        Collection<ParameterRewriter> result = new LinkedList<>();
        addParameterRewriter(result, new ShardingInValueParameterRewriter());
        return result;
    }

    @SuppressWarnings("rawtypes")
    private void addParameterRewriter(final Collection<ParameterRewriter> parameterRewriters, final ParameterRewriter toBeAddedParameterRewriter) {
        if (toBeAddedParameterRewriter instanceof SchemaMetaDataAware) {
            ((SchemaMetaDataAware) toBeAddedParameterRewriter).setSchemas(schemas);
        }
        if (toBeAddedParameterRewriter instanceof ShardingRuleAware) {
            ((ShardingRuleAware) toBeAddedParameterRewriter).setShardingRule(shardingRule);
        }
        if (toBeAddedParameterRewriter instanceof RouteContextAware) {
            ((RouteContextAware) toBeAddedParameterRewriter).setRouteContext(routeContext);
        }
        if (toBeAddedParameterRewriter.isNeedRewrite(sqlStatementContext)) {
            parameterRewriters.add(toBeAddedParameterRewriter);
        }
    }
}
