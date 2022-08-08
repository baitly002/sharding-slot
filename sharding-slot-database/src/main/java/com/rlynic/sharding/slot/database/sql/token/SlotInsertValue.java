package com.rlynic.sharding.slot.database.sql.token;

import lombok.Getter;
import org.apache.shardingsphere.infra.datanode.DataNode;
import org.apache.shardingsphere.infra.rewrite.sql.token.pojo.generic.InsertValue;
import org.apache.shardingsphere.sql.parser.sql.common.segment.dml.expr.ExpressionSegment;

import java.util.Collection;
import java.util.List;

@Getter
public final class SlotInsertValue extends InsertValue {

    private final Collection<DataNode> dataNodes;

    public SlotInsertValue(final List<ExpressionSegment> values, final Collection<DataNode> dataNodes) {
        super(values);
        this.dataNodes = dataNodes;
    }
}
