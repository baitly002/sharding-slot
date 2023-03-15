package com.rlynic.sharding.slot.database.sql.rewrite.parameter;

import com.rlynic.sharding.slot.database.configuration.ShardingAutoConfiguration;
import com.rlynic.sharding.slot.database.configuration.SlotShardingProperties;
import com.rlynic.sharding.slot.database.context.selectin.engine.SelectInContextEngine;
import lombok.extern.slf4j.Slf4j;
import org.apache.shardingsphere.infra.binder.statement.SQLStatementContext;
import org.apache.shardingsphere.infra.binder.statement.dml.SelectStatementContext;
import org.apache.shardingsphere.infra.rewrite.parameter.builder.ParameterBuilder;
import org.apache.shardingsphere.infra.rewrite.parameter.rewriter.ParameterRewriter;
import org.apache.shardingsphere.sql.parser.sql.common.segment.dml.expr.InExpression;
import org.apache.shardingsphere.sql.parser.sql.common.segment.dml.predicate.WhereSegment;

import java.util.Collection;
import java.util.List;

/**
 * <code>{@link ShardingInValueParameterRewriter}</code>
 * 注入slot字段值
 * @author crisis
 */
@Slf4j
public class ShardingInValueParameterRewriter implements ParameterRewriter<SelectStatementContext> {
    private SlotShardingProperties slotShardingProperties;
    private Collection<InExpression> inExpressions;

    @Override
    public boolean isNeedRewrite(final SQLStatementContext sqlStatementContext) {
        if(null == slotShardingProperties){
            slotShardingProperties = ShardingAutoConfiguration.context.getBean(SlotShardingProperties.class);
        }
        if(sqlStatementContext instanceof SelectStatementContext){
//            sqlStatementContext.getTablesContext().getTableNames().contains()
            Collection<WhereSegment> whereSegments = ((SelectStatementContext) sqlStatementContext).getWhereSegments();
            inExpressions = new SelectInContextEngine().getInExpressions(whereSegments);
        }
        return sqlStatementContext instanceof SelectStatementContext;
//                && !((InsertStatementContext)sqlStatementContext).getColumnNames().contains(slotShardingProperties.getColumn())
//                && slotShardingProperties.getTableNames().contains(((SelectStatementContext) ((SelectStatementContext) sqlStatementContext).isSameGroupByAndOrderByItems().getSqlStatement()).getTable().getTableName().getIdentifier().getValue());
    }


    /**
     * 1、in查询是常量，非占位符"?"方式  -->暂未支持该sql重写
     *    也就是select * from test where id in (1,2,3) 非 select * from test where id in (？,？,？)::[1, 2, 3]
     * 2、表有别名，但字段没有   --> 理论上支持
     *    字段名唯一（多表之间，仅有一个表有该字段），可运行
     *    字段名不唯一（多表之间，其余表也含有该字段），sql语句错误，不符合规范
     * 3、in查询解释为单路由库时，是否需要重写 --> 暂未想清楚，理论上无需重写（目前代码不进行重写）
     *    场景： field1 in (route到 db1 db2) and field2 in (route到 db2)  最终路由为db2
     * 4、多in查询时，移除参数后仅剩空括号，此时SQL语法错误 -->目前没做处理，在什么场景下会出现此情况未想清楚
     * 5、not in查询 --> 目前与in做同样过滤
     */
    @Override
    public void rewrite(ParameterBuilder parameterBuilder, SelectStatementContext selectStatementContext, List<Object> parameters) {
        try {
            //TODO 问题如方法上所描述
        }catch (Exception e){
            log.error("处理in语句时发生异常，请检查！", e);
        }
    }
}
