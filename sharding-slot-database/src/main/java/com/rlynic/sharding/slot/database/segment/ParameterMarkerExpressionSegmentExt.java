package com.rlynic.sharding.slot.database.segment;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.apache.shardingsphere.sql.parser.sql.common.segment.dml.expr.ExpressionSegment;
import org.apache.shardingsphere.sql.parser.sql.common.segment.dml.expr.simple.ParameterMarkerExpressionSegment;

import java.util.Optional;

@Getter
@ToString
@EqualsAndHashCode
public class ParameterMarkerExpressionSegmentExt extends ParameterMarkerExpressionSegment {

    private ExpressionSegment left;
    private Optional<String> aliasValue;
    private ParameterMarkerExpressionSegment parameterMarkerExpressionSegment;

    public ParameterMarkerExpressionSegmentExt(final int startIndex, final int stopIndex, final int parameterMarkerIndex, ExpressionSegment left){
        super(startIndex, stopIndex, parameterMarkerIndex);
        this.left = left;
    }

    public ParameterMarkerExpressionSegmentExt(ExpressionSegment left, ParameterMarkerExpressionSegment parameterMarkerExpressionSegment){
        super(parameterMarkerExpressionSegment.getStartIndex(), parameterMarkerExpressionSegment.getStopIndex(), parameterMarkerExpressionSegment.getParameterMarkerIndex());
        this.parameterMarkerExpressionSegment = parameterMarkerExpressionSegment;
        this.left = left;
        this.aliasValue = parameterMarkerExpressionSegment.getAlias();
    }
}
