package com.rlynic.sharding.slot.database.sql.token;

import org.apache.shardingsphere.infra.datanode.DataNode;
import org.apache.shardingsphere.infra.rewrite.sql.token.pojo.RouteUnitAware;
import org.apache.shardingsphere.infra.rewrite.sql.token.pojo.generic.InsertValue;
import org.apache.shardingsphere.infra.rewrite.sql.token.pojo.generic.InsertValuesToken;
import org.apache.shardingsphere.infra.route.context.RouteUnit;

/**
 * @ShardingInsertValuesToken
 */
public final class SlotInsertValuesToken extends InsertValuesToken implements RouteUnitAware {

    public SlotInsertValuesToken(final int startIndex, final int stopIndex) {
        super(startIndex, stopIndex);
    }

    @Override
    public String toString(final RouteUnit routeUnit) {
        StringBuilder result = new StringBuilder();
        appendInsertValue(routeUnit, result);
        result.delete(result.length() - 2, result.length());
        return result.toString();
    }

    private void appendInsertValue(final RouteUnit routeUnit, final StringBuilder stringBuilder) {
        for (InsertValue each : getInsertValues()) {
            if (isAppend(routeUnit, (SlotInsertValue) each)) {
                stringBuilder.append(each).append(", ");
            }
        }
    }

    private boolean isAppend(final RouteUnit routeUnit, final SlotInsertValue insertValueToken) {
        if (insertValueToken.getDataNodes().isEmpty() || null == routeUnit) {
            return true;
        }
        for (DataNode each : insertValueToken.getDataNodes()) {
            if (routeUnit.findTableMapper(each.getDataSourceName(), each.getTableName()).isPresent()) {
                return true;
            }
        }
        return false;
    }
}