package com.rlynic.sharding.slot.database.sql.token;

import com.google.common.base.Preconditions;
import org.apache.shardingsphere.infra.binder.statement.dml.InsertStatementContext;
import org.apache.shardingsphere.sql.parser.sql.common.segment.dml.column.InsertColumnsSegment;

import java.util.Optional;

public final class ShardingSlotInsertColumnTokenGenerator extends AbstractBaseSlotTokenGenerator {
    @Override
    protected boolean isGenerateSQLToken(final InsertStatementContext insertStatementContext) {
        Optional<InsertColumnsSegment> sqlSegment = insertStatementContext.getSqlStatement().getInsertColumns();
        return sqlSegment.isPresent() && !sqlSegment.get().getColumns().isEmpty()
                && !insertStatementContext.getColumnNames().contains(slotShardingProperties.getColumn())
                && slotShardingProperties.getTableNames().contains(insertStatementContext.getSqlStatement().getTable().getTableName().getIdentifier().getValue());
    }

    @Override
    public SlotInsertColumnToken generateSQLToken(final InsertStatementContext insertStatementContext) {
        Optional<InsertColumnsSegment> sqlSegment = insertStatementContext.getSqlStatement().getInsertColumns();
        Preconditions.checkState(sqlSegment.isPresent());
        return new SlotInsertColumnToken(sqlSegment.get().getStopIndex(), slotShardingProperties.getColumn());
    }
}