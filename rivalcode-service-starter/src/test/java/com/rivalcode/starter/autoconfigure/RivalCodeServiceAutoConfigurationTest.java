package com.rivalcode.starter.autoconfigure;

import com.rivalcode.contracts.common.EventEnvelope;
import com.rivalcode.starter.events.EventEnvelopeFactory;
import com.rivalcode.starter.service.ServiceInfo;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class RivalCodeServiceAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(RivalCodeServiceAutoConfiguration.class));

    @Test
    void createsServiceInfoFromRivalCodeProperties() {
        contextRunner
                .withPropertyValues(
                        "rivalcode.service.name=sample-service",
                        "rivalcode.service.version=1.2.3"
                )
                .run(context -> {
                    ServiceInfo serviceInfo = context.getBean(ServiceInfo.class);

                    assertThat(serviceInfo.name()).isEqualTo("sample-service");
                    assertThat(serviceInfo.version()).isEqualTo("1.2.3");
                });
    }

    @Test
    void createsEventEnvelopeWithServiceProducer() {
        contextRunner
                .withPropertyValues("rivalcode.service.name=sample-service")
                .run(context -> {
                    EventEnvelopeFactory factory = context.getBean(EventEnvelopeFactory.class);

                    EventEnvelope<String> envelope = factory.create("SAMPLE_EVENT", 1, "payload");

                    assertThat(envelope.getEventId()).isNotNull();
                    assertThat(envelope.getEventType()).isEqualTo("SAMPLE_EVENT");
                    assertThat(envelope.getEventVersion()).isEqualTo(1);
                    assertThat(envelope.getOccurredAt()).isNotNull();
                    assertThat(envelope.getProducer()).isEqualTo("sample-service");
                    assertThat(envelope.getPayload()).isEqualTo("payload");
                });
    }
}
