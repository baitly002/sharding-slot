package com.rlynic.sharding.slot.database.sql.token;

import org.apache.shardingsphere.infra.binder.statement.SQLStatementContext;
import org.apache.shardingsphere.infra.binder.statement.dml.InsertStatementContext;
import org.apache.shardingsphere.infra.rewrite.sql.token.generator.OptionalSQLTokenGenerator;
import org.apache.shardingsphere.sharding.rewrite.token.generator.impl.keygen.BaseGeneratedKeyTokenGenerator;
import org.apache.shardingsphere.sql.parser.sql.common.segment.dml.column.InsertColumnsSegment;

import java.util.Optional;

public final class SlotInsertColumnTokenGenerator implements OptionalSQLTokenGenerator<InsertStatementContext> {
//    @Override
//    protected boolean isGenerateSQLToken(final InsertStatementContext insertStatementContext) {
//        Optional<InsertColumnsSegment> sqlSegment = insertStatementContext.getSqlStatement().getInsertColumns();
//        return sqlSegment.isPresent() && !sqlSegment.get().getColumns().isEmpty()
//                && insertStatementContext.getGeneratedKeyContext().isPresent()
//                && !insertStatementContext.getGeneratedKeyContext().get().getGeneratedValues().isEmpty();
//        return true;
//    }

    @Override
    public SlotInsertColumnToken generateSQLToken(final InsertStatementContext insertStatementContext) {
//        Optional<GeneratedKeyContext> generatedKey = insertStatementContext.getGeneratedKeyContext();
//        Preconditions.checkState(generatedKey.isPresent());
        Optional<InsertColumnsSegment> sqlSegment = insertStatementContext.getSqlStatement().getInsertColumns();
//        Preconditions.checkState(sqlSegment.isPresent());
        return new SlotInsertColumnToken(sqlSegment.get().getStopIndex(), "slot");
    }

    @Override
    public boolean isGenerateSQLToken(SQLStatementContext<?> sqlStatementContext) {
        return true;
    }
}
