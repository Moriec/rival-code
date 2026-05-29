package com.rivalcode.starter.autoconfigure;

import com.rivalcode.starter.events.EventEnvelopeFactory;
import com.rivalcode.starter.properties.RivalCodeServiceProperties;
import com.rivalcode.starter.service.ServiceInfo;
import com.rivalcode.starter.tracing.MdcTraceIdProvider;
import com.rivalcode.starter.tracing.TraceIdProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

@AutoConfiguration
@EnableConfigurationProperties(RivalCodeServiceProperties.class)
public class RivalCodeServiceAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ServiceInfo rivalCodeServiceInfo(
            RivalCodeServiceProperties properties,
            Environment environment
    ) {
        String name = StringUtils.hasText(properties.getName())
                ? properties.getName()
                : environment.getProperty("spring.application.name", "unknown-service");

        return new ServiceInfo(name, properties.getVersion());
    }

    @Bean
    @ConditionalOnMissingBean
    public TraceIdProvider traceIdProvider() {
        return new MdcTraceIdProvider();
    }

    @Bean
    @ConditionalOnMissingBean
    public EventEnvelopeFactory eventEnvelopeFactory(
            ServiceInfo serviceInfo,
            TraceIdProvider traceIdProvider
    ) {
        return new EventEnvelopeFactory(serviceInfo, traceIdProvider);
    }
}
