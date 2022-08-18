//package com.rlynic.sharding.slot.example.config.sharding;
//
//import org.apache.shardingsphere.driver.jdbc.adapter.AbstractDataSourceAdapter;
//import org.apache.shardingsphere.driver.jdbc.context.CachedDatabaseMetaData;
//import org.apache.shardingsphere.driver.jdbc.context.JDBCContext;
//import org.apache.shardingsphere.driver.state.DriverStateContext;
//import org.apache.shardingsphere.infra.config.RuleConfiguration;
//import org.apache.shardingsphere.infra.config.checker.RuleConfigurationCheckerFactory;
//import org.apache.shardingsphere.infra.config.database.impl.DataSourceProvidedDatabaseConfiguration;
//import org.apache.shardingsphere.infra.config.mode.ModeConfiguration;
//import org.apache.shardingsphere.infra.config.scope.GlobalRuleConfiguration;
//import org.apache.shardingsphere.infra.instance.definition.InstanceDefinition;
//import org.apache.shardingsphere.infra.instance.definition.InstanceType;
//import org.apache.shardingsphere.mode.manager.ContextManager;
//import org.apache.shardingsphere.mode.manager.ContextManagerBuilderFactory;
//import org.apache.shardingsphere.mode.manager.ContextManagerBuilderParameter;
//
//import javax.sql.DataSource;
//import java.sql.Connection;
//import java.sql.SQLException;
//import java.util.*;
//import java.util.stream.Collectors;
//
//public class MyShardingSphereDataSource extends AbstractDataSourceAdapter implements AutoCloseable {
//
//    private final String databaseName;
//
//    private final ContextManager contextManager;
//
//    private final JDBCContext jdbcContext;
//
//    public MyShardingSphereDataSource(final String databaseName, final ModeConfiguration modeConfig) throws SQLException {
//        this.databaseName = databaseName;
//        contextManager = createContextManager(databaseName, modeConfig, new HashMap<>(), new LinkedList<>(), new Properties());
//        jdbcContext = new JDBCContext(contextManager.getDataSourceMap(databaseName));
//    }
//
//    public MyShardingSphereDataSource(final String databaseName, final ModeConfiguration modeConfig, final Map<String, DataSource> dataSourceMap,
//                                    final Collection<RuleConfiguration> ruleConfigs, final Properties props) throws SQLException {
//        checkRuleConfiguration(databaseName, ruleConfigs);
//        this.databaseName = databaseName;
//        contextManager = createContextManager(databaseName, modeConfig, dataSourceMap, ruleConfigs, null == props ? new Properties() : props);
//        jdbcContext = new JDBCContext(contextManager.getDataSourceMap(databaseName));
//    }
//
//    @SuppressWarnings("unchecked")
//    private void checkRuleConfiguration(final String databaseName, final Collection<RuleConfiguration> ruleConfigs) {
//        ruleConfigs.forEach(each -> RuleConfigurationCheckerFactory.findInstance(each).ifPresent(optional -> optional.check(databaseName, each)));
//    }
//
//    private ContextManager createContextManager(final String databaseName, final ModeConfiguration modeConfig, final Map<String, DataSource> dataSourceMap,
//                                                final Collection<RuleConfiguration> ruleConfigs, final Properties props) throws SQLException {
//        ContextManagerBuilderParameter parameter = ContextManagerBuilderParameter.builder()
//                .modeConfig(modeConfig)
//                .databaseConfigs(Collections.singletonMap(databaseName, new DataSourceProvidedDatabaseConfiguration(dataSourceMap, ruleConfigs)))
//                .globalRuleConfigs(ruleConfigs.stream().filter(each -> each instanceof GlobalRuleConfiguration).collect(Collectors.toList()))
//                .props(props)
//                .instanceDefinition(new InstanceDefinition(InstanceType.JDBC)).build();
//        return ContextManagerBuilderFactory.getInstance(modeConfig).build(parameter);
//    }
//
//    private Optional<CachedDatabaseMetaData> createCachedDatabaseMetaData(final Map<String, DataSource> dataSources) throws SQLException {
//        if (dataSources.isEmpty()) {
//            return Optional.empty();
//        }
//        try (Connection connection = dataSources.values().iterator().next().getConnection()) {
//            return Optional.of(new CachedDatabaseMetaData(connection.getMetaData()));
//        }
//    }
//
//    @Override
//    public Connection getConnection() throws SQLException {
//        return DriverStateContext.getConnection(databaseName, contextManager, jdbcContext);
//    }
//
//    @Override
//    public Connection getConnection(final String username, final String password) throws SQLException {
//        return getConnection();
//    }
//
//    /**
//     * Close data sources.
//     *
//     * @param dataSourceNames data source names to be closed
//     * @throws Exception exception
//     */
//    public void close(final Collection<String> dataSourceNames) throws Exception {
//        Map<String, DataSource> dataSourceMap = contextManager.getDataSourceMap(databaseName);
//        for (String each : dataSourceNames) {
//            close(dataSourceMap.get(each));
//        }
//        contextManager.close();
//    }
//
//    private void close(final DataSource dataSource) throws Exception {
//        if (dataSource instanceof AutoCloseable) {
//            ((AutoCloseable) dataSource).close();
//        }
//    }
//
//    @Override
//    public void close() throws Exception {
//        close(contextManager.getDataSourceMap(databaseName).keySet());
//    }
//
//    @Override
//    public int getLoginTimeout() throws SQLException {
//        Map<String, DataSource> dataSourceMap = contextManager.getDataSourceMap(databaseName);
//        return dataSourceMap.isEmpty() ? 0 : dataSourceMap.values().iterator().next().getLoginTimeout();
//    }
//
//    @Override
//    public void setLoginTimeout(final int seconds) throws SQLException {
//        for (DataSource each : contextManager.getDataSourceMap(databaseName).values()) {
//            each.setLoginTimeout(seconds);
//        }
//    }
//}