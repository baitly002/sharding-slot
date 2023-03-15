package com.rlynic.sharding.slot.database;

import com.google.common.collect.Lists;
import org.apache.shardingsphere.sql.parser.sql.common.segment.dml.expr.simple.ParameterMarkerExpressionSegment;

import java.util.*;

public class RemoveParameterMarkerHolder {

    private static ThreadLocal<Map<String, List<ParameterMarkerExpressionSegment>>> parameterMarkerMap = new ThreadLocal<>();

    public static void add(String db, List<ParameterMarkerExpressionSegment> parameterMarker){
        Map<String, List<ParameterMarkerExpressionSegment>> removeParameterMarker = parameterMarkerMap.get();
        if(null == removeParameterMarker){
            removeParameterMarker = new HashMap<>();
            parameterMarkerMap.set(removeParameterMarker);
        }
        List<ParameterMarkerExpressionSegment> parameterMarkerList = removeParameterMarker.get(db);
        if(parameterMarkerList != null){
//            parameterMarkerList.addAll(parameterMarker);
            parameterMarker.addAll(parameterMarkerList);
        }
        removeParameterMarker.put(db, parameterMarker);
    }

    public static Map<String, List<ParameterMarkerExpressionSegment>> get(){
        return parameterMarkerMap.get();
    }

    public static void clear(){
        parameterMarkerMap.remove();
    }

}