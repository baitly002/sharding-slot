package com.rlynic.sharding.plugin;

import com.rlynic.sharding.slot.database.strategy.RewriteMethodInterceptor;
import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.FixedValue;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.pool.TypePool;
import net.bytebuddy.utility.JavaModule;
import org.apache.shardingsphere.driver.jdbc.core.connection.ShardingSphereConnection;
import org.apache.shardingsphere.infra.rewrite.engine.RouteSQLRewriteEngine;

import java.lang.reflect.Modifier;

import static net.bytebuddy.agent.builder.AgentBuilder.RedefinitionStrategy.RETRANSFORMATION;

@Slf4j
public class ShardingJdbcCoreAgent {
    private static ShardingJdbcCoreAgent instance = new ShardingJdbcCoreAgent();
    private ShardingJdbcCoreAgent(){};
    public static ShardingJdbcCoreAgent getInstance(){
        return instance;
    }

    /**
     * new ByteBuddy().subclass(Foo.class)
     * - 可以用来扩展父类或接口的方法（可以理解成子类继承父类，或者接口实现类） 不能继承基本类型、数组、final类型的类
     *
     * new ByteBuddy().redefine(Foo.class)
     * -  当重定义一个类时，Bytebuddy可以对一个已有的类添加属性和方法，或者删除已经存在的方法实现。如果使用其他的方法实现替换已经存在的方法实现，则原来存在的方法实现就会消失。
     *
     * new ByteBuddy().rebase(Foo.class)
     * - 当重定基底一个类时，Bytebuddy保存基底类所有方法的实现。当Bytebuddy 如执行类型重定义时，它将所有这些方法实现复制到具有兼容签名的重新命名的私有方法中，而不是抛弃重写的方法。重定义的方法可以继续通过它们重命名过的名称调用原来的方法。
     */
    public void init(){
//        GeneratedKeysResultSet generatedKeysResultSet = new GeneratedKeysResultSet();
        synchronized (ShardingJdbcCoreAgent.class){
            ByteBuddyAgent.install();

            //修改GeneratedKeysResultSet 增加isAfterLast的方法实现，seata需要调用
            TypePool typePool = TypePool.Default.ofSystemLoader();
            new ByteBuddy()
                    .redefine(typePool.describe("org.apache.shardingsphere.driver.jdbc.core.resultset.GeneratedKeysResultSet").resolve(), // do not use 'Bar.class'  warning: Working with unloaded classes
                            ClassFileLocator.ForClassLoader.ofSystemLoader())
                    .defineMethod("isAfterLast", boolean.class, Modifier.PUBLIC).intercept(FixedValue.value(true))
                    .make()
                    .load(ClassLoader.getSystemClassLoader(), ClassLoadingStrategy.Default.INJECTION)
                    .getLoaded();


            //subclass() 不能继承基本类型、数组、final类型的类  Cannot subclass primitive, array or final types
//            new ByteBuddy().redefine(TypePool.Default.ofSystemLoader().describe("org.apache.shardingsphere.driver.jdbc.core.resultset.GeneratedKeysResultSet").resolve(), ClassFileLocator.ForClassLoader.ofSystemLoader())
//                    .defineMethod("isAfterLast", boolean.class, Modifier.PUBLIC).intercept(FixedValue.value(true))
//                    .make()
//                    .load(ClassLoader.getSystemClassLoader(), ClassReloadingStrategy.fromInstalledAgent()).getLoaded();

            //用自定义的ShardingSphereDatabaseMetaData类替换sharding源码中的ShardingSphereDatabaseMetaData
            new AgentBuilder.Default()
//                    .type(ElementMatchers.is(ShardingSphereDatabaseMetaData.class))
//                    .transform((builder, type, classLoader, module) ->
//                            builder.method(ElementMatchers.named("getConnection")).intercept(MethodDelegation.to(ConnectionMethodInterceptor.class)))
//                    .transform((builder, type, classLoader, module) ->
//                            builder.method(ElementMatchers.named("getColumns")).intercept(MethodDelegation.to(ColumnsMethodInterceptor.class)))

                    .type(ElementMatchers.is(ShardingSphereConnection.class))
                    .transform((builder, type, classLoader, module, protectionDomain) ->

                            builder.method(ElementMatchers.named("getMetaData")).intercept(MethodDelegation.to(MetaDataMethodInterceptor.class)))
//                    .type(ElementMatchers.is(GeneratedKeysResultSet.class))
//                    .transform((builder, type, classLoader, module, protectionDomain) ->
//                            builder.defineMethod("isAfterLast", boolean.class, Modifier.PUBLIC).intercept(FixedValue.value(true)))

//                    .with(AgentBuilder.PoolStrategy.Default.EXTENDED)
//                    .with(AgentBuilder.InitializationStrategy.NoOp.INSTANCE)
//                    .type(ElementMatchers.is(ShardingSphereDatabaseMetaData.class))
//                    .transform((builder, type, classLoader, module, protectionDomain) ->
//                            builder.visit(Advice.to(AdviceGetConnection.class).on(ElementMatchers.named("getConnection"))))
                    .type(ElementMatchers.is(RouteSQLRewriteEngine.class))
                    .transform((builder, type, classLoader, module, protectionDomain) ->
                            builder.method(ElementMatchers.named("translate")).intercept(MethodDelegation.to(RewriteMethodInterceptor.class)))
                    .disableClassFormatChanges() //bytebuddy详细日志
//                    .with(AgentBuilder.Listener.StreamWriting.toSystemOut()) //bytebuddy详细日志
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
