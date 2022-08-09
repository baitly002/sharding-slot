package com.rlynic.sharding.slot.database.sql.token;

import com.google.common.base.Preconditions;
import org.apache.shardingsphere.infra.binder.statement.dml.InsertStatementContext;
import org.apache.shardingsphere.sharding.rewrite.token.generator.impl.keygen.BaseGeneratedKeyTokenGenerator;
import org.apache.shardingsphere.sql.parser.sql.common.segment.dml.column.InsertColumnsSegment;

import java.util.Optional;

public final class ShardingSlotInsertColumnTokenGenerator extends BaseGeneratedKeyTokenGenerator {
    public final String SLOT_KEY = "slot";
    @Override
    protected boolean isGenerateSQLToken(final InsertStatementContext insertStatementContext) {
        Optional<InsertColumnsSegment> sqlSegment = insertStatementContext.getSqlStatement().getInsertColumns();
        return sqlSegment.isPresent() && !sqlSegment.get().getColumns().isEmpty();
//        return sqlSegment.isPresent() && !sqlSegment.get().getColumns().isEmpty()
//                && insertStatementContext.getGeneratedKeyContext().isPresent()
//                && !insertStatementContext.getGeneratedKeyContext().get().getGeneratedValues().isEmpty();
    }

    @Override
    public SlotInsertColumnToken generateSQLToken(final InsertStatementContext insertStatementContext) {
//        Optional<GeneratedKeyContext> generatedKey = insertStatementContext.getGeneratedKeyContext();
//        Preconditions.checkState(generatedKey.isPresent());
        Optional<InsertColumnsSegment> sqlSegment = insertStatementContext.getSqlStatement().getInsertColumns();
        Preconditions.checkState(sqlSegment.isPresent());
//        return new GeneratedKeyInsertColumnToken(sqlSegment.get().getStopIndex(), generatedKey.get().getColumnName());
        return new SlotInsertColumnToken(sqlSegment.get().getStopIndex(), SLOT_KEY);
    }
}