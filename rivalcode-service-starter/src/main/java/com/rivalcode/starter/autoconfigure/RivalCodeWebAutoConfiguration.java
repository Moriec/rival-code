package com.rivalcode.starter.autoconfigure;

import com.rivalcode.starter.error.GlobalApiExceptionHandler;
import com.rivalcode.starter.properties.RivalCodeServiceProperties;
import com.rivalcode.starter.tracing.TraceIdFilter;
import com.rivalcode.starter.tracing.TraceIdProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.filter.OncePerRequestFilter;

@AutoConfiguration(after = RivalCodeServiceAutoConfiguration.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass({OncePerRequestFilter.class, FilterRegistrationBean.class})
public class RivalCodeWebAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(name = "rivalCodeTraceIdFilterRegistration")
    @ConditionalOnProperty(prefix = "rivalcode.service", name = "trace-id-enabled", havingValue = "true", matchIfMissing = true)
    public FilterRegistrationBean<TraceIdFilter> rivalCodeTraceIdFilterRegistration(
            RivalCodeServiceProperties properties
    ) {
        FilterRegistrationBean<TraceIdFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new TraceIdFilter(properties.getTraceHeaderName()));
        registration.setName("rivalCodeTraceIdFilter");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }

    @Bean
    @ConditionalOnClass(RestControllerAdvice.class)
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "rivalcode.service", name = "error-handling-enabled", havingValue = "true", matchIfMissing = true)
    public GlobalApiExceptionHandler globalApiExceptionHandler(TraceIdProvider traceIdProvider) {
        return new GlobalApiExceptionHandler(traceIdProvider);
    }
}
