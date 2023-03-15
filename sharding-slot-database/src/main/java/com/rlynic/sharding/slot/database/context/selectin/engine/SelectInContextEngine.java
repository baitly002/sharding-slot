package com.rlynic.sharding.slot.database.context.selectin.engine;

import com.rlynic.sharding.slot.database.context.selectin.SelectInContext;
import com.rlynic.sharding.slot.database.util.SqlExpressionExtractUtil;
import org.apache.shardingsphere.sql.parser.sql.common.segment.dml.expr.ExpressionSegment;
import org.apache.shardingsphere.sql.parser.sql.common.segment.dml.expr.InExpression;
import org.apache.shardingsphere.sql.parser.sql.common.segment.dml.predicate.WhereSegment;
import org.apache.shardingsphere.sql.parser.sql.common.statement.dml.SelectStatement;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;

public class SelectInContextEngine {

    public SelectInContext createSelectInContext(final SelectStatement selectStatement) {
        if (!selectStatement.getWhere().isPresent()) {
            return new SelectInContext(new LinkedList<>());
        }else{
            WhereSegment whereSegment = selectStatement.getWhere().get();
            return new SelectInContext(getInExpressions(Collections.singletonList(whereSegment)));
        }
    }

    public Collection<InExpression> getInExpressions(Collection<WhereSegment> whereSegments) {
        Collection<ExpressionSegment> expressions = new LinkedList<>();
        for (WhereSegment each : whereSegments) {
            expressions.add(each.getExpr());
        }
        return SqlExpressionExtractUtil.getInExpressions(expressions);
    }
}
