package com.rlynic.sharding.slot.database.segment;

import lombok.Data;
import org.apache.shardingsphere.sql.parser.sql.common.segment.generic.table.SimpleTableSegment;

import java.util.Collection;
import java.util.Map;

@Data
public class TableMapping {
    private Map<String, String> mapping;
    private Collection<SimpleTableSegment> tableSegments;
}
