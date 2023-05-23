package com.rlynic.sharding.slot.database.strategy;


import com.rlynic.sharding.slot.database.RemoveParameterMarkerHolder;
import net.bytebuddy.implementation.bind.annotation.*;
import org.apache.shardingsphere.infra.database.type.DatabaseType;
import org.apache.shardingsphere.infra.rewrite.engine.result.SQLRewriteUnit;
import org.apache.shardingsphere.infra.route.context.RouteUnit;
import org.apache.shardingsphere.sql.parser.sql.common.segment.dml.expr.ExpressionSegment;
import org.apache.shardingsphere.sql.parser.sql.common.segment.dml.expr.simple.LiteralExpressionSegment;
import org.apache.shardingsphere.sql.parser.sql.common.segment.dml.expr.simple.ParameterMarkerExpressionSegment;
import org.apache.shardingsphere.sql.parser.sql.common.statement.SQLStatement;
import org.apache.shardingsphere.sqltranslator.rule.SQLTranslatorRule;

import java.util.*;
import java.util.concurrent.Callable;

public class RewriteMethodInterceptor {

    /**
     * 进行方法拦截, 注意这里可以对所有修饰符的修饰的方法（包含private的方法）进行拦截
     *
     * SQLTranslatorRule translatorRule
     * DatabaseType protocolType;
     * Map<String, DatabaseType> storageTypes;
     * param callable 原方法执行
     * @return 执行结果
     */
    @RuntimeType
    public static Map<RouteUnit, SQLRewriteUnit> translate(
            // 被拦截的目标对象 （动态生成的目标对象）
//            @This Object target,
            // 只想读取一个值
            // 如果要写入值，则必须使用(并安装)@FieldAccessor .
            @FieldValue("translatorRule") SQLTranslatorRule translatorRule,
            @FieldValue("protocolType") DatabaseType protocolType,
            @FieldValue("storageTypes") Map<String, DatabaseType> storageTypes,
            // 正在执行的方法Method 对象（目标对象父类的Method）
//            @Origin Method method,
            // 正在执行的方法的全部参数
//            @AllArguments Object[] argumengts,
            // 正在执行的方法的指定参数
            @Argument(0) SQLStatement sqlStatement,
            @Argument(1) Map<RouteUnit, SQLRewriteUnit> sqlRewriteUnits,
            // 目标对象的一个代理
//            @Super Object delegate,
            // 方法的调用者对象 对原始方法的调用依靠它
            @SuperCall Callable<Map<RouteUnit, SQLRewriteUnit>> callable
    ) throws Exception {

        Map<String, List<ExpressionSegment>> removeParameterMarkerMap = RemoveParameterMarkerHolder.get();
        if (removeParameterMarkerMap == null) {
            return callable.call();
        } else {
            Map<RouteUnit, SQLRewriteUnit> result = new LinkedHashMap<>(sqlRewriteUnits.size(), 1);
            for (Map.Entry<RouteUnit, SQLRewriteUnit> entry : sqlRewriteUnits.entrySet()) {
                DatabaseType storageType = storageTypes.get(entry.getKey().getDataSourceMapper().getActualName());
                String sql = translatorRule.translate(entry.getValue().getSql(), sqlStatement, protocolType, storageType);
                List<ExpressionSegment> pms = removeParameterMarkerMap.get(entry.getKey().getDataSourceMapper().getLogicName());

                int removeIndex = 0;
                List<Object> parameters = entry.getValue().getParameters();
                Collections.reverse(pms);
                StringBuilder ret = new StringBuilder(sql);
                for (ExpressionSegment pm : pms) {
                    if(pm instanceof ParameterMarkerExpressionSegment) {
                        ret.replace(pm.getStartIndex(), pm.getStopIndex() + 1, createSpaceString(pm.getStopIndex() - pm.getStartIndex() + 1));
                        processQuotation(ret, pm.getStopIndex());
                        parameters.remove(((ParameterMarkerExpressionSegment) pm).getParameterMarkerIndex() - removeIndex);
                        removeIndex++;
                    }
                    if(pm instanceof LiteralExpressionSegment){
                        ret.replace(pm.getStartIndex(), pm.getStopIndex() + 1, createSpaceString(pm.getStopIndex() - pm.getStartIndex() + 1));
                        processQuotation(ret, pm.getStopIndex());
                    }
                }
                SQLRewriteUnit sqlRewriteUnit = new SQLRewriteUnit(formatString(ret.toString()), entry.getValue().getParameters());
                result.put(entry.getKey(), sqlRewriteUnit);
            }
            RemoveParameterMarkerHolder.clear();
            return result;
        }
    }

    //处理分隔符
    public static void processQuotation(StringBuilder builder, int fromIndex){
        int quotationIndex = builder.indexOf(",", fromIndex);//？后的第一个,
        int bracketsIndex = builder.indexOf(")", fromIndex);//？后的第一个)
        if(quotationIndex != -1 && quotationIndex < bracketsIndex){
            builder.replace(quotationIndex, quotationIndex+1, " ");
            return;
        }
        quotationIndex = builder.lastIndexOf(",", fromIndex);//？前的第一个,
        bracketsIndex = builder.lastIndexOf("(", fromIndex);//？前的第一个(
        if(quotationIndex != -1 && quotationIndex>bracketsIndex){
            builder.replace(quotationIndex, quotationIndex+1, " ");
        }
    }

    public static String createSpaceString(int length){
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(" ");
        }
        return sb.toString();
    }

    public static String formatString(String str){
//        return str.replaceAll(" {2,}", " ").replaceAll("(\n ){2,}", "\n ");
        return str.replaceAll("\n", " ").replaceAll("\\(\\s*\\)\\s*,", " ")
                .replaceAll("\\s*,\\s*\\(\\s*\\)", " ").replaceAll(" {2,}", " ").trim();
    }
}
