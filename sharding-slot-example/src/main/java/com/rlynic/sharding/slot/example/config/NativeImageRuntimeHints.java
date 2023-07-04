package com.rlynic.sharding.slot.example.config;

import com.rlynic.sharding.slot.example.entities.Order;
import com.rlynic.sharding.slot.example.entities.OrderItem;
import com.rlynic.sharding.slot.example.repositories.master.MasterOrderItemRepository;
import com.rlynic.sharding.slot.example.repositories.master.MasterOrderRepository;
import org.apache.ibatis.javassist.util.proxy.ProxyFactory;
import org.apache.ibatis.logging.slf4j.Slf4jImpl;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.scripting.defaults.RawLanguageDriver;
import org.apache.ibatis.scripting.xmltags.XMLLanguageDriver;
import org.apache.shardingsphere.driver.ShardingSphereDriver;
import org.springframework.aot.hint.ExecutableMode;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportRuntimeHints;

@Configuration
@ImportRuntimeHints(NativeImageRuntimeHints.HibernateRegistrar.class)
public class NativeImageRuntimeHints {
    static class HibernateRegistrar implements RuntimeHintsRegistrar {
        @Override
        public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
            try {
//                hints.reflection()
//                        .registerType(ShardingSphereDriver.class,
//                                hint -> hint.withMembers(MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS, MemberCategory.INTROSPECT_PUBLIC_METHODS)
//                                        .onReachableType(PostgreSQLPGObjectJdbcType.class));
                hints.reflection().registerConstructor(ShardingSphereDriver.class.getConstructor(), ExecutableMode.INVOKE);
                hints.reflection().registerConstructor(Slf4jImpl.class.getConstructor(String.class), ExecutableMode.INVOKE);
                hints.reflection().registerConstructor(org.apache.ibatis.session.Configuration.class.getConstructor(), ExecutableMode.INVOKE);
                hints.reflection().registerConstructor(ProxyFactory.class.getConstructor(), ExecutableMode.INVOKE);
                hints.reflection().registerConstructor(XMLLanguageDriver.class.getConstructor(), ExecutableMode.INVOKE);
                hints.reflection().registerConstructor(RawLanguageDriver.class.getConstructor(), ExecutableMode.INVOKE);
//                hints.reflection().registerConstructor(Interceptor.class.getConstructor(), ExecutableMode.INVOKE);


////                hints.reflection().registerConstructor(org.apache.ibatis.session.Configuration.class.getConstructor(Environment.class), ExecutableMode.INVOKE);
//                hints.resources().registerPattern("META-INF/mybatis-config.xml");
//                hints.resources().registerPattern("META-INF/mybatis-master-config.xml");
//                hints.resources().registerPattern("META-INF/mappers/OrderMapper.xml");
//                hints.resources().registerPattern("META-INF/mappers/OrderItemMapper.xml");
                hints.resources().registerPattern("META-INF/mappers/MasterOrderMapper.xml");
                hints.resources().registerPattern("META-INF/mappers/MasterOrderItemMapper.xml");
//
                hints.proxies().registerJdkProxy(MasterOrderRepository.class);
                hints.proxies().registerJdkProxy(MasterOrderItemRepository.class);

                hints.serialization().registerType(Order.class);
                hints.serialization().registerType(OrderItem.class);
//                hints.proxies().registerJdkProxy(OrderRepository.class);
//                hints.proxies().registerJdkProxy(OrderItemRepository.class);

//                hints.reflection().registerConstructor(SelectorProvider.class.getConstructor(), ExecutableMode.INVOKE);
//
//                Method method = ReflectionUtils.findMethod(SelectorProvider.class, "openDatagramChannel", ProtocolFamily.class);
//                if(method!=null) {
//                    hints.reflection().registerMethod(method, ExecutableMode.INVOKE);
//                }else{
//                    System.out.println("openDatagramChannel = null");
//                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
