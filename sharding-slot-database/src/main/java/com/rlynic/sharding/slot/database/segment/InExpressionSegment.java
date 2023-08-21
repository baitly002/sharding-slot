package com.rlynic.sharding.slot.database.segment;

import lombok.Data;
import org.apache.shardingsphere.sql.parser.sql.common.segment.dml.expr.ExpressionSegment;

import java.util.Collection;
import java.util.List;

@Data
public class InExpressionSegment {
    private List<InColumnSegment> columnSegmentList;
    private Collection<ExpressionSegment> parameterMarkerText;
}
