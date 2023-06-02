package com.rlynic.sharding.slot.database.sql.token;

import com.google.common.base.Preconditions;
import lombok.Setter;
import org.apache.shardingsphere.infra.binder.segment.insert.keygen.GeneratedKeyContext;
import org.apache.shardingsphere.infra.binder.statement.dml.InsertStatementContext;
import org.apache.shardingsphere.infra.rewrite.sql.token.generator.aware.ParametersAware;
import org.apache.shardingsphere.sharding.rewrite.token.generator.impl.keygen.BaseGeneratedKeyTokenGenerator;
import org.apache.shardingsphere.sharding.rewrite.token.pojo.GeneratedKeyAssignmentToken;
import org.apache.shardingsphere.sharding.rewrite.token.pojo.LiteralGeneratedKeyAssignmentToken;
import org.apache.shardingsphere.sharding.rewrite.token.pojo.ParameterMarkerGeneratedKeyAssignmentToken;
import org.apache.shardingsphere.sql.parser.sql.common.statement.dml.InsertStatement;
import org.apache.shardingsphere.sql.parser.sql.dialect.handler.dml.InsertStatementHandler;

import java.util.List;
import java.util.Optional;

@Setter
public final class GeneratedKeyAssignmentTokenGenerator extends BaseGeneratedKeyTokenGenerator implements ParametersAware {

    private List<Object> parameters;

    @Override
    protected boolean isGenerateSQLToken(final InsertStatementContext insertStatementContext) {
//        return true;
        return InsertStatementHandler.getSetAssignmentSegment(insertStatementContext.getSqlStatement()).isPresent();
    }

    @Override
    public GeneratedKeyAssignmentToken generateSQLToken(final InsertStatementContext insertStatementContext) {
        Optional<GeneratedKeyContext> generatedKey = insertStatementContext.getGeneratedKeyContext();
//        Preconditions.checkState(generatedKey.isPresent());
        InsertStatement insertStatement = insertStatementContext.getSqlStatement();
//        Preconditions.checkState(InsertStatementHandler.getSetAssignmentSegment(insertStatement).isPresent());
        int startIndex = InsertStatementHandler.getSetAssignmentSegment(insertStatement).get().getStopIndex() + 1;
        return parameters.isEmpty() ? new LiteralGeneratedKeyAssignmentToken(startIndex, generatedKey.get().getColumnName(), generatedKey.get().getGeneratedValues().iterator().next())
                : new ParameterMarkerGeneratedKeyAssignmentToken(startIndex, generatedKey.get().getColumnName());
    }
}
