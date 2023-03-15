package com.rlynic.sharding.slot.database.util;

import com.rlynic.sharding.slot.database.segment.ParameterMarkerExpressionSegmentExt;
import org.apache.shardingsphere.sql.parser.sql.common.constant.LogicalOperator;
import org.apache.shardingsphere.sql.parser.sql.common.segment.dml.expr.BinaryOperationExpression;
import org.apache.shardingsphere.sql.parser.sql.common.segment.dml.expr.ExpressionSegment;
import org.apache.shardingsphere.sql.parser.sql.common.segment.dml.expr.FunctionSegment;
import org.apache.shardingsphere.sql.parser.sql.common.segment.dml.expr.InExpression;
import org.apache.shardingsphere.sql.parser.sql.common.segment.dml.expr.simple.ParameterMarkerExpressionSegment;
import org.apache.shardingsphere.sql.parser.sql.common.segment.dml.predicate.AndPredicate;

import java.util.*;

public class SqlExpressionExtractUtil {

    /**
     * Get and predicate collection.
     *
     * @param expression expression segment
     * @return and predicate collection
     */
    public static Collection<AndPredicate> getAndPredicates(final ExpressionSegment expression) {
        Collection<AndPredicate> result = new LinkedList<>();
        extractAndPredicates(result, expression);
        return result;
    }

    private static void extractAndPredicates(final Collection<AndPredicate> result, final ExpressionSegment expression) {
        if (!(expression instanceof BinaryOperationExpression)) {
            result.add(createAndPredicate(expression));
            return;
        }
        BinaryOperationExpression binaryExpression = (BinaryOperationExpression) expression;
        Optional<LogicalOperator> logicalOperator = LogicalOperator.valueFrom(binaryExpression.getOperator());
        if (logicalOperator.isPresent() && LogicalOperator.OR == logicalOperator.get()) {
            extractAndPredicates(result, binaryExpression.getLeft());
            extractAndPredicates(result, binaryExpression.getRight());
        } else if (logicalOperator.isPresent() && LogicalOperator.AND == logicalOperator.get()) {
            Collection<AndPredicate> predicates = getAndPredicates(binaryExpression.getRight());
            for (AndPredicate each : getAndPredicates(binaryExpression.getLeft())) {
                extractCombinedAndPredicates(result, each, predicates);
            }
        } else {
            result.add(createAndPredicate(expression));
        }
    }

    private static void extractCombinedAndPredicates(final Collection<AndPredicate> result, final AndPredicate current, final Collection<AndPredicate> predicates) {
        for (AndPredicate each : predicates) {
            AndPredicate predicate = new AndPredicate();
            predicate.getPredicates().addAll(current.getPredicates());
            predicate.getPredicates().addAll(each.getPredicates());
            result.add(predicate);
        }
    }

    private static AndPredicate createAndPredicate(final ExpressionSegment expression) {
        AndPredicate result = new AndPredicate();
        result.getPredicates().add(expression);
        return result;
    }

    /**
     * Get parameter marker expression collection.
     *
     * @param expressions expression collection
     * @return parameter marker expression collection
     */
    public static List<ParameterMarkerExpressionSegmentExt> getParameterMarkerExpressions(final Collection<ExpressionSegment> expressions) {
        List<ParameterMarkerExpressionSegmentExt> result = new ArrayList<>();
        extractParameterMarkerExpressions(result, expressions, null);
        return result;
    }

    private static void extractParameterMarkerExpressions(final List<ParameterMarkerExpressionSegmentExt> result, final Collection<ExpressionSegment> expressions, ExpressionSegment left) {
        for (ExpressionSegment each : expressions) {
            if (each instanceof ParameterMarkerExpressionSegment) {
                result.add(new ParameterMarkerExpressionSegmentExt(left, (ParameterMarkerExpressionSegment)each));
            }
            // TODO support more expression type if necessary
            if (each instanceof InExpression){
                extractParameterMarkerExpressions(result, Collections.singletonList(((InExpression) each).getLeft()), null);
                extractParameterMarkerExpressions(result, Collections.singletonList(((InExpression) each).getRight()), ((InExpression) each).getLeft());
            }
            if (each instanceof BinaryOperationExpression) {
                extractParameterMarkerExpressions(result, Collections.singletonList(((BinaryOperationExpression) each).getLeft()), null);
                extractParameterMarkerExpressions(result, Collections.singletonList(((BinaryOperationExpression) each).getRight()), ((InExpression) each).getLeft());
            }
            if (each instanceof FunctionSegment) {
                extractParameterMarkerExpressions(result, ((FunctionSegment) each).getParameters(), null);
            }
        }
    }

    public static List<InExpression> getInExpressions(final Collection<ExpressionSegment> expressions) {
        List<InExpression> result = new ArrayList<>();
        extractInExpressions(result, expressions);
        return result;
    }

    private static void extractInExpressions(final List<InExpression> result, final Collection<ExpressionSegment> expressions) {
        for (ExpressionSegment each : expressions) {
            // TODO support more expression type if necessary
            if (each instanceof InExpression){
                result.add((InExpression) each);
            }
            if (each instanceof BinaryOperationExpression) {
                extractInExpressions(result, Collections.singletonList(((BinaryOperationExpression) each).getLeft()));
                extractInExpressions(result, Collections.singletonList(((BinaryOperationExpression) each).getRight()));
            }
            if (each instanceof FunctionSegment) {
                extractInExpressions(result, ((FunctionSegment) each).getParameters());
            }
        }
    }
}