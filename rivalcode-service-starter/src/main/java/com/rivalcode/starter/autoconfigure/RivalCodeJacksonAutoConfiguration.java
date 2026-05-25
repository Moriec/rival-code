package com.rivalcode.starter.autoconfigure;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnClass(JavaTimeModule.class)
public class RivalCodeJacksonAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(name = "rivalCodeJacksonCustomizer")
    public Jackson2ObjectMapperBuilderCustomizer rivalCodeJacksonCustomizer() {
        return builder -> builder
                .modules(new JavaTimeModule())
                .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }
}
