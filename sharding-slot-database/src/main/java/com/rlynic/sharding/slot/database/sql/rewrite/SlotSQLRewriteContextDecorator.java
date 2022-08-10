package com.rlynic.sharding.slot.database.sql.rewrite;

import com.rlynic.sharding.slot.database.configuration.SlotShardingProperties;
import com.rlynic.sharding.slot.database.sql.rewrite.parameter.ShardingSlotParameterRewriterBuilder;
import org.apache.shardingsphere.infra.config.props.ConfigurationProperties;
import org.apache.shardingsphere.infra.rewrite.context.SQLRewriteContext;
import org.apache.shardingsphere.infra.rewrite.context.SQLRewriteContextDecorator;
import org.apache.shardingsphere.infra.rewrite.parameter.rewriter.ParameterRewriter;
import org.apache.shardingsphere.infra.route.context.RouteContext;
import org.apache.shardingsphere.sharding.rule.ShardingRule;

import java.util.Collection;

/**
 * sql改写SPI扩展点
 */
public class SlotSQLRewriteContextDecorator implements SQLRewriteContextDecorator<ShardingRule> {

    protected SlotShardingProperties slotShardingProperties;

    @Override
    public void decorate(ShardingRule shardingRule, ConfigurationProperties configurationProperties, SQLRewriteContext sqlRewriteContext, RouteContext routeContext) {
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
        //RouteSQLRewriteEngine
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
