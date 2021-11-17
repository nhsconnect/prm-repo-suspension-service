package uk.nhs.prm.repo.suspension.service.metrics;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class HealthCheckStatusPublisherTest {

    @Mock
    private MetricPublisher metricPublisher;

    @Test
    void shouldSendHealthyMetricUpdate() {
        HealthCheckStatusPublisher healthMetricPublisher = new HealthCheckStatusPublisher(metricPublisher);
        healthMetricPublisher.publishHealthStatus();
        verify(metricPublisher).publishMetric("Health", 1.0);
    }

}
