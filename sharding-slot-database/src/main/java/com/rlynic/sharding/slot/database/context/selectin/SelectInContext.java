package com.rlynic.sharding.slot.database.context.selectin;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.shardingsphere.sql.parser.sql.common.segment.dml.expr.InExpression;

import java.util.Collection;

@RequiredArgsConstructor
@Getter
public class SelectInContext {

    private final Collection<InExpression> inExpressions;
}
