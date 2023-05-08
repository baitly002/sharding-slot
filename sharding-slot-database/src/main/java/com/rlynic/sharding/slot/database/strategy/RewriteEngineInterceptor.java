package com.rlynic.sharding.slot.database.strategy;


import com.rlynic.sharding.slot.database.RemoveParameterMarkerHolder;
import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.implementation.bind.annotation.*;
import org.apache.shardingsphere.infra.rewrite.context.SQLRewriteContext;
import org.apache.shardingsphere.infra.rewrite.engine.result.SQLRewriteUnit;
import org.apache.shardingsphere.infra.rewrite.sql.impl.RouteSQLBuilder;
import org.apache.shardingsphere.infra.route.context.RouteContext;
import org.apache.shardingsphere.infra.route.context.RouteUnit;
import org.apache.shardingsphere.sql.parser.sql.common.segment.dml.expr.ExpressionSegment;
import org.apache.shardingsphere.sql.parser.sql.common.segment.dml.expr.simple.LiteralExpressionSegment;
import org.apache.shardingsphere.sql.parser.sql.common.segment.dml.expr.simple.ParameterMarkerExpressionSegment;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.Callable;

@Slf4j
public class RewriteEngineInterceptor {

    /**
     * 进行方法拦截, 注意这里可以对所有修饰符的修饰的方法（包含private的方法）进行拦截
     *
     * @param method   待处理方法
     * @param callable 原方法执行
     * @return 执行结果
     */
    @RuntimeType
    public static void intercept(
                                    // 被拦截的目标对象 （动态生成的目标对象）
                                   @This Object target,
                                   // 正在执行的方法Method 对象（目标对象父类的Method）
                                   @Origin Method method,
                                   // 正在执行的方法的全部参数
                                   @AllArguments Object[] argumengts,
                                   // 目标对象的一个代理
                                   @Super Object delegate,
                                   // 方法的调用者对象 对原始方法的调用依靠它
                                   @SuperCall Callable<?> callable) throws Exception {
        Map<String, List<ExpressionSegment>> removeParameterMarkerMap = RemoveParameterMarkerHolder.get();
        if(removeParameterMarkerMap == null){
            callable.call();
        }else {
            final Map<RouteUnit, SQLRewriteUnit> sqlRewriteUnits = (Map) argumengts[0];
            final SQLRewriteContext sqlRewriteContext = (SQLRewriteContext) argumengts[1];
            final RouteContext routeContext = (RouteContext) argumengts[2];
            final Collection<RouteUnit> routeUnits = (Collection<RouteUnit>) argumengts[3];

            for (RouteUnit each : routeUnits) {
                String sql = new RouteSQLBuilder(sqlRewriteContext, each).toSQL();
                StringBuilder result = new StringBuilder(sql);
                List<ExpressionSegment> pms = removeParameterMarkerMap.get(each.getDataSourceMapper().getLogicName());

                int removeIndex = 0;
                ArrayList<Object> parameters;
                ArrayList<Object> sqlParameters = (ArrayList<Object>) sqlRewriteContext.getParameters();
                parameters = (ArrayList) sqlParameters.clone();
                Collections.reverse(pms);
                for (ExpressionSegment pm : pms) {
                    if(pm instanceof ParameterMarkerExpressionSegment) {
                        result.replace(pm.getStartIndex(), pm.getStopIndex() + 1, createSpaceString(pm.getStopIndex() - pm.getStartIndex() + 1));
                        processQuotation(result, pm.getStopIndex());
                        parameters.remove(((ParameterMarkerExpressionSegment) pm).getParameterMarkerIndex() - removeIndex);
                        removeIndex++;
                    }
                    if(pm instanceof LiteralExpressionSegment){
                        result.replace(pm.getStartIndex(), pm.getStopIndex() + 1, createSpaceString(pm.getStopIndex() - pm.getStartIndex() + 1));
                        processQuotation(result, pm.getStopIndex());
                    }
                }
                sqlRewriteUnits.put(each, new SQLRewriteUnit(formatString(result.toString()), parameters));
            }
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
