package com.rlynic.sharding.spi;

import com.google.common.base.Preconditions;
import com.rlynic.sharding.slot.database.ShardingEnvironmentPostProcessor;
import com.rlynic.sharding.slot.database.util.SpringBeanUtil;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.apache.shardingsphere.driver.jdbc.core.driver.ShardingSphereDriverURLProvider;
import org.springframework.context.ApplicationContext;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class SpringClasspathDriverURLProvider implements ShardingSphereDriverURLProvider {

    private static final String CLASSPATH_TYPE = "spring:";

    @Override
    public boolean accept(final String url) {
        return StringUtils.isNotBlank(url) && url.contains(CLASSPATH_TYPE);
    }

    @Override
    @SneakyThrows(IOException.class)
    public byte[] getContent(final String url) {
        String configuredFile = url.substring("jdbc:shardingsphere:".length(), url.contains("?") ? url.indexOf("?") : url.length());
        String file = configuredFile.substring(CLASSPATH_TYPE.length());
        Preconditions.checkArgument(!file.isEmpty(), "Configuration file is required in ShardingSphere driver URL.");
        try (InputStream stream = getResourceAsStream(file)) {
            Objects.requireNonNull(stream, String.format("Can not find configuration file `%s`.", file));
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            String line;
            while (null != (line = reader.readLine())) {
                if (!line.startsWith("#")) {
                    builder.append(line).append('\n');
                }
            }
            String resolveContent = ShardingEnvironmentPostProcessor.resoveConfig(builder.toString());
            System.out.println(resolveContent);
            return resolveContent.getBytes(StandardCharsets.UTF_8);
        }
    }

    private InputStream getResourceAsStream(final String resource) {
        ClassLoader[] classLoaders = new ClassLoader[]{
                Thread.currentThread().getContextClassLoader(), getClass().getClassLoader(), ClassLoader.getSystemClassLoader(),
        };
        for (ClassLoader each : classLoaders) {
            if (null != each) {
                InputStream result = each.getResourceAsStream(resource);
                if (null == result) {
                    result = each.getResourceAsStream("/" + resource);
                }
                if (null != result) {
                    return result;
                }
            }
        }
        return null;
    }
}
