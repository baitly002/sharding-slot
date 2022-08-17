package io.seata.spring.annotation.datasource;

import io.seata.rm.datasource.SeataDataSourceProxy;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

public class DataSourceProxyHolder {

    private static final Map<DataSource, SeataDataSourceProxy> PROXY_MAP = new HashMap<>(4);

    public static SeataDataSourceProxy put(DataSource origin, SeataDataSourceProxy proxy) {
        return PROXY_MAP.put(origin, proxy);
    }

    public static SeataDataSourceProxy get(DataSource origin) {
        return PROXY_MAP.get(origin);
    }
}