package com.rlynic.sharding.slot.database.strategy;

import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.utility.JavaModule;

public class RouteSQLRewriteEngineAgent {
    private static RouteSQLRewriteEngineAgent instance = new RouteSQLRewriteEngineAgent();
    private RouteSQLRewriteEngineAgent(){};
    public static RouteSQLRewriteEngineAgent getInstance(){
        return instance;
    }

    public void init(){
        synchronized (RouteSQLRewriteEngineAgent.class){
            ByteBuddyAgent.install();
            new AgentBuilder.Default()
                                               //org.apache.shardingsphere.infra.rewrite.engine
                    .type(ElementMatchers.named("org.apache.shardingsphere.infra.rewrite.engine.RouteSQLRewriteEngine"))
//                    .transform((builder, type, classLoader, module) ->
//                            builder.method(ElementMatchers.named("addSQLRewriteUnits")).intercept(MethodDelegation.to(RewriteEngineInterceptor.class)))
                    .transform((builder, type, classLoader, module) ->
                            builder.method(ElementMatchers.named("translate")).intercept(MethodDelegation.to(RewriteMethodInterceptor.class)))
                    .with(new AgentBuilder.Listener(){
                        @Override
                        public void onDiscovery(String s, ClassLoader classLoader, JavaModule javaModule, boolean b) {
                        }

                        @Override
                        public void onTransformation(TypeDescription typeDescription, ClassLoader classLoader, JavaModule module, boolean loaded, DynamicType dynamicType) {
                        }

                        @Override
                        public void onIgnored(TypeDescription typeDescription, ClassLoader classLoader, JavaModule module, boolean loaded) {
                        }

                        @Override
                        public void onError(String typeName, ClassLoader classLoader, JavaModule module, boolean loaded, Throwable throwable) {

                        }

                        @Override
                        public void onComplete(String typeName, ClassLoader classLoader, JavaModule module, boolean loaded) {
                        }
                    })
                    .installOnByteBuddyAgent();
        }
    }
}
