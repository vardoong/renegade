package com.renegade.telemetry.diagnostics;

import com.zaxxer.hikari.HikariDataSource;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;
import org.aopalliance.intercept.MethodInterceptor;
import org.jspecify.annotations.NonNull;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.NameMatchMethodPointcutAdvisor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "renegade.diagnostics.telemetry-timing", name = "enabled", havingValue = "true")
class TelemetryTimingConfig {
    @Bean
    TelemetryTimingFilter telemetryTimingFilter(
            @Value("${renegade.diagnostics.telemetry-timing.threshold-ms:30}") long thresholdMillis) {
        return new TelemetryTimingFilter(thresholdMillis);
    }

    @Bean
    static BeanPostProcessor telemetryTimingDataSourcePostProcessor() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) {
                if (!(bean instanceof HikariDataSource pool)) {
                    return bean;
                }

                DataSource timedDataSource = ProxyDataSourceBuilder.create(pool)
                        .methodListener(new TelemetryJdbcTimingListener())
                        .build();
                NameMatchMethodPointcutAdvisor advisor = getAdvisor(timedDataSource);

                // Keep the HikariDataSource type available to Boot's pool metrics.
                ProxyFactory factory = new ProxyFactory(pool);
                factory.setProxyTargetClass(true);
                factory.addAdvisor(advisor);
                return factory.getProxy();
            }

            private static @NonNull NameMatchMethodPointcutAdvisor getAdvisor(DataSource timedDataSource) {
                MethodInterceptor acquireConnection = invocation -> {
                    if (TelemetryRequestTiming.current() == null) {
                        return invocation.proceed();
                    }
                    if (invocation.getArguments().length == 0) {
                        return timedDataSource.getConnection();
                    }
                    return timedDataSource.getConnection(
                            (String) invocation.getArguments()[0], (String) invocation.getArguments()[1]);
                };
                NameMatchMethodPointcutAdvisor advisor = new NameMatchMethodPointcutAdvisor(acquireConnection);
                advisor.setMappedName("getConnection");
                return advisor;
            }
        };
    }
}
