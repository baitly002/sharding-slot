package com.rlynic.sharding.plugin;

import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.utility.JavaModule;
import org.apache.shardingsphere.driver.jdbc.core.connection.ShardingSphereConnection;

import static net.bytebuddy.agent.builder.AgentBuilder.RedefinitionStrategy.RETRANSFORMATION;

@Slf4j
public class ShardingJdbcCoreAgent {
    private static ShardingJdbcCoreAgent instance = new ShardingJdbcCoreAgent();
    private ShardingJdbcCoreAgent(){};
    public static ShardingJdbcCoreAgent getInstance(){
        return instance;
    }

    public void init(){
        synchronized (ShardingJdbcCoreAgent.class){
            ByteBuddyAgent.install();
            new AgentBuilder.Default()
//                    .type(ElementMatchers.is(ShardingSphereDatabaseMetaData.class))
//                    .transform((builder, type, classLoader, module) ->
//                            builder.method(ElementMatchers.named("getConnection")).intercept(MethodDelegation.to(ConnectionMethodInterceptor.class)))
//                    .transform((builder, type, classLoader, module) ->
//                            builder.method(ElementMatchers.named("getColumns")).intercept(MethodDelegation.to(ColumnsMethodInterceptor.class)))
                    .type(ElementMatchers.is(ShardingSphereConnection.class))
                    .transform((builder, type, classLoader, module) ->
                            builder.method(ElementMatchers.named("getMetaData")).intercept(MethodDelegation.to(MetaDataMethodInterceptor.class)))
//                    .type(ElementMatchers.is(GeneratedKeysResultSet.class))
//                    .transform((builder, type, classLoader, module) ->
//                            builder.defineMethod("isAfterLast", boolean.class, Modifier.PUBLIC).intercept(FixedValue.value(true)))
                    .disableClassFormatChanges() //bytebuddy详细日志
                    .with(AgentBuilder.Listener.StreamWriting.toSystemOut()) //bytebuddy详细日志
                    .with(RETRANSFORMATION) //bytebuddy详细日志
                    .with(new AgentBuilder.Listener(){
                        @Override
                        public void onDiscovery(String s, ClassLoader classLoader, JavaModule javaModule, boolean b) {
//                            log.info("onDiscovery");
                        }

                        @Override
                        public void onTransformation(TypeDescription typeDescription, ClassLoader classLoader, JavaModule module, boolean loaded, DynamicType dynamicType) {
                            log.info("onTransformation");
                        }

                        @Override
                        public void onIgnored(TypeDescription typeDescription, ClassLoader classLoader, JavaModule module, boolean loaded) {
//                            log.info("onIgnored");
                        }

                        @Override
                        public void onError(String typeName, ClassLoader classLoader, JavaModule module, boolean loaded, Throwable throwable) {
                            log.info("onError", throwable);
                        }

                        @Override
                        public void onComplete(String typeName, ClassLoader classLoader, JavaModule module, boolean loaded) {
//                            log.info("onComplete");
                        }
                    })
                    .installOnByteBuddyAgent();
        }
    }
}
