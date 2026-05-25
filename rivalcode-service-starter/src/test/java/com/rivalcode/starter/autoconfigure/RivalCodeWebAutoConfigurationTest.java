package com.rivalcode.starter.autoconfigure;

import com.rivalcode.starter.error.GlobalApiExceptionHandler;
import com.rivalcode.starter.tracing.TraceIdFilter;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.web.servlet.FilterRegistrationBean;

import static org.assertj.core.api.Assertions.assertThat;

class RivalCodeWebAutoConfigurationTest {

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    RivalCodeServiceAutoConfiguration.class,
                    RivalCodeWebAutoConfiguration.class
            ));

    @Test
    void createsTraceFilterAndExceptionHandlerForServletApplication() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(GlobalApiExceptionHandler.class);
            assertThat(context).hasBean("rivalCodeTraceIdFilterRegistration");

            FilterRegistrationBean<?> registration = context.getBean(
                    "rivalCodeTraceIdFilterRegistration",
                    FilterRegistrationBean.class
            );
            assertThat(registration.getFilter()).isInstanceOf(TraceIdFilter.class);
        });
    }
}
