package com.rlynic.sharding.slot.database.sql.token;

import org.apache.shardingsphere.infra.rewrite.sql.token.pojo.Attachable;
import org.apache.shardingsphere.infra.rewrite.sql.token.pojo.SQLToken;

public final class SlotInsertColumnToken extends SQLToken implements Attachable {

    private final String column;

    public SlotInsertColumnToken(final int startIndex, final String column) {
        super(startIndex);
        this.column = column;
    }

    @Override
    public String toString() {
        return ", " + column;
    }
}