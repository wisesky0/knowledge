package com.example.tobe;

import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import javax.sql.DataSource;

@TestConfiguration
public class TestDataSourceConfig {

    public static final BatchCountListener BATCH_LISTENER = new BatchCountListener();

    @Bean
    public static BeanPostProcessor dataSourceProxyBeanPostProcessor() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
                if (bean instanceof DataSource && !(bean instanceof net.ttddyy.dsproxy.support.ProxyDataSource)) {
                    return ProxyDataSourceBuilder
                            .create((DataSource) bean)
                            .listener(BATCH_LISTENER)
                            .build();
                }
                return bean;
            }
        };
    }
}
