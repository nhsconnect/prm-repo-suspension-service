package uk.nhs.prm.repo.suspension.service.metrics;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;

@Configuration
public class AppConfig {
    private final String environment;

    public AppConfig(@Value("${environment}") String environment) {
        this.environment = environment;
    }

    public String environment() {
        return environment;
    }


    @Bean
    @SuppressWarnings("unused")
    public CloudWatchClient cloudWatchClient() {
        return CloudWatchClient.create();
    }
}
