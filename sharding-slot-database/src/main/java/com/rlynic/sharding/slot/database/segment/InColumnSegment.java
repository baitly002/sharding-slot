package com.rlynic.sharding.slot.database.segment;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@ToString
@EqualsAndHashCode
public class InColumnSegment {
    private String columnName;
    private String aliasName;
    private Integer index;
    private Boolean shardingKey;
    public InColumnSegment(){}
    public InColumnSegment(String columnName, String aliasName, Integer index, Boolean shardingKey){
        this.columnName = columnName;
        this.aliasName = aliasName;
        this.index = index;
        this.shardingKey = shardingKey;
    }
}
