package com.rlynic.sharding.slot.database.segment;

import org.apache.shardingsphere.sql.parser.sql.common.segment.dml.column.ColumnSegment;
import org.apache.shardingsphere.sql.parser.sql.common.segment.dml.expr.ExpressionSegment;
import org.apache.shardingsphere.sql.parser.sql.common.segment.dml.expr.InExpression;
import org.apache.shardingsphere.sql.parser.sql.common.segment.dml.expr.ListExpression;
import org.apache.shardingsphere.sql.parser.sql.common.segment.dml.expr.complex.CommonExpressionSegment;
import org.apache.shardingsphere.sql.parser.sql.common.segment.generic.OwnerSegment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class SegmentUtil {

    public static List<InColumnSegment> parserInColumn(InExpression inExpression){
        ExpressionSegment left = inExpression.getLeft();
        List<InColumnSegment> inColumnSegmentList = new ArrayList<>();
        if(left instanceof ColumnSegment) {
            InColumnSegment columnSegment = new InColumnSegment();
            OwnerSegment ownerSegment = ((ColumnSegment) left).getOwner().orElse(null);
            if(ownerSegment != null) {
                //有别名
                columnSegment.setAliasName(ownerSegment.getIdentifier().getValue());
            }
            columnSegment.setColumnName(((ColumnSegment) left).getQualifiedName());
            columnSegment.setIndex(0);
            columnSegment.setShardingKey(false);
            inColumnSegmentList.add(columnSegment);
        }
        if(left instanceof CommonExpressionSegment) {
            //多字段的in操作
            String leftText = ((CommonExpressionSegment) left).getText();
            String leftColumns = leftText.substring(1, leftText.length() - 1);
            int index = 0;
            for (String column : leftColumns.split(",")) {
                InColumnSegment columnSegment = new InColumnSegment();
                column = column.trim();
                if (column.indexOf(".") > 0) {
                    //有别名
                    String[] aliasColumn = column.split("\\.");
                    if (aliasColumn.length > 1) {
                        columnSegment.setColumnName(aliasColumn[1]);
                        columnSegment.setAliasName(aliasColumn[0]);
                    }
                }else{
                    columnSegment.setColumnName(column);
                }
                columnSegment.setIndex(index);
                columnSegment.setShardingKey(false);
                inColumnSegmentList.add(columnSegment);
                index ++;
            }
        }
//        Collection<ExpressionSegment> listExpression = inExpression.getExpressionList();
        return inColumnSegmentList;
    }


}
