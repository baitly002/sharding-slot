package com.rlynic.sharding.slot.database.sql.rewrite.parameter;

import lombok.Setter;
import org.apache.shardingsphere.infra.binder.segment.select.pagination.PaginationContext;
import org.apache.shardingsphere.infra.binder.statement.SQLStatementContext;
import org.apache.shardingsphere.infra.binder.statement.dml.SelectStatementContext;
import org.apache.shardingsphere.infra.rewrite.parameter.builder.ParameterBuilder;
import org.apache.shardingsphere.infra.rewrite.parameter.builder.impl.StandardParameterBuilder;
import org.apache.shardingsphere.infra.rewrite.parameter.rewriter.ParameterRewriter;
import org.apache.shardingsphere.infra.rewrite.sql.token.generator.aware.RouteContextAware;
import org.apache.shardingsphere.infra.route.context.RouteContext;

import java.util.List;

@Setter
public final class ShardingPaginationSlotParameterRewriter implements ParameterRewriter<SelectStatementContext>, RouteContextAware {

    private RouteContext routeContext;

    @Override
    public boolean isNeedRewrite(final SQLStatementContext<?> sqlStatementContext) {
        return true;
//        return sqlStatementContext instanceof SelectStatementContext
//                && ((SelectStatementContext) sqlStatementContext).getPaginationContext().isHasPagination() && !routeContext.isSingleRouting();
    }

    @Override
    public void rewrite(final ParameterBuilder parameterBuilder, final SelectStatementContext selectStatementContext, final List<Object> parameters) {
        PaginationContext pagination = selectStatementContext.getPaginationContext();
        pagination.getOffsetParameterIndex().ifPresent(optional -> rewriteOffset(pagination, optional, (StandardParameterBuilder) parameterBuilder));
        pagination.getRowCountParameterIndex()
                .ifPresent(optional -> rewriteRowCount(pagination, optional, (StandardParameterBuilder) parameterBuilder, selectStatementContext));
    }

    private void rewriteOffset(final PaginationContext pagination, final int offsetParameterIndex, final StandardParameterBuilder parameterBuilder) {
        parameterBuilder.addReplacedParameters(offsetParameterIndex, pagination.getRevisedOffset());
    }

    private void rewriteRowCount(final PaginationContext pagination,
                                 final int rowCountParameterIndex, final StandardParameterBuilder parameterBuilder, final SQLStatementContext sqlStatementContext) {
        parameterBuilder.addReplacedParameters(rowCountParameterIndex, pagination.getRevisedRowCount((SelectStatementContext) sqlStatementContext));
    }
}