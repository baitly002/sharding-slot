package com.rlynic.sharding.slot.database.sql.token;

import lombok.Getter;
import org.apache.shardingsphere.infra.rewrite.sql.token.pojo.SQLToken;
import org.apache.shardingsphere.infra.rewrite.sql.token.pojo.Substitutable;

@Getter
public final class RemoveInToken extends SQLToken implements Substitutable {

    private final int stopIndex;
    private final int sort;

    public RemoveInToken(final int startIndex, final int stopIndex, final int sort) {
        super(startIndex);
        this.stopIndex = stopIndex;
        this.sort = sort;
    }

    @Override
    public String toString() {
        return "";
    }
}