package com.rlynic.sharding.slot.database;

import org.apache.shardingsphere.sql.parser.sql.common.segment.dml.expr.ExpressionSegment;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RemoveParameterMarkerHolder {

    private static ThreadLocal<Map<String, List<ExpressionSegment>>> expressionSegmentMap = new ThreadLocal<>();

    public static void add(String db, List<ExpressionSegment> expressionSegment){
        Map<String, List<ExpressionSegment>> removeExpressionSegment = expressionSegmentMap.get();
        if(null == removeExpressionSegment){
            removeExpressionSegment = new HashMap<>();
            expressionSegmentMap.set(removeExpressionSegment);
        }
        List<ExpressionSegment> expressionSegmentList = removeExpressionSegment.get(db);
        if(expressionSegmentList != null){
            expressionSegment.addAll(expressionSegmentList);
        }
        removeExpressionSegment.put(db, expressionSegment);
    }

    public static Map<String, List<ExpressionSegment>> get(){
        return expressionSegmentMap.get();
    }

    public static void clear(){
        expressionSegmentMap.remove();
    }

}